-- Spec #352: DeviceVersion 컬럼명 가독성 개선
ALTER TABLE device_version RENAME COLUMN createdate TO create_date;
