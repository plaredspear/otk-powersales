CREATE TABLE tmp_claim_code (
    tmp_claim_code_id BIGSERIAL PRIMARY KEY,
    claim1_code       VARCHAR(80),
    claim1_name       VARCHAR(80),
    claim2_code       VARCHAR(80),
    claim2_name       VARCHAR(80),
    created_at        TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at        TIMESTAMP NOT NULL DEFAULT NOW()
);
