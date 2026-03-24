-- if_product → product_sync_buffer 테이블 리네이밍
ALTER TABLE if_product RENAME TO product_sync_buffer;

-- PK 제약조건 리네이밍
ALTER INDEX if_product_pkey RENAME TO product_sync_buffer_pkey;
