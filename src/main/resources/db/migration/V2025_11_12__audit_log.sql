CREATE SCHEMA IF NOT EXISTS audit;

CREATE TYPE audit.operation_type AS ENUM ('INSERT', 'UPDATE', 'DELETE');

CREATE TABLE IF NOT EXISTS audit.audit_log
(
    id             BIGSERIAL PRIMARY KEY,
    table_name     TEXT                 NOT NULL,
    operation      audit.operation_type NOT NULL,
    record_id      TEXT,
    old_data       JSONB,
    new_data       JSONB,
    changed_fields TEXT[],
    changed_at     TIMESTAMP            NOT NULL DEFAULT NOW()
);

REVOKE ALL ON SCHEMA audit FROM PUBLIC;

CREATE INDEX idx_audit_log_table_name ON audit.audit_log (table_name);
CREATE INDEX idx_audit_log_changed_at ON audit.audit_log (changed_at DESC);
CREATE INDEX idx_audit_log_record_id ON audit.audit_log (record_id);

COMMENT ON TABLE audit.audit_log IS 'Stores audit trail for tracked database tables';

CREATE OR REPLACE FUNCTION audit.audit()
    RETURNS TRIGGER AS
$$
DECLARE
    old_data_json   JSONB;
    new_data_json   JSONB;
    changed_fields  TEXT[];
    record_id_value TEXT;
BEGIN
    IF TG_OP = 'DELETE' THEN
        record_id_value := OLD.id::TEXT;
    ELSE
        record_id_value := NEW.id::TEXT;
    END IF;

    IF TG_OP = 'UPDATE' THEN
        old_data_json := to_jsonb(OLD);
        new_data_json := to_jsonb(NEW);

        SELECT array_agg(old_record.key)
        INTO changed_fields
        FROM jsonb_each(old_data_json) AS old_record(key, value)
        WHERE old_record.value IS DISTINCT FROM (new_data_json -> old_record.key);

    ELSIF TG_OP = 'INSERT' THEN
        new_data_json := to_jsonb(NEW);
        old_data_json := NULL;
        changed_fields := NULL;

    ELSIF TG_OP = 'DELETE' THEN
        old_data_json := to_jsonb(OLD);
        new_data_json := NULL;
        changed_fields := NULL;
    END IF;

    INSERT INTO audit.audit_log (table_name,
                                 operation,
                                 record_id,
                                 old_data,
                                 new_data,
                                 changed_fields,
                                 changed_at)
    VALUES (TG_TABLE_NAME,
            TG_OP::audit.operation_type,
            record_id_value,
            old_data_json,
            new_data_json,
            changed_fields,
            NOW());

    IF TG_OP = 'DELETE' THEN
        RETURN OLD;
    ELSE
        RETURN NEW;
    END IF;
END;
$$ LANGUAGE plpgsql;

COMMENT ON FUNCTION audit.audit() IS 'Generic trigger function for auditing table changes';

DROP TRIGGER IF EXISTS audit_users_trigger ON marlin.user;
CREATE TRIGGER audit
    AFTER INSERT OR UPDATE OR DELETE
    ON marlin.user
    FOR EACH ROW
EXECUTE FUNCTION audit.audit();

DROP TRIGGER IF EXISTS audit_payment_trigger ON marlin.payment;
CREATE TRIGGER audit
    AFTER INSERT OR UPDATE OR DELETE
    ON marlin.payment
    FOR EACH ROW
EXECUTE FUNCTION audit.audit();