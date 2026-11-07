CREATE TABLE greeting (
  id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  message TEXT NOT NULL
);

INSERT INTO greeting (message) VALUES ('Hello, World!');
