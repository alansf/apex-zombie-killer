package com.alansf.apexzombiekiller.repo;

import com.alansf.apexzombiekiller.model.CodeBinding;
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
public class BindingRepository {
	private final JdbcTemplate jdbc;
	private final RowMapper<CodeBinding> row = new RowMapper<>() {
		@Override public CodeBinding mapRow(ResultSet rs, int rowNum) throws SQLException {
			CodeBinding b = new CodeBinding();
			b.id = (UUID) rs.getObject("id");
			b.name = rs.getString("name");
			b.type = rs.getString("type");
			b.configJson = rs.getString("config");
			b.createdAt = rs.getTimestamp("created_at").toInstant();
			b.updatedAt = rs.getTimestamp("updated_at").toInstant();
			return b;
		}
	};

	public BindingRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }

	public Optional<CodeBinding> findWeb(String name) {
		List<CodeBinding> list = jdbc.query("SELECT * FROM code_binding WHERE name=? AND type='web'", row, name);
		return list.stream().findFirst();
	}

	public void upsertWeb(String name, String path) {
		String cfg = "{\"path\":\"" + path + "\"}";
		Optional<CodeBinding> existing = findWeb(name);
		if (existing.isPresent()) {
			jdbc.update("UPDATE code_binding SET config=?::jsonb, updated_at=NOW() WHERE id=?",
					cfg, existing.get().id);
		} else {
			jdbc.update("INSERT INTO code_binding(id,name,type,config) VALUES (?,?, 'web', ?::jsonb)",
					UUID.randomUUID(), name, cfg);
		}
	}

	public List<CodeBinding> listWeb() {
		return jdbc.query("SELECT * FROM code_binding WHERE type='web' ORDER BY updated_at DESC", row);
	}
}


