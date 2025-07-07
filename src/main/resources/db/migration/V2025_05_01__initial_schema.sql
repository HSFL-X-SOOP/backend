CREATE SCHEMA IF NOT EXISTS marlin;
SET search_path TO marlin, pg_catalog, public;

CREATE EXTENSION  IF NOT EXISTS postgis WITH SCHEMA marlin;
CREATE EXTENSION IF NOT EXISTS timescaledb WITH SCHEMA marlin;
CREATE EXTENSION IF NOT EXISTS timescaledb_toolkit WITH SCHEMA marlin;

CREATE TABLE Sensor (
                        id BIGINT PRIMARY KEY,
                        name TEXT NOT NULL,
                        description TEXT,
                        is_moving BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE TABLE Location (
                          id BIGSERIAL PRIMARY KEY,
                          name TEXT,
                          coordinates GEOGRAPHY(POINT, 4326)  -- PostGIS
);

-- Gist index for spatial queries and fast comparison between coordinates
CREATE INDEX location_coordinates_idx ON Location USING GIST(coordinates);


CREATE TABLE MeasurementType (
                                 id BIGINT PRIMARY KEY,
                                 name TEXT NOT NULL,
                                 description TEXT,
                                 unit_name TEXT,
                                 unit_symbol TEXT,
                                 unit_definition TEXT
);

CREATE TABLE Measurement (
                             sensor_id BIGINT REFERENCES Sensor(id),
                             type_id BIGINT REFERENCES MeasurementType(id),
                             location_id BIGINT REFERENCES Location(id),
                             time TIMESTAMPTZ NOT NULL,
                             value DOUBLE PRECISION NOT NULL,
                             PRIMARY KEY(sensor_id, type_id, time)
) WITH (
      tsdb.hypertable,
      tsdb.partition_column='time'
      );

-- Test hypertable
--SELECT * FROM timescaledb_information.hypertables;

