-- QE-55: GPS attendance configuration on events and GPS telemetry / suspicious flag on attendances

ALTER TABLE events
  ADD COLUMN gps_lat double precision,
  ADD COLUMN gps_lng double precision,
  ADD COLUMN allowed_radius_meters int,
  ADD CONSTRAINT ck_events_gps_lat CHECK (gps_lat IS NULL OR (gps_lat BETWEEN -90.0 AND 90.0)),
  ADD CONSTRAINT ck_events_gps_lng CHECK (gps_lng IS NULL OR (gps_lng BETWEEN -180.0 AND 180.0)),
  ADD CONSTRAINT ck_events_allowed_radius CHECK (allowed_radius_meters IS NULL OR (allowed_radius_meters BETWEEN 10 AND 1000));

ALTER TABLE attendances
  ADD COLUMN gps_lat double precision,
  ADD COLUMN gps_lng double precision,
  ADD COLUMN gps_accuracy_meters double precision,
  ADD COLUMN gps_distance_meters double precision,
  ADD COLUMN suspicious_flag boolean NOT NULL DEFAULT false,
  ADD COLUMN suspicious_reason varchar(200);
