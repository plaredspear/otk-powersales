-- User SF 정합 — 누락 writeable 필드 `prnflag__c` (홍보영양사여부) 적재 컬럼 추가.
--
-- 단일 권위: Salesforce Object (`User`)
-- 필드: prnflag__c (SF type=Text, length=100, 라벨 "홍보영양사여부")
-- 매핑: user.prn_flag varchar(100) NULL (데이터 보존용 — 신규 조회/로직 미사용)
--
-- 비고: SF Apex/Trigger/Aura/LWC + Heroku 어느 로직에서도 미사용. SF 운영 리포트
--       ("홍보영양사 명단") / 리스트뷰 / DKRetail__SalesDiary__c.prnflag__c formula 참조용
--       보조 데이터. 신규 시스템에 소비처는 없으나 원본 보존 목적으로 적재.

ALTER TABLE powersales."user"
    ADD COLUMN prn_flag varchar(100);
