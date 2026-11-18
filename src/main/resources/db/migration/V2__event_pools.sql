-- V2: Quota nhiều tầng theo phạm vi (QE-12/QE-39/QE-40)
-- Entity tương ứng: vn.edu.qnu.events.event.EventPool (@Table event_pool)
CREATE TABLE event_pools (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  event_id uuid NOT NULL REFERENCES events(id) ON DELETE CASCADE,
  scope_type varchar(20) NOT NULL CHECK (scope_type IN ('LOP','KHOA','KHOI','CLB','ALL')),
  scope_ref varchar(64) NOT NULL DEFAULT '',
  capacity int NOT NULL CHECK (capacity >= 1),
  used int NOT NULL DEFAULT 0 CHECK (used >= 0 AND used <= capacity),
  priority int NOT NULL DEFAULT 0,
  allow_overflow boolean NOT NULL DEFAULT false,
  is_mandatory boolean NOT NULL DEFAULT false,
  reserved_until timestamptz,
  created_at timestamptz NOT NULL DEFAULT now(),
  UNIQUE (event_id, scope_type, scope_ref)
);
CREATE INDEX ix_event_pools_lookup ON event_pools(event_id, priority);

-- QE-39/QE-40: đăng ký gắn pool đã cấp; hàng chờ xếp theo thời điểm
ALTER TABLE registrations ADD COLUMN pool_id uuid REFERENCES event_pools(id);
ALTER TABLE registrations ADD COLUMN waiting_at timestamptz;
