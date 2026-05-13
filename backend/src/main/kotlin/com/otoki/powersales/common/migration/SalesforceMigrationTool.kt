package com.otoki.powersales.common.migration

import com.otoki.powersales.claim.entity.Claim
import com.otoki.powersales.common.entity.AgreementHistory
import com.otoki.powersales.common.entity.BranchReview
import com.otoki.powersales.common.entity.UploadFile
import com.otoki.powersales.leave.entity.AlternativeHoliday
import com.otoki.powersales.leave.entity.HolidayMaster
import com.otoki.powersales.notice.entity.Notice
import com.otoki.powersales.product.entity.Product
import com.otoki.powersales.order.entity.ErpOrder
import com.otoki.powersales.order.entity.ErpOrderProduct
import com.otoki.powersales.order.entity.OrderRequest
import com.otoki.powersales.order.entity.OrderRequestProduct
import com.otoki.powersales.product.entity.ProductBarcode
import com.otoki.powersales.sales.entity.MonthlySalesHistory
import com.otoki.powersales.promotion.entity.ProfessionalPromotionTeamHistory
import com.otoki.powersales.promotion.entity.ProfessionalPromotionTeamMaster
import com.otoki.powersales.promotion.entity.Promotion
import com.otoki.powersales.promotion.entity.PromotionEmployee
import com.otoki.powersales.schedule.entity.AttendInfo
import com.otoki.powersales.schedule.entity.DisplayWorkSchedule
import com.otoki.powersales.schedule.entity.MonthlyFemaleEmployeeIntegrationSchedule
import com.otoki.powersales.schedule.entity.TeamMemberSchedule
import com.otoki.powersales.organization.entity.Organization
import com.otoki.powersales.schedule.entity.Appointment
import com.otoki.powersales.schedule.entity.AttendanceLog

/**
 * Salesforce API → Dev DB 데이터 마이그레이션 도구
 * Salesforce REST API로 오브젝트를 조회하여 Dev DB에 적재. @SFObject/@SFField 어노테이션 기반 매핑.
 *
 * 정책 변경 (2026-05-13): HC (Heroku Connect) 경유 마이그레이션 폐지. 본 도구가 모든 @SFObject 엔티티의
 * 유일한 마이그레이션 경로. 현재 등록 entity 외 추가 대상은 후속 스펙으로 단계적 등재.
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

    /** @SFObject 엔티티 등록. 순서가 마이그레이션 순서를 결정 (FK 의존성 순) */
    private val entities = listOf(
        // ── 마스터 데이터 ──
        EntityRegistration("organization", Organization::class.java),
        EntityRegistration("holidayMaster", HolidayMaster::class.java),
        EntityRegistration("product", Product::class.java),
        EntityRegistration("productBarcode", ProductBarcode::class.java),

        // ── Employee 참조 ──
        EntityRegistration("attendInfo", AttendInfo::class.java),
        EntityRegistration("alternativeHoliday", AlternativeHoliday::class.java),
        EntityRegistration("displayWorkSchedule", DisplayWorkSchedule::class.java),
        EntityRegistration("monthlyFemaleEmployeeIntegrationSchedule", MonthlyFemaleEmployeeIntegrationSchedule::class.java),
        EntityRegistration("teamMemberSchedule", TeamMemberSchedule::class.java),
        EntityRegistration("branchReview", BranchReview::class.java),
        EntityRegistration("appointment", Appointment::class.java),

        // ── Promotion 관련 (account, product, team_member_schedule 선행 적재 필요) ──
        EntityRegistration("promotion", Promotion::class.java),
        EntityRegistration("promotionEmployee", PromotionEmployee::class.java),
        EntityRegistration("professionalPromotionTeamMaster", ProfessionalPromotionTeamMaster::class.java),
        EntityRegistration("professionalPromotionTeamHistory", ProfessionalPromotionTeamHistory::class.java),
        EntityRegistration("monthlySalesHistory", MonthlySalesHistory::class.java),

        // ── Claim 관련 (Employee, Account 선행 적재 필요) ──
        EntityRegistration("claim", Claim::class.java),

        // ── Agreement / Upload ──
        EntityRegistration("agreementHistory", AgreementHistory::class.java),
        EntityRegistration("uploadFile", UploadFile::class.java),

        // ── Order / Notice ──
        EntityRegistration("notice", Notice::class.java),
        EntityRegistration("erpOrder", ErpOrder::class.java),
        EntityRegistration("erpOrderProduct", ErpOrderProduct::class.java),
        EntityRegistration("orderRequest", OrderRequest::class.java),
        EntityRegistration("orderRequestProduct", OrderRequestProduct::class.java),

        // ── 출근현황 (Employee, Account 선행 적재 필요) ──
        EntityRegistration("attendanceLog", AttendanceLog::class.java),
    )
}
