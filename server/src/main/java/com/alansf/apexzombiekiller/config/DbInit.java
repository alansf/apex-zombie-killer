package com.alansf.apexzombiekiller.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration
public class DbInit {
	@Bean
	CommandLineRunner initDb(JdbcTemplate jdbc) {
		return args -> {
			jdbc.execute("""
				CREATE TABLE IF NOT EXISTS transformed_code (
				  id UUID PRIMARY KEY,
				  name VARCHAR(200) UNIQUE NOT NULL,
				  language VARCHAR(16) NOT NULL,
				  source TEXT NOT NULL,
				  metadata JSONB,
				  status VARCHAR(32) DEFAULT 'READY',
				  created_at TIMESTAMPTZ DEFAULT NOW(),
				  created_by VARCHAR(120)
				)
			""");
			jdbc.execute("""
				CREATE TABLE IF NOT EXISTS execution_audit (
				  id UUID PRIMARY KEY,
				  code_id UUID REFERENCES transformed_code(id),
				  code_name VARCHAR(200),
				  started_at TIMESTAMPTZ DEFAULT NOW(),
				  completed_at TIMESTAMPTZ,
				  status VARCHAR(32),
				  error TEXT,
				  input JSONB
				)
			""");
		};
	}
}


