package com.alansf.apexzombiekiller.repo;

import com.alansf.apexzombiekiller.model.ExecutionAudit;
import com.alansf.apexzombiekiller.model.TransformedCode;
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
public class CodeRepository {
	private final JdbcTemplate jdbc;
	private final RowMapper<TransformedCode> codeRow = new RowMapper<>() {
		@Override public TransformedCode mapRow(ResultSet rs, int rowNum) throws SQLException {
			TransformedCode c = new TransformedCode();
			c.id = (UUID) rs.getObject("id");
			c.name = rs.getString("name");
			c.language = rs.getString("language");
			c.source = rs.getString("source");
			c.metadataJson = rs.getString("metadata");
			c.status = rs.getString("status");
			c.createdAt = rs.getTimestamp("created_at").toInstant();
			c.createdBy = rs.getString("created_by");
			return c;
		}
	};
	private final RowMapper<ExecutionAudit> auditRow = new RowMapper<>() {
		@Override public ExecutionAudit mapRow(ResultSet rs, int rowNum) throws SQLException {
			ExecutionAudit a = new ExecutionAudit();
			a.id = (UUID) rs.getObject("id");
			a.codeId = (UUID) rs.getObject("code_id");
			a.codeName = rs.getString("code_name");
			a.startedAt = rs.getTimestamp("started_at").toInstant();
			var completed = rs.getTimestamp("completed_at");
			a.completedAt = completed == null ? null : completed.toInstant();
			a.status = rs.getString("status");
			a.error = rs.getString("error");
			a.inputJson = rs.getString("input");
			return a;
		}
	};

	public CodeRepository(JdbcTemplate jdbc) {
		this.jdbc = jdbc;
	}

	public TransformedCode insertCode(String name, String language, String source, String metadataJson, String createdBy) {
		UUID id = UUID.randomUUID();
		jdbc.update("INSERT INTO transformed_code(id,name,language,source,metadata,created_by) VALUES (?,?,?,?,?::jsonb,?)",
				id, name, language, source, metadataJson, createdBy);
		return findById(id).orElseThrow();
	}

	public Optional<TransformedCode> findById(UUID id) {
		List<TransformedCode> list = jdbc.query("SELECT * FROM transformed_code WHERE id = ?", codeRow, id);
		return list.stream().findFirst();
	}

	public Optional<TransformedCode> findByName(String name) {
		List<TransformedCode> list = jdbc.query("SELECT * FROM transformed_code WHERE name = ?", codeRow, name);
		return list.stream().findFirst();
	}

	public ExecutionAudit createAudit(UUID codeId, String codeName, String inputJson) {
		UUID id = UUID.randomUUID();
		jdbc.update("INSERT INTO execution_audit(id, code_id, code_name, status, input) VALUES (?,?,?,?,?::jsonb)",
				id, codeId, codeName, "ACCEPTED", inputJson);
		return findAudit(id).orElseThrow();
	}

	public void markAuditRunning(UUID id) {
		jdbc.update("UPDATE execution_audit SET status='RUNNING', started_at=? WHERE id=?", Instant.now(), id);
	}

	public void markAuditSuccess(UUID id) {
		jdbc.update("UPDATE execution_audit SET status='SUCCEEDED', completed_at=? WHERE id=?", Instant.now(), id);
	}

	public void markAuditFailure(UUID id, String error) {
		jdbc.update("UPDATE execution_audit SET status='FAILED', completed_at=?, error=? WHERE id=?", Instant.now(), error, id);
	}

	public Optional<ExecutionAudit> findAudit(UUID id) {
		List<ExecutionAudit> list = jdbc.query("SELECT * FROM execution_audit WHERE id=?", auditRow, id);
		return list.stream().findFirst();
	}
}


