package com.alansf.apexzombiekiller.repo;

import com.alansf.apexzombiekiller.model.JobItem;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class JobQueueRepository {
	private final JdbcTemplate jdbc;
	private final RowMapper<JobItem> row = new RowMapper<>() {
		@Override public JobItem mapRow(ResultSet rs, int rowNum) throws SQLException {
			JobItem j = new JobItem();
			j.id = (UUID) rs.getObject("id");
			j.jobType = rs.getString("job_type");
			j.targetName = rs.getString("target_name");
			j.payloadJson = rs.getString("payload");
			j.status = rs.getString("status");
			j.attempts = rs.getInt("attempts");
			j.enqueuedAt = rs.getTimestamp("enqueued_at").toInstant();
			var st = rs.getTimestamp("started_at");
			j.startedAt = st == null ? null : st.toInstant();
			var fn = rs.getTimestamp("finished_at");
			j.finishedAt = fn == null ? null : fn.toInstant();
			j.error = rs.getString("error");
			return j;
		}
	};

	public JobQueueRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }

	public UUID enqueue(String jobType, String targetName, String payloadJson) {
		UUID id = UUID.randomUUID();
		jdbc.update("INSERT INTO job_queue(id, job_type, target_name, payload) VALUES (?,?,?,?::jsonb)",
				id, jobType, targetName, payloadJson == null ? "{}" : payloadJson);
		// Fire a lightweight NOTIFY for demo
		try { jdbc.execute("NOTIFY mia_events"); } catch (Exception ignored) {}
		return id;
	}

	public Optional<JobItem> lockNext() {
		List<JobItem> list = jdbc.query(
				"UPDATE job_queue SET status='running', started_at=?, attempts=attempts+1 " +
						"WHERE id = (" +
						"  SELECT id FROM job_queue WHERE status='queued' ORDER BY enqueued_at LIMIT 1 FOR UPDATE SKIP LOCKED" +
						") RETURNING *",
				row, Instant.now());
		return list.stream().findFirst();
	}

	public void markDone(UUID id) {
		jdbc.update("UPDATE job_queue SET status='done', finished_at=? WHERE id=?", Instant.now(), id);
	}
	public void markError(UUID id, String error) {
		jdbc.update("UPDATE job_queue SET status='error', finished_at=?, error=? WHERE id=?", Instant.now(), error, id);
	}
}


