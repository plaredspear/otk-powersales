CREATE TABLE org (
    id         BIGSERIAL PRIMARY KEY,
    cc_cd2     VARCHAR(10),
    org_cd2    VARCHAR(20),
    org_nm2    VARCHAR(100),
    cc_cd3     VARCHAR(10),
    org_cd3    VARCHAR(20),
    org_nm3    VARCHAR(100),
    cc_cd4     VARCHAR(10),
    org_cd4    VARCHAR(20),
    org_nm4    VARCHAR(100),
    cc_cd5     VARCHAR(10),
    org_cd5    VARCHAR(20),
    org_nm5    VARCHAR(100),
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_org_cc_cd3 ON org (cc_cd3);
CREATE INDEX idx_org_cc_cd4 ON org (cc_cd4);
CREATE INDEX idx_org_cc_cd5 ON org (cc_cd5);
