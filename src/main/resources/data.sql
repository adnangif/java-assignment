MERGE INTO sequence_registry (sequence_type, current_value, increment_by) KEY(sequence_type) VALUES ('ORDER',   0, 1);
MERGE INTO sequence_registry (sequence_type, current_value, increment_by) KEY(sequence_type) VALUES ('USER',    0, 1);
MERGE INTO sequence_registry (sequence_type, current_value, increment_by) KEY(sequence_type) VALUES ('INVOICE', 0, 1);
