CREATE TABLE IF NOT EXISTS transformed_code (
  id UUID PRIMARY KEY,
  name VARCHAR(200) UNIQUE NOT NULL,
  language VARCHAR(16) NOT NULL,
  source TEXT NOT NULL,
  metadata JSONB,
  status VARCHAR(32) DEFAULT 'READY',
  created_at TIMESTAMPTZ DEFAULT NOW(),
  created_by VARCHAR(120)
);

CREATE TABLE IF NOT EXISTS execution_audit (
  id UUID PRIMARY KEY,
  code_id UUID REFERENCES transformed_code(id),
  code_name VARCHAR(200),
  started_at TIMESTAMPTZ DEFAULT NOW(),
  completed_at TIMESTAMPTZ,
  status VARCHAR(32),
  error TEXT,
  input JSONB
);

CREATE INDEX IF NOT EXISTS idx_transformed_code_name ON transformed_code (name);
CREATE INDEX IF NOT EXISTS idx_exec_audit_code_time ON execution_audit (code_name, started_at);


