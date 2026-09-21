CREATE SEQUENCE resource_seq START WITH 1 INCREMENT BY 1;

CREATE TABLE resource (
                          id BIGINT PRIMARY KEY DEFAULT nextval('resource_seq'),
                          name VARCHAR(100) NOT NULL,
                          type VARCHAR(30) NOT NULL,
                          status VARCHAR(30) NOT NULL
);

CREATE SEQUENCE reservation_seq START WITH 1 INCREMENT BY 1;

CREATE TABLE reservation (
                             id BIGINT PRIMARY KEY DEFAULT nextval('reservation_seq'),
                             resource_id BIGINT NOT NULL REFERENCES resource(id),
                             resident_id BIGINT NOT NULL,
                             start_time TIMESTAMP NOT NULL,
                             end_time TIMESTAMP NOT NULL,
                             status VARCHAR(20) NOT NULL
);