package com.alansf.apexzombiekiller.worker;

import com.alansf.apexzombiekiller.model.JobItem;
import com.alansf.apexzombiekiller.repo.JobQueueRepository;
import com.alansf.apexzombiekiller.repo.CodeRepository;
import com.alansf.apexzombiekiller.service.JavaExecutionAdapter;
import com.alansf.apexzombiekiller.service.PublishService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@EnableScheduling
public class QueueWorker {
	private static final Logger log = LoggerFactory.getLogger(QueueWorker.class);
	private final JobQueueRepository jobs;
	private final CodeRepository codeRepo;
	private final PublishService publish;

	public QueueWorker(JobQueueRepository jobs, CodeRepository codeRepo, PublishService publish) {
		this.jobs = jobs;
		this.codeRepo = codeRepo;
		this.publish = publish;
	}

	@Scheduled(fixedDelay = 2000)
	public void tick() {
		try {
			Optional<JobItem> next = jobs.lockNext();
			if (next.isEmpty()) return;
			JobItem j = next.get();
			log.info("Processing job {} type={} target={}", j.id, j.jobType, j.targetName);
			try {
				switch (j.jobType) {
					case "compile" -> compile(j);
					case "publish" -> publish(j);
					case "execute" -> execute(j);
					default -> log.warn("Unknown job type {}", j.jobType);
				}
				jobs.markDone(j.id);
			} catch (Exception ex) {
				log.error("Job {} failed", j.id, ex);
				jobs.markError(j.id, ex.getMessage());
			}
		} catch (Exception ex) {
			log.warn("Queue tick error", ex);
		}
	}

	private void compile(JobItem j) throws Exception {
		codeRepo.findByName(j.targetName).ifPresent(code -> {
			try {
				if ("java".equalsIgnoreCase(code.language)) {
					new JavaExecutionAdapter().execute(code.name, code.source);
				}
				// JS compile is eval-on-demand; skip here
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		});
	}

	private void publish(JobItem j) {
		publish.queueRepublish();
	}

	private void execute(JobItem j) throws Exception {
		// In this demo, executions are handled via REST endpoints; queue executions optional
		compile(j);
	}
}


