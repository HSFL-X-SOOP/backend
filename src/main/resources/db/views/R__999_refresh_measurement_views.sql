-- Refresh/fill all measurement views with historical data

CALL refresh_continuous_aggregate('marlin.measurement_1h_view', '2024-01-01', NULL);
CALL refresh_continuous_aggregate('marlin.measurement_2h_view', '2024-01-01', NULL);
CALL refresh_continuous_aggregate('marlin.measurement_6h_view', '2024-01-01', NULL);
CALL refresh_continuous_aggregate('marlin.measurement_12h_view', '2024-01-01', NULL);
CALL refresh_continuous_aggregate('marlin.measurement_1d_view', '2024-01-01', NULL);