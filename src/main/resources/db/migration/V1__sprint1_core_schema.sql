CREATE TABLE org_class (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  code varchar(30) NOT NULL UNIQUE,
  name varchar(150) NOT NULL,
  faculty_name varchar(150) NOT NULL,
  cohort_year smallint,
  created_at timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE app_user (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  login_id varchar(50) NOT NULL UNIQUE,
  password_hash varchar(100) NOT NULL,
  full_name varchar(150) NOT NULL,
  student_code varchar(30) UNIQUE,
  class_id uuid REFERENCES org_class(id),
  status varchar(20) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE','LOCKED')),
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE user_role (
  user_id uuid NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
  role varchar(30) NOT NULL CHECK (role IN ('STUDENT','CLASS_REPRESENTATIVE','EVENT_OWNER','SYSTEM_ADMIN')),
  PRIMARY KEY (user_id, role)
);

CREATE TABLE auth_session (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id uuid NOT NULL REFERENCES app_user(id),
  issued_at timestamptz NOT NULL DEFAULT now(),
  expires_at timestamptz NOT NULL,
  revoked_at timestamptz
);
CREATE INDEX ix_auth_session_user ON auth_session(user_id);

CREATE TABLE events (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  owner_id uuid NOT NULL REFERENCES app_user(id),
  title varchar(200) NOT NULL,
  description text,
  location varchar(300) NOT NULL,
  start_at timestamptz NOT NULL,
  end_at timestamptz NOT NULL,
  registration_deadline timestamptz NOT NULL,
  attendance_open_at timestamptz NOT NULL,
  attendance_close_at timestamptz NOT NULL,
  attendance_method varchar(20) NOT NULL DEFAULT 'DYNAMIC_QR'
    CHECK (attendance_method IN ('DYNAMIC_QR','GPS','EVIDENCE')),
  qr_rotation_seconds int NOT NULL DEFAULT 10 CHECK (qr_rotation_seconds BETWEEN 5 AND 120),
  qr_grace_seconds int NOT NULL DEFAULT 3 CHECK (qr_grace_seconds BETWEEN 0 AND 10),
  quota_total int NOT NULL CHECK (quota_total > 0),
  registered_count int NOT NULL DEFAULT 0,
  activity_type varchar(50) NOT NULL,
  activity_points int NOT NULL DEFAULT 0 CHECK (activity_points >= 0),
  status varchar(20) NOT NULL DEFAULT 'DRAFT' CHECK (status IN ('DRAFT','PUBLISHED','CANCELLED')),
  version int NOT NULL DEFAULT 0,
  previewed_version int,
  published_at timestamptz,
  published_by uuid REFERENCES app_user(id),
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now(),
  CONSTRAINT ck_events_time CHECK (end_at > start_at),
  CONSTRAINT ck_events_attendance_window CHECK (attendance_close_at > attendance_open_at),
  CONSTRAINT ck_events_quota CHECK (registered_count >= 0 AND registered_count <= quota_total)
);
CREATE INDEX ix_events_owner ON events(owner_id);
CREATE INDEX ix_events_status ON events(status);

CREATE TABLE event_audience (
  event_id uuid NOT NULL REFERENCES events(id) ON DELETE CASCADE,
  class_id uuid NOT NULL REFERENCES org_class(id),
  PRIMARY KEY (event_id, class_id)
);

CREATE TABLE registrations (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  event_id uuid NOT NULL REFERENCES events(id),
  student_id uuid NOT NULL REFERENCES app_user(id),
  status varchar(20) NOT NULL CHECK (status IN ('REGISTERED','WAITING_LIST','CANCELLED','NO_SHOW')),
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now()
);
CREATE UNIQUE INDEX uq_registrations_active
  ON registrations(event_id, student_id) WHERE status IN ('REGISTERED','WAITING_LIST');
CREATE INDEX ix_registrations_student ON registrations(student_id, created_at DESC);

CREATE TABLE attendances (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  event_id uuid NOT NULL REFERENCES events(id),
  student_id uuid NOT NULL REFERENCES app_user(id),
  method varchar(20) NOT NULL
    CHECK (method IN ('DYNAMIC_QR','GPS','EVIDENCE','OFFLINE_QR','OFFLINE_GPS','PROXY_EVIDENCE')),
  status varchar(20) NOT NULL CHECK (status IN ('PENDING','ACCEPTED','REJECTED')),
  recorded_at timestamptz NOT NULL,
  created_at timestamptz NOT NULL DEFAULT now()
);
CREATE UNIQUE INDEX uq_attendances_valid ON attendances(event_id, student_id) WHERE status = 'ACCEPTED';

CREATE TABLE attendance_attempts (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  event_id uuid NOT NULL REFERENCES events(id),
  student_id uuid NOT NULL REFERENCES app_user(id),
  method varchar(20) NOT NULL,
  idempotency_key varchar(100) NOT NULL,
  outcome varchar(20) NOT NULL CHECK (outcome IN ('ACCEPTED','REJECTED')),
  reason_code varchar(50),
  counted boolean NOT NULL,
  http_status smallint NOT NULL,
  response_body jsonb NOT NULL,
  created_at timestamptz NOT NULL DEFAULT now(),
  UNIQUE (event_id, student_id, idempotency_key)
);

CREATE TABLE audit_logs (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  occurred_at timestamptz NOT NULL DEFAULT now(),
  actor_id uuid REFERENCES app_user(id),
  actor_role varchar(30),
  action varchar(60) NOT NULL,
  target_type varchar(40) NOT NULL,
  target_id varchar(64),
  outcome varchar(20) NOT NULL CHECK (outcome IN ('SUCCESS','REJECTED','DENIED')),
  reason_code varchar(60),
  details jsonb NOT NULL DEFAULT '{}',
  request_id varchar(64)
);
CREATE INDEX ix_audit_target ON audit_logs(target_type, target_id);
CREATE INDEX ix_audit_time ON audit_logs(occurred_at);

CREATE FUNCTION audit_logs_immutable() RETURNS trigger LANGUAGE plpgsql AS
$$ BEGIN RAISE EXCEPTION 'audit_logs is append-only'; END $$;
CREATE TRIGGER trg_audit_logs_immutable BEFORE UPDATE OR DELETE ON audit_logs
  FOR EACH ROW EXECUTE FUNCTION audit_logs_immutable();
