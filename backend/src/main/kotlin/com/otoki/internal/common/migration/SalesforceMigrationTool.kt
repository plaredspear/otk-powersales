package com.otoki.internal.common.migration

import com.otoki.internal.common.entity.AgreementHistory
import com.otoki.internal.common.entity.UploadFile
import com.otoki.internal.leave.entity.AlternativeHoliday
import com.otoki.internal.promotion.entity.Promotion
import com.otoki.internal.promotion.entity.PromotionEmployee
import com.otoki.internal.sap.entity.AttendInfo
import com.otoki.internal.sap.entity.Organization
import com.otoki.internal.schedule.entity.AttendanceLog

/**
 * Salesforce API → Dev DB 데이터 마이그레이션 도구
 * Salesforce REST API로 오브젝트를 조회하여 Dev DB에 적재. @SFObject/@SFField 어노테이션 기반 매핑.
 *
 * HerokuMigrationTool에 이미 등록된 엔티티는 제외 (Heroku DB 경유로 마이그레이션 완료).
 * 본 도구는 Heroku DB에 존재하지 않아 Salesforce API로 직접 조회해야 하는 오브젝트만 대상.
 *
 * ┌──────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────┐
 * │ Migrate │ Salesforce Object (API Name)               │ Dev DB (salesforce2)        │ Entity                  │ 참조키 (sfid FK)                                              │ 비고                          │
 * ├──────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────┤
 * │         │ ── 마스터 데이터 ──────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────── │
 * │  NO     │ Organization__c                             │ organization                │ Organization            │ —                                                             │ @SAPSource 병행               │
 * ├──────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────┤
 * │         │ ── Employee 참조 ─────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────── │
 * │  NO     │ AttendInfo__c                               │ attend_info                 │ AttendInfo              │ EmployeeCode__c → employee.employee_code                      │ @SAPSource 병행               │
 * │  NO     │ DKRetail__AlternativeHoliday__c             │ alternative_holiday         │ AlternativeHoliday      │ DKRetail__EmployeeId__c → employee.sfid                       │ V83: sfid 컬럼 추가, employee_id FK │
 * ├──────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────┤
 * │         │ ── Promotion 관련 ────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────── │
 * │  NO     │ DKRetail__Promotion__c                      │ promotion                   │ Promotion               │ AccId__c → account.sfid, DKRetail__PrimaryProductId__c →      │ UPDATE: account_id, product_id │
 * │         │                                             │                             │                         │   product.sfid                                                │                               │
 * │  NO     │ DKRetail__PromotionEmployee__c              │ promotion_employee          │ PromotionEmployee       │ DKRetail__PromotionId__c → promotion.sfid,                    │ UPDATE: promotion_id          │
 * │         │                                             │                             │                         │   DKRetail__ScheduleId__c → team_member_schedule.sfid         │ V73: sfid 컬럼 추가, schedule_id → team_member_schedule_id 리네이밍 │
 * ├──────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────┤
 * │         │ ── Agreement / Upload ───────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────── │
 * │  NO     │ AgreementHistory__c                         │ agreement_history           │ AgreementHistory        │ AgreementWordId__c → agreement_word.sfid                      │ UPDATE: agreement_word_id     │
 * │  NO     │ UploadFile__c                               │ upload_file                 │ UploadFile              │ RecordId__c → 다형성 sfid (여러 오브젝트 참조)                 │                               │
 * ├──────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────┤
 * │         │ ── HerokuMigrationTool 대상 (본 도구 제외) ──────────────────────────────────────────────────────────────────────────────────────────────────────────────────── │
 * │  제외   │ Account                                     │ account                     │ Account                 │ —                                                             │ Heroku 경유 마이그레이션      │
 * │  제외   │ DKRetail__Product__c                        │ product                     │ Product                 │ —                                                             │ Heroku 경유 마이그레이션      │
 * │  제외   │ DKRetail__Employee__c                       │ employee                    │ Employee                │ —                                                             │ Heroku 경유 마이그레이션      │
 * │  제외   │ ProductBarcode__c                           │ product_barcode             │ ProductBarcode          │ —                                                             │ Heroku 경유 마이그레이션      │
 * │  제외   │ DKRetail__Notice__c                         │ notice                      │ Notice                  │ —                                                             │ Heroku 경유 마이그레이션      │
 * │  제외   │ DKRetail__DisplayWorkScheduleMaster__c      │ display_work_schedule       │ DisplayWorkSchedule     │ —                                                             │ Heroku 경유 마이그레이션      │
 * │  제외   │ DKRetail__TeamMemberSchedule__c             │ team_member_schedule        │ TeamMemberSchedule      │ —                                                             │ Heroku 경유 마이그레이션      │
 * │  제외   │ Theme__c                                    │ inspection_theme            │ InspectionTheme         │ —                                                             │ Heroku 경유 마이그레이션      │
 * │  제외   │ AgreementWord__c                            │ agreement_word              │ AgreementWord           │ —                                                             │ Heroku 경유 마이그레이션      │
 * │  제외   │ PushMessage__c                              │ push_message                │ PushMessage             │ —                                                             │ Heroku 경유 마이그레이션      │
 * │  제외   │ PushMessageReceiver__c                      │ push_message_receiver       │ PushMessageReceiver     │ —                                                             │ Heroku 경유 마이그레이션      │
 * │  제외   │ StaffReview__c                              │ staff_review                │ StaffReview             │ —                                                             │ Heroku 경유 마이그레이션      │
 * │  제외   │ HQReview__c                                 │ hq_review                   │ HqReview                │ —                                                             │ Heroku 경유 마이그레이션      │
 * ├──────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────┤
 * │         │ ── 출근현황 ──────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────── │
 * │  NO     │ DKRetail__CommuteLog__c                     │ attendance_log              │ AttendanceLog           │ DKRetail__EmployeeId__c → employee.sfid,                      │ Heroku 미동기화 SF 전용        │
 * │         │                                             │                             │                         │   DKRetail__AccId__c → account.sfid                           │                               │
 * └──────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────┘
 *
 * HerokuMigrationTool과의 차이:
 *   - HerokuMigrationTool: Heroku DB (salesforce2) → Dev DB. @HCTable/@HCColumn 기반. 33개 테이블.
 *   - SalesforceMigrationTool: Salesforce REST API → Dev DB. @SFObject/@SFField 기반. 7개 오브젝트.
 *   - Heroku DB에 존재하는 @SFObject 엔티티(13개)는 HerokuMigrationTool에서 처리하므로 본 도구 대상에서 제외.
 *   - @SFObject가 없는 Heroku 전용 테이블(education_*, tmp_*, employee_mng 등)은 양쪽 모두 SF API 대상 아님.
 *
 * TODO: Salesforce API 연동 정보 수령 후 구현 예정
 *   - Salesforce Connected App 인증 (OAuth 2.0 Client Credentials)
 *   - REST API 호출: /services/data/vXX.0/query?q=SELECT ... FROM <SFObject>
 *   - Bulk API 2.0 대량 조회 지원
 *
 * 실행:
 *   SF_CLIENT_ID=... SF_CLIENT_SECRET=... SF_INSTANCE_URL=... ./gradlew migrateSalesforce
 */
object SalesforceMigrationTool {

    /**
     * 엔티티 등록 정보.
     * @param name 로그 식별명
     * @param entityClass JPA 엔티티 클래스 (@SFObject/@SFField 필수)
     */
    private data class EntityRegistration(
        val name: String,
        val entityClass: Class<*>,
        /** FK 없이 연관된 테이블 목록 — TRUNCATE 시 함께 정리 */
        val dependentTables: List<String> = emptyList(),
        /** true이면 @Id 필드를 INSERT에 포함 (자연키 PK). false(기본)이면 제외 (IDENTITY 자동채번) */
        val includeId: Boolean = false,
    )

    /** @SFObject 엔티티 등록 (HerokuMigrationTool 대상 제외). 순서가 마이그레이션 순서를 결정 (FK 의존성 순) */
    private val entities = listOf(
        // ── 마스터 데이터 ──
        EntityRegistration("organization", Organization::class.java),

        // ── Employee 참조 ──
        EntityRegistration("attendInfo", AttendInfo::class.java),
        EntityRegistration("alternativeHoliday", AlternativeHoliday::class.java),

        // ── Promotion 관련 (account, product, team_member_schedule은 Heroku에서 먼저 적재) ──
        EntityRegistration("promotion", Promotion::class.java),
        EntityRegistration("promotionEmployee", PromotionEmployee::class.java),

        // ── Agreement / Upload ──
        EntityRegistration("agreementHistory", AgreementHistory::class.java),
        EntityRegistration("uploadFile", UploadFile::class.java),

        // ── 출근현황 (Employee, Account는 HerokuMigrationTool에서 먼저 적재) ──
        EntityRegistration("attendanceLog", AttendanceLog::class.java),
    )
}
