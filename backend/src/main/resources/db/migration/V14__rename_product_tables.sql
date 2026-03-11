-- V14: Product 테이블/컬럼명 가독성 개선 (Spec #204)
-- dkretail__product__c → product, if_product__c → if_product

SET search_path TO salesforce2;

-- ============================================================
-- 1. 테이블 리네임
-- ============================================================
ALTER TABLE dkretail__product__c RENAME TO product;
ALTER TABLE if_product__c RENAME TO if_product;

-- ============================================================
-- 2. product 테이블 컬럼 리네임 (36개)
-- ============================================================
ALTER TABLE product RENAME COLUMN dkretail__productcode__c TO product_code;
ALTER TABLE product RENAME COLUMN dkretail__producttype__c TO product_type;
ALTER TABLE product RENAME COLUMN dkretail__productstatus__c TO product_status;
ALTER TABLE product RENAME COLUMN dkretail__storecondition__c TO storage_condition;
ALTER TABLE product RENAME COLUMN dkretail__shelflife__c TO shelf_life;
ALTER TABLE product RENAME COLUMN dkretail__shelflifeunit__c TO shelf_life_unit;
ALTER TABLE product RENAME COLUMN shelflifefull__c TO shelf_life_full;
ALTER TABLE product RENAME COLUMN dkretail__category1__c TO category1;
ALTER TABLE product RENAME COLUMN dkretail__category2__c TO category2;
ALTER TABLE product RENAME COLUMN dkretail__category3__c TO category3;
ALTER TABLE product RENAME COLUMN dkretail__categorycode1__c TO category_code1;
ALTER TABLE product RENAME COLUMN dkretail__categorycode2__c TO category_code2;
ALTER TABLE product RENAME COLUMN dkretail__categorycode3__c TO category_code3;
ALTER TABLE product RENAME COLUMN dkretail__unit__c TO unit;
ALTER TABLE product RENAME COLUMN dkretail__orderingunit__c TO ordering_unit;
ALTER TABLE product RENAME COLUMN dkretail__conversionquantity__c TO conversion_quantity;
ALTER TABLE product RENAME COLUMN dkretail__boxreceivingquantity__c TO box_receiving_quantity;
ALTER TABLE product RENAME COLUMN dkretail__standardunitprice__c TO standard_unit_price;
ALTER TABLE product RENAME COLUMN standardprice__c TO standard_price;
ALTER TABLE product RENAME COLUMN supertax__c TO super_tax;
ALTER TABLE product RENAME COLUMN dkretail__launchdate__c TO launch_date;
ALTER TABLE product RENAME COLUMN dkretail__logisticsbarcode__c TO logistics_barcode;
ALTER TABLE product RENAME COLUMN tastegift__c TO taste_gift;
ALTER TABLE product RENAME COLUMN productfeatures__c TO product_features;
ALTER TABLE product RENAME COLUMN sellingpoint__c TO selling_point;
ALTER TABLE product RENAME COLUMN purpose__c TO purpose;
ALTER TABLE product RENAME COLUMN targetaccounttype__c TO target_account_type;
ALTER TABLE product RENAME COLUMN allergen__c TO allergen;
ALTER TABLE product RENAME COLUMN crosscontamination__c TO cross_contamination;
ALTER TABLE product RENAME COLUMN imgrefpath__c TO img_ref_path;
ALTER TABLE product RENAME COLUMN imgrefpath_front__c TO img_ref_path_front;
ALTER TABLE product RENAME COLUMN imgrefpath_back__c TO img_ref_path_back;
ALTER TABLE product RENAME COLUMN imgrefpathtxt__c TO img_ref_path_txt;
ALTER TABLE product RENAME COLUMN isdeleted TO is_deleted;
ALTER TABLE product RENAME COLUMN createddate TO created_date;
ALTER TABLE product RENAME COLUMN systemmodstamp TO system_mod_stamp;

-- ============================================================
-- 3. if_product 테이블 컬럼 리네임 (product와 동일 + updateflag__c)
-- ============================================================
ALTER TABLE if_product RENAME COLUMN dkretail__productcode__c TO product_code;
ALTER TABLE if_product RENAME COLUMN dkretail__producttype__c TO product_type;
ALTER TABLE if_product RENAME COLUMN dkretail__productstatus__c TO product_status;
ALTER TABLE if_product RENAME COLUMN dkretail__storecondition__c TO storage_condition;
ALTER TABLE if_product RENAME COLUMN dkretail__shelflife__c TO shelf_life;
ALTER TABLE if_product RENAME COLUMN dkretail__shelflifeunit__c TO shelf_life_unit;
ALTER TABLE if_product RENAME COLUMN dkretail__category1__c TO category1;
ALTER TABLE if_product RENAME COLUMN dkretail__category2__c TO category2;
ALTER TABLE if_product RENAME COLUMN dkretail__category3__c TO category3;
ALTER TABLE if_product RENAME COLUMN dkretail__categorycode1__c TO category_code1;
ALTER TABLE if_product RENAME COLUMN dkretail__categorycode2__c TO category_code2;
ALTER TABLE if_product RENAME COLUMN dkretail__categorycode3__c TO category_code3;
ALTER TABLE if_product RENAME COLUMN dkretail__unit__c TO unit;
ALTER TABLE if_product RENAME COLUMN dkretail__orderingunit__c TO ordering_unit;
ALTER TABLE if_product RENAME COLUMN dkretail__conversionquantity__c TO conversion_quantity;
ALTER TABLE if_product RENAME COLUMN dkretail__boxreceivingquantity__c TO box_receiving_quantity;
ALTER TABLE if_product RENAME COLUMN dkretail__standardunitprice__c TO standard_unit_price;
ALTER TABLE if_product RENAME COLUMN supertax__c TO super_tax;
ALTER TABLE if_product RENAME COLUMN dkretail__launchdate__c TO launch_date;
ALTER TABLE if_product RENAME COLUMN dkretail__logisticsbarcode__c TO logistics_barcode;
ALTER TABLE if_product RENAME COLUMN tastegift__c TO taste_gift;
ALTER TABLE if_product RENAME COLUMN productfeatures__c TO product_features;
ALTER TABLE if_product RENAME COLUMN sellingpoint__c TO selling_point;
ALTER TABLE if_product RENAME COLUMN purpose__c TO purpose;
ALTER TABLE if_product RENAME COLUMN targetaccounttype__c TO target_account_type;
ALTER TABLE if_product RENAME COLUMN allergen__c TO allergen;
ALTER TABLE if_product RENAME COLUMN crosscontamination__c TO cross_contamination;
ALTER TABLE if_product RENAME COLUMN imgrefpath__c TO img_ref_path;
ALTER TABLE if_product RENAME COLUMN imgrefpath_front__c TO img_ref_path_front;
ALTER TABLE if_product RENAME COLUMN imgrefpath_back__c TO img_ref_path_back;
ALTER TABLE if_product RENAME COLUMN imgrefpathtxt__c TO img_ref_path_txt;
ALTER TABLE if_product RENAME COLUMN updateflag__c TO update_flag;
ALTER TABLE if_product RENAME COLUMN isdeleted TO is_deleted;
ALTER TABLE if_product RENAME COLUMN createddate TO created_date;
ALTER TABLE if_product RENAME COLUMN systemmodstamp TO system_mod_stamp;

-- ============================================================
-- 4. 인덱스 삭제 후 재생성
-- ============================================================
DROP INDEX IF EXISTS hc_idx_dkretail__product__c_systemmodstamp;
DROP INDEX IF EXISTS hcu_idx_dkretail__product__c_dkretail__productcode__c;
DROP INDEX IF EXISTS hcu_idx_dkretail__product__c_sfid;

CREATE INDEX idx_product_system_mod_stamp ON product (system_mod_stamp);
CREATE UNIQUE INDEX uq_product_product_code ON product (product_code);
CREATE UNIQUE INDEX uq_product_sfid ON product (sfid);

-- ============================================================
-- 5. PK 제약조건 리네임
-- ============================================================
ALTER TABLE product RENAME CONSTRAINT dkretail__product__c_pkey TO product_pkey;
ALTER TABLE if_product RENAME CONSTRAINT if_product__c_pkey TO if_product_pkey;

-- ============================================================
-- 6. 시퀀스 리네임
-- ============================================================
ALTER SEQUENCE dkretail__product__c_id_seq RENAME TO product_id_seq;
