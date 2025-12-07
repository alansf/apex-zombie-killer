package com.alansf.apexzombiekiller.worker;

import com.alansf.apexzombiekiller.repo.JobQueueRepository;
import org.postgresql.PGConnection;
import org.postgresql.PGNotification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Statement;
import java.util.concurrent.Executors;

@Component
public class NotifyListener {
	private static final Logger log = LoggerFactory.getLogger(NotifyListener.class);
	private final DataSource dataSource;
	private final JobQueueRepository jobs;

	public NotifyListener(DataSource dataSource, JobQueueRepository jobs) {
		this.dataSource = dataSource;
		this.jobs = jobs;
	}

	@PostConstruct
	public void start() {
		Executors.newSingleThreadExecutor(r -> {
			Thread t = new Thread(r, "pg-listen");
			t.setDaemon(true);
			return t;
		}).submit(this::listenLoop);
	}

	private void listenLoop() {
		while (true) {
			try {
				Connection conn = DataSourceUtils.getConnection(dataSource);
				PGConnection pg = conn.unwrap(PGConnection.class);
				try (Statement st = conn.createStatement()) {
					st.execute("LISTEN mia_events");
				}
				while (true) {
					PGNotification[] notifications = pg.getNotifications(5000);
					if (notifications != null) {
						for (PGNotification n : notifications) {
							log.info("Received NOTIFY on {} payload={}", n.getName(), n.getParameter());
							// For demo we simply wake the queue worker by enqueuing a no-op publish job
							try { jobs.enqueue("publish", "_notify_tick_", null); } catch (Exception ignored) {}
						}
					}
				}
			} catch (Exception ex) {
				log.warn("LISTEN loop error, retrying in 2s", ex);
				try { Thread.sleep(2000); } catch (InterruptedException ignored) {}
			}
		}
	}
}


