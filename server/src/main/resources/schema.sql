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


-- New demo runtime tables for bindings, queue, and optional compiled cache
-- Transformed code approval metadata
ALTER TABLE IF EXISTS transformed_code
  ADD COLUMN IF NOT EXISTS approved_at TIMESTAMPTZ,
  ADD COLUMN IF NOT EXISTS status VARCHAR(32) DEFAULT 'READY';

-- Bindings: how code is exposed (web/trigger/queue)
CREATE TABLE IF NOT EXISTS code_binding (
  id UUID PRIMARY KEY,
  name VARCHAR(200) NOT NULL,
  type VARCHAR(16) NOT NULL, -- 'web' | 'trigger' | 'queue'
  config JSONB,
  created_at TIMESTAMPTZ DEFAULT NOW(),
  updated_at TIMESTAMPTZ DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_binding_name ON code_binding (name);
CREATE INDEX IF NOT EXISTS idx_binding_type ON code_binding (type);

-- Simple Postgres-backed job queue
CREATE TABLE IF NOT EXISTS job_queue (
  id UUID PRIMARY KEY,
  job_type VARCHAR(16) NOT NULL, -- 'compile' | 'publish' | 'execute'
  target_name VARCHAR(200) NOT NULL,
  payload JSONB,
  status VARCHAR(16) DEFAULT 'queued', -- 'queued' | 'running' | 'done' | 'error'
  attempts INT DEFAULT 0,
  enqueued_at TIMESTAMPTZ DEFAULT NOW(),
  started_at TIMESTAMPTZ,
  finished_at TIMESTAMPTZ,
  error TEXT
);
CREATE INDEX IF NOT EXISTS idx_job_status_time ON job_queue (status, enqueued_at);
CREATE INDEX IF NOT EXISTS idx_job_target ON job_queue (target_name);

-- Optional durable compiled artifact cache (not required for demo)
CREATE TABLE IF NOT EXISTS compiled_artifact (
  name VARCHAR(200) PRIMARY KEY,
  bytes BYTEA,
  updated_at TIMESTAMPTZ DEFAULT NOW()
);


