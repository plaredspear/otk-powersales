ALTER TABLE product_barcode
    ADD CONSTRAINT fk_product_barcode_product
        FOREIGN KEY (product_id) REFERENCES product (product_id);
