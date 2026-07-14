package com.otoki.powersales.domain.activity.promotion.service

import com.otoki.powersales.platform.common.enums.WorkingCategory1
import com.otoki.powersales.platform.common.enums.WorkingCategory3
import com.otoki.powersales.platform.common.enums.WorkingType
import com.otoki.powersales.domain.activity.promotion.dto.request.BatchUpdatePromotionEmployeeItem
import com.otoki.powersales.domain.activity.promotion.dto.request.BatchUpdatePromotionEmployeeRequest
import com.otoki.powersales.domain.activity.promotion.dto.request.PromotionEmployeeRequest
import com.otoki.powersales.domain.activity.promotion.dto.response.BatchItemError
import com.otoki.powersales.domain.activity.promotion.dto.response.BatchUpdatePromotionEmployeeResponse
import com.otoki.powersales.domain.activity.promotion.dto.response.PromotionEmployeeDetailResponse
import com.otoki.powersales.domain.activity.promotion.dto.response.PromotionEmployeeListResponse
import com.otoki.powersales.domain.activity.promotion.entity.Promotion
import com.otoki.powersales.domain.activity.promotion.entity.PromotionEmployee
import com.otoki.powersales.domain.activity.promotion.entity.QPromotion.Companion.promotion as qPromotion
import com.otoki.powersales.domain.activity.promotion.repository.PromotionEmployeeRepository
import com.otoki.powersales.domain.activity.promotion.repository.PromotionRepository
import com.otoki.powersales.admin.dto.DataScope
import com.otoki.powersales.domain.activity.promotion.exception.BatchValidationException
import com.otoki.powersales.domain.activity.promotion.exception.ClosedEmployeeDeleteException
import com.otoki.powersales.domain.activity.promotion.exception.ClosedEmployeeModificationException
import com.otoki.powersales.domain.activity.promotion.exception.InvalidWorkStatusException
import com.otoki.powersales.domain.activity.promotion.exception.InvalidWorkType3Exception
import com.otoki.powersales.domain.activity.promotion.exception.PromotionDateRequiredException
import com.otoki.powersales.domain.activity.promotion.exception.PromotionEmployeeNotFoundException
import com.otoki.powersales.domain.activity.promotion.exception.PromotionForbiddenException
import com.otoki.powersales.domain.activity.promotion.exception.PromotionInvalidParameterException
import com.otoki.powersales.domain.activity.promotion.exception.PromotionNotFoundException
import com.otoki.powersales.domain.activity.promotion.exception.ScheduleDateOutOfRangeException
import com.otoki.powersales.domain.activity.promotion.exception.TeamCategoryMismatchException
import com.otoki.powersales.platform.auth.entity.AppAuthority
import com.otoki.powersales.platform.auth.sharing.service.SharingRulePolicyEvaluator
import com.otoki.powersales.platform.auth.web.WebUserPrincipal
import com.otoki.powersales.domain.org.employee.dto.response.EmployeeListItem
import com.otoki.powersales.domain.org.employee.dto.response.EmployeeListResponse
import com.otoki.powersales.domain.org.employee.repository.EmployeeRepository
import com.otoki.powersales.domain.org.organization.branchmapping.BranchCodeExpander
import com.otoki.powersales.domain.activity.schedule.repository.TeamMemberScheduleRepository
import com.otoki.powersales.domain.activity.schedule.service.TeamMemberScheduleCascadeHelper
import com.otoki.powersales.platform.common.storage.StorageConstants
import com.otoki.powersales.platform.common.storage.StorageService
import org.springframework.core.env.Environment
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.LocalDate

@Service
@Transactional(readOnly = true)
class AdminPromotionEmployeeService(
    private val promotionEmployeeRepository: PromotionEmployeeRepository,
    private val promotionRepository: PromotionRepository,
    private val employeeRepository: EmployeeRepository,
    private val branchCodeExpander: BranchCodeExpander,
    private val teamMemberScheduleRepository: TeamMemberScheduleRepository,
    private val policyEvaluator: SharingRulePolicyEvaluator,
    private val teamMemberScheduleCascadeHelper: TeamMemberScheduleCascadeHelper,
    private val storageService: StorageService,
    private val environment: Environment,
) {

    // 현장사진 미리보기 가능 환경(= S3StorageService 활성) 여부.
    // S3StorageService 는 @Profile("dev | prod") 로만 등록되고, 그 외(local 등)에는 LocalStorageService(stub)
    // 가 등록되어 getPresignedUrl 이 브라우저에서 로드 불가한 "local://..." 를 반환한다. 따라서 web 이 깨진
    // 이미지를 그리지 않도록, S3 프로파일이 아닐 때는 siteImageUrl 을 아예 null 로 내린다(web 은 URL 유무로만 분기).
    // 주의: 이 프로파일 조건은 S3StorageService 의 @Profile 과 동일해야 한다(변경 시 양쪽 동기 필요).
    private val siteImagePreviewable: Boolean =
        environment.activeProfiles.any { it in S3_PROFILES }

    // 현장사진(private/ 저장)을 presigned URL 로 변환. key 없거나 blank 면 null.
    // 모바일 일매출 조회(MobileDailySalesService.imageUrl)와 동일한 TTL 로 web 관리자 조회 정합.
    // 레거시 SF SiteImage__c 수식필드(public URL)의 신규 대체 — s3ImageUniqueKey 는 응답에 그대로 유지.
    // local 환경에서는 브라우저가 로드할 수 없는 stub URL 대신 null 을 반환한다.
    private fun siteImageUrl(key: String?): String? {
        if (!siteImagePreviewable) return null
        return key?.takeIf { it.isNotBlank() }
            ?.let { storageService.getPresignedUrl(it, StorageConstants.DAILY_SALES_PRESIGN_TTL_SECONDS) }
    }

    /**
     * SF Sharing Rule 정책이 합성된 가시 PromotionEmployee 일람 (spec #782 P4-B — ControlledByParent).
     *
     * SF_SHARING_MODEL[DKRetail__PromotionEmployee__c] = ControlledByParent (parent = DKRetail__Promotion__c).
     * 본 메서드는 부모 Promotion entity 기준 [SharingRulePolicyEvaluator.buildPredicate] 호출 후 결과 Predicate 를
     * [com.otoki.powersales.domain.activity.promotion.repository.PromotionEmployeeRepositoryCustom.findAllAccessibleByParentPolicy] 에 전달.
     *
     * Promotion 자체는 sharingRule 본문 부재 — Hierarchy + Legacy branchCodes + ownerPredicate 만 평가.
     */
    fun getAccessiblePromotionEmployeesByPolicy(scope: DataScope): List<PromotionEmployee> {
        val parentPolicyPredicate = policyEvaluator.buildPredicate(
            scope = scope,
            sObjectName = "DKRetail__Promotion__c",
            entityPath = qPromotion,
        )
        return promotionEmployeeRepository.findAllAccessibleByParentPolicy(parentPolicyPredicate)
    }

    companion object {
        // 현장사진 presigned URL 이 브라우저에서 로드 가능한(= S3StorageService 활성) 프로파일.
        // S3StorageService @Profile("dev | prod") 과 반드시 일치시킬 것.
        private val S3_PROFILES = setOf("dev", "prod")
        private val VALID_WORK_STATUSES = setOf("근무", "연차", "대휴")
        private val VALID_WORK_TYPE3 = setOf("고정", "격고", "순회")
        private const val BATCH_MAX_SIZE = 200
        private val DEFAULT_WORK_TYPE1 = WorkingCategory1.EVENT
        private val DEFAULT_WORK_STATUS = WorkingType.WORK
        // 레거시 동등: 여사원 마감 행 삭제 차단 우회 대상 사번
        private const val SPECIAL_BYPASS_EMPLOYEE_CODE = "00000009"

        // 행사사원 후보 lookup 에서 거래처 지점 무관 전체 지점 여사원 조회를 허용하는 조직코드.
        // 영업지원2팀(costCenterCode = 4889, org_nm3="영업지원실" / org_nm4="영업지원2팀") 소속 사용자는
        // 시스템 관리자와 동일하게 전사 여사원을 후보로 조회한다(2026-07-14 요구). 로그인 사용자의
        // costCenterCode(대행 시 대행 대상 기준)로 판정. 조직 개편으로 코드가 바뀌면 본 상수만 변경.
        private const val ALL_BRANCH_LOOKUP_COST_CENTER_CODE = "4889"

        // 레거시 PromotionEmployeeTriggerHandler 의 대표제품 vs 전문행사조 매칭 룰
        // 좌변 promotion.category1 == 카테고리 정확 일치 (레거시 `Category1__c == '라면'` 동등)
        // 우변 team contains 키워드 부분 매칭 (예: 사원 team "라면세일조" contains "라면" → OK)
        // null / "일반" 사원은 카테고리 무관 OK (호출처에서 사전 분기)
        private const val CATEGORY_RAMEN = "라면"
        private const val CATEGORY_REFRIGERATED = "냉장"
        private const val CATEGORY_FROZEN = "냉동"
        private const val CATEGORY_DUMPLING = "만두"

        private const val MSG_MISMATCH_RAMEN =
            "대표제품이 라면인 행사에는 전문행사조가 라면세일조 혹은 일반인 사원만 들어갈 수 있습니다."
        private const val MSG_MISMATCH_REFRIGERATED =
            "대표제품이 냉장인 행사에는 전문행사조가 프레시세일조_냉장 혹은 일반인 사원만 들어갈 수 있습니다."
        private const val MSG_MISMATCH_FROZEN =
            "대표제품이 냉동인 행사에는 전문행사조가 프레시세일조_냉동 혹은 일반인 사원만 들어갈 수 있습니다."
        private const val MSG_MISMATCH_DUMPLING =
            "대표제품이 만두인 행사에는 전문행사조가 프레시세일조_냉동, 프레시세일조_만두, 일반인 사원만 들어갈 수 있습니다."
    }

    private data class ResolvedEmployee(val id: Long?, val name: String?, val employeeCode: String?, val orgName: String?)

    /**
     * 행사 진열사원 일람 — SF `DKRetail__PromotionEmployee__c` = ControlledByParent (parent = Promotion).
     *
     * 부모 Promotion 의 가시 범위를 상속해야 하므로 [scope] 로 부모 가시성을 검증한다.
     * 가시 범위 밖 행사면 [PromotionForbiddenException] (403) — promotionId 추측으로 타 지점 행사의
     * 진열사원을 열람하는 과다노출 방지(ControlledByParent 동등).
     */
    fun getEmployees(scope: DataScope, promotionId: Long): List<PromotionEmployeeListResponse> {
        val promotion = findActivePromotion(promotionId)
        validateParentVisible(scope, promotion)

        val employees = promotionEmployeeRepository.findWithEmployeeByPromotionId(promotionId)

        return employees.map { pe ->
            PromotionEmployeeListResponse.from(pe, pe.employee?.name, pe.employee?.employeeCode, pe.employee?.orgName, siteImageUrl(pe.s3ImageUniqueKey))
        }
    }

    /**
     * 행사사원 추가 시 후보 여사원 검색 — 행사 거래처가 속한 지점 소속 여사원만.
     *
     * ## 정책 배경
     * SF 레거시(`RelatedListDataGridController.getLookupCandidates`)의 행사사원 후보 검색은 `Name LIKE + Status='재직'
     * LIMIT 5` 뿐 — 지점/직책 필터가 없었다(재직자 전원, OWD Private + with sharing 암묵 가시성만). 본 메서드의
     * "거래처 지점 소속 여사원만" 제한은 **레거시에 없던 신규 정책**이나, 그 **지점 매칭 방식은 SF 여사원일정 최신 경로
     * (`ScheduleSearchByTeamMemberController` → `Util.getIncludedBranchCode`) 정합**으로 구현한다.
     *
     * ## 매칭 (SF 정합)
     * 1. 행사 거래처(`promotion.account.branchCode`, HR OrgCode 축)를 지점 코드로 사용.
     * 2. [BranchCodeExpander.expand] (= SF `getIncludedBranchCode`, BranchMapping 이력/포함 지점 확장) 로 확장.
     * 3. [com.otoki.powersales.domain.org.employee.repository.EmployeeRepositoryCustom.findActiveWomenForPromotionByCostCenterCodes]
     *    로 `employee.cost_center_code IN (확장집합)` + `role=여사원`(AppAuthority.WOMAN) + `status='재직'` 조회.
     *    상태 필터는 SF 원본 lookup(`getLookupCandidates` 의 `Status='재직'`) 및 확정 검증(status 휴직/퇴직 차단)과
     *    동일한 status 축이라 lookup↔확정 기준이 일치한다(appLoginActive 축은 사용하지 않음).
     *
     * 참고: 여사원일정 경로와 동일하게 Organization 조직트리 레벨 정규화(orgCodeLevel2~5 승격)는 태우지 않는다
     * (SF `ScheduleSearchByTeamMemberController` 가 getIncludedBranchCode 만 쓰는 것과 정합).
     *
     * ## 영업지원2팀 예외 — 전체 지점 조회
     * 로그인 사용자([loginCostCenterCode], 대행 시 대행 대상 기준)가 영업지원2팀
     * ([ALL_BRANCH_LOOKUP_COST_CENTER_CODE] = 4889) 이면 거래처 지점 제한을 건너뛰고 시스템 관리자와 동일하게
     * 전체 지점 여사원을 후보로 조회한다([EmployeeRepositoryCustom.findActiveWomenByCostCenterCodes] 인자 null
     * = costCenterCode 필터 스킵). 이 경우 거래처지점코드가 없어도 후보를 반환한다.
     *
     * ## 가시성
     * 부모 Promotion 가시 범위를 [validateParentVisible] 로 검증(ControlledByParent) — 타 지점 행사 promotionId
     * 추측으로 후보를 열람하는 과다노출 방지.
     *
     * 거래처 또는 거래처지점코드가 없으면 빈 결과(영업지원2팀 예외 제외). 응답은 [EmployeeListResponse] 재사용(기존 사원 lookup 정합).
     */
    fun lookupEmployeeCandidates(
        scope: DataScope,
        promotionId: Long,
        keyword: String?,
        size: Int,
        loginCostCenterCode: String?,
    ): EmployeeListResponse {
        val promotion = findActivePromotion(promotionId)
        validateParentVisible(scope, promotion)

        // 영업지원2팀(4889) 은 거래처 지점 무관 전체 지점 여사원 조회(costCenterCode 필터 스킵).
        // 그 외에는 거래처 지점(없으면 빈 결과) 확장 집합으로 제한.
        val expandedBranchCodes: List<String>? = if (loginCostCenterCode == ALL_BRANCH_LOOKUP_COST_CENTER_CODE) {
            null
        } else {
            val branchCode = promotion.account?.branchCode?.takeIf { it.isNotBlank() }
                ?: return EmployeeListResponse(content = emptyList(), page = 0, size = size, totalElements = 0, totalPages = 0)
            branchCodeExpander.expand(setOf(branchCode)).toList()
        }
        val today = LocalDate.now()
        val kw = keyword?.trim()?.takeIf { it.isNotBlank() }

        val matched = employeeRepository.findActiveWomenForPromotionByCostCenterCodes(expandedBranchCodes)
            .asSequence()
            .filter { emp ->
                // 이름 / 사번 부분일치 (SF 후보 검색 Name LIKE 정합, 사번도 함께 허용)
                kw == null ||
                    emp.name.contains(kw, ignoreCase = true) ||
                    (emp.employeeCode?.contains(kw, ignoreCase = true) == true)
            }
            .take(size)
            .map { EmployeeListItem.from(it, today) }
            .toList()

        return EmployeeListResponse(
            content = matched,
            page = 0,
            size = size,
            totalElements = matched.size.toLong(),
            totalPages = 1,
        )
    }

    /**
     * 부모 Promotion 가시 범위 검증 (ControlledByParent). [AdminPromotionService.validateDataScope] 동등 —
     * `scope.validateAccess(promotion.costCenterCode)` 실패 시 [PromotionForbiddenException].
     */
    private fun validateParentVisible(scope: DataScope, promotion: Promotion) {
        if (!scope.validateAccess(promotion.costCenterCode)) {
            throw PromotionForbiddenException()
        }
    }

    fun getEmployee(id: Long): PromotionEmployeeDetailResponse {
        val pe = findPromotionEmployeeById(id)
        return PromotionEmployeeDetailResponse.from(pe, pe.employee?.name, pe.employee?.employeeCode, pe.employee?.orgName, siteImageUrl(pe.s3ImageUniqueKey))
    }

    @Transactional
    fun createEmployee(promotionId: Long, request: PromotionEmployeeRequest): PromotionEmployeeDetailResponse {
        val promotion = findActivePromotion(promotionId)

        val resolved = resolveEmployee(request.employeeId)

        // 레거시 PromotionEmployeeTriggerHandler 동등: 대표제품 vs 전문행사조 매칭 검증
        validateProfessionalTeamMatch(promotion, resolved?.id ?: request.employeeId)
            ?.let { throw TeamCategoryMismatchException(it) }

        // SF AutoNumber "행사사원#"("PE" + 8자리) 동등 채번
        val seq = promotionEmployeeRepository.getNextPromotionEmployeeNumberSeq()
        val name = "PE" + String.format("%08d", seq)

        val pe = promotionEmployeeRepository.save(
            PromotionEmployee(
                promotionId = promotionId,
                name = name,
                employeeId = resolved?.id ?: request.employeeId,
                scheduleDate = request.scheduleDate,
                workStatus = request.workStatus?.let { WorkingType.fromDisplayNameOrNull(it) } ?: DEFAULT_WORK_STATUS,
                workType1 = request.workType1?.let { WorkingCategory1.fromDisplayNameOrNull(it) } ?: DEFAULT_WORK_TYPE1,
                workType3 = request.workType3?.let { WorkingCategory3.fromDisplayNameOrNull(it) },
                basePrice = request.basePrice,
                dailyTargetCount = request.dailyTargetCount,
                targetAmount = calculateTargetAmount(request.basePrice, request.dailyTargetCount),
                actualAmount = calculateActualAmount(request.primaryProductAmount, request.otherSalesAmount),
                primaryProductAmount = request.primaryProductAmount,
                primarySalesQuantity = request.primarySalesQuantity,
                primarySalesPrice = request.primarySalesPrice,
                otherSalesAmount = request.otherSalesAmount,
                otherSalesQuantity = request.otherSalesQuantity,
                s3ImageUniqueKey = request.s3ImageUniqueKey
            )
        )

        return PromotionEmployeeDetailResponse.from(pe, resolved?.name, resolved?.employeeCode, resolved?.orgName, siteImageUrl(pe.s3ImageUniqueKey))
    }

    @Transactional
    fun updateEmployee(principal: WebUserPrincipal, id: Long, userId: Long, request: PromotionEmployeeRequest): PromotionEmployeeDetailResponse {
        val pe = findPromotionEmployeeById(id)

        // employeeId로 사원 해소
        val resolved = resolveEmployee(request.employeeId)

        // 빈 문자열 → null 정규화
        val normalizedWorkType3 = request.workType3?.takeIf { it.isNotBlank() }

        if (request.workStatus != null) validateWorkStatus(request.workStatus)
        validateWorkType3(normalizedWorkType3)

        // 1-2-A: 마감 보호 — 핵심필드 수정 차단 (ADMIN 예외)
        val employee = employeeRepository.findById(userId)
            .orElseThrow { IllegalStateException("사용자를 찾을 수 없습니다: $userId") }
        validateClosedEmployeeModification(
            pe, resolved?.id, request.scheduleDate, normalizedWorkType3,
            request.basePrice, request.dailyTargetCount, employee.role == AppAuthority.BRANCH_MANAGER
        )

        val promotion = pe.promotion ?: throw PromotionNotFoundException()
        if (promotion.isDeleted) throw PromotionNotFoundException()
        validateScheduleDateRange(request.scheduleDate, promotion)

        // 레거시 PromotionEmployeeTriggerHandler 동등: 대표제품 vs 전문행사조 매칭 검증
        validateProfessionalTeamMatch(promotion, resolved?.id ?: request.employeeId)
            ?.let { throw TeamCategoryMismatchException(it) }

        // 1-5: 핵심필드 변경 시 스케줄 삭제
        removeScheduleOnCriticalFieldChange(
            principal, pe, resolved?.id, request.scheduleDate, normalizedWorkType3
        )

        pe.update(
            employeeId = resolved?.id,
            scheduleDate = request.scheduleDate,
            workStatus = request.workStatus?.let { WorkingType.fromDisplayNameOrNull(it) } ?: pe.workStatus,
            workType1 = request.workType1?.let { WorkingCategory1.fromDisplayNameOrNull(it) } ?: pe.workType1,
            workType3 = normalizedWorkType3?.let { WorkingCategory3.fromDisplayNameOrNull(it) },
            basePrice = request.basePrice,
            dailyTargetCount = request.dailyTargetCount,
            targetAmount = calculateTargetAmount(request.basePrice, request.dailyTargetCount),
            actualAmount = calculateActualAmount(request.primaryProductAmount, request.otherSalesAmount),
            primaryProductAmount = request.primaryProductAmount,
            primarySalesQuantity = request.primarySalesQuantity,
            primarySalesPrice = request.primarySalesPrice,
            otherSalesAmount = request.otherSalesAmount,
            otherSalesQuantity = request.otherSalesQuantity,
            s3ImageUniqueKey = request.s3ImageUniqueKey
        )

        promotionEmployeeRepository.save(pe)

        return PromotionEmployeeDetailResponse.from(pe, resolved?.name, resolved?.employeeCode, resolved?.orgName, siteImageUrl(pe.s3ImageUniqueKey))
    }

    @Transactional
    fun batchUpdateEmployees(
        principal: WebUserPrincipal,
        promotionId: Long,
        userId: Long,
        request: BatchUpdatePromotionEmployeeRequest
    ): BatchUpdatePromotionEmployeeResponse {
        val promotion = findActivePromotion(promotionId)

        // 행사마스터 날짜 null 검증
        @Suppress("SENSELESS_COMPARISON")
        if (promotion.startDate == null || promotion.endDate == null) {
            throw PromotionDateRequiredException()
        }

        // items 크기 검증
        if (request.items.isEmpty() || request.items.size > BATCH_MAX_SIZE) {
            throw PromotionInvalidParameterException()
        }

        // items 내 id 중복 검증
        val ids = request.items.map { it.id }
        if (ids.toSet().size != ids.size) {
            throw PromotionInvalidParameterException()
        }

        // 권한 확인
        val employee = employeeRepository.findById(userId)
            .orElseThrow { IllegalStateException("사용자를 찾을 수 없습니다: $userId") }
        val isAdmin = employee.role == AppAuthority.BRANCH_MANAGER

        // 전체 항목 검증 (에러 수집)
        val errors = mutableListOf<BatchItemError>()

        val employeeMap = mutableMapOf<Long, PromotionEmployee>()

        for ((index, item) in request.items.withIndex()) {
            val error = validateBatchItem(index, item, promotionId, promotion, isAdmin, employeeMap)
            if (error != null) {
                errors.add(error)
            }
        }

        if (errors.isNotEmpty()) {
            throw BatchValidationException(errors)
        }

        // 에러 없으면 전체 UPDATE
        for (item in request.items) {
            val pe = employeeMap[item.id]!!

            // 빈 문자열 → null 정규화
            val normalizedWorkType3 = item.workType3?.takeIf { it.isNotBlank() }

            val resolved = resolveEmployee(item.employeeId)

            removeScheduleOnCriticalFieldChange(
                principal, pe, resolved?.id, item.scheduleDate, normalizedWorkType3
            )

            pe.update(
                employeeId = resolved?.id,
                scheduleDate = item.scheduleDate,
                workStatus = item.workStatus?.let { WorkingType.fromDisplayNameOrNull(it) } ?: pe.workStatus ?: DEFAULT_WORK_STATUS,
                workType1 = item.workType1?.let { WorkingCategory1.fromDisplayNameOrNull(it) } ?: pe.workType1 ?: DEFAULT_WORK_TYPE1,
                workType3 = normalizedWorkType3?.let { WorkingCategory3.fromDisplayNameOrNull(it) },
                basePrice = item.basePrice,
                dailyTargetCount = item.dailyTargetCount,
                targetAmount = calculateTargetAmount(item.basePrice, item.dailyTargetCount),
                actualAmount = calculateActualAmount(item.primaryProductAmount, item.otherSalesAmount),
                primaryProductAmount = item.primaryProductAmount,
                primarySalesQuantity = item.primarySalesQuantity,
                primarySalesPrice = item.primarySalesPrice,
                otherSalesAmount = item.otherSalesAmount,
                otherSalesQuantity = item.otherSalesQuantity,
                s3ImageUniqueKey = item.s3ImageUniqueKey
            )

            promotionEmployeeRepository.save(pe)
        }

        // 전체 행사사원 목록 조회하여 응답
        val allEmployees = promotionEmployeeRepository.findWithEmployeeByPromotionId(promotionId)
        val responseItems = allEmployees.map { pe ->
            PromotionEmployeeListResponse.from(pe, pe.employee?.name, pe.employee?.employeeCode, pe.employee?.orgName, siteImageUrl(pe.s3ImageUniqueKey))
        }

        return BatchUpdatePromotionEmployeeResponse(
            updatedCount = request.items.size,
            items = responseItems
        )
    }

    @Transactional
    fun deleteEmployee(principal: WebUserPrincipal, id: Long) {
        val pe = findPromotionEmployeeById(id)
        val promotionId = pe.promotionId

        // 1-2-B: 마감 보호 — 삭제 차단
        // 레거시 PromotionEmployeeTriggerHandler.removeScheduleOnDelete 의 사번 00000009 예외 분기 유지
        if (pe.teamMemberScheduleId != null && pe.promoCloseByTm && pe.employee?.employeeCode != SPECIAL_BYPASS_EMPLOYEE_CODE) {
            throw ClosedEmployeeDeleteException()
        }

        // 1-4-B: 연쇄 삭제 — 스케줄 삭제
        // Spec #693 — cascade helper 로 validateDisplayMasterLink 가드 + MFEIS refresh 일관 적용
        // cascade 내부 refreshIntegration 의 SELECT auto-flush 시점에 PE 가 삭제될 schedule 을 참조하면
        // TransientPropertyValueException → PE 를 먼저 삭제·flush 하여 dangling reference 를 제거한 뒤 cascade.
        val scheduleId = pe.teamMemberScheduleId
        promotionEmployeeRepository.delete(pe)
        promotionEmployeeRepository.flush()

        if (scheduleId != null) {
            teamMemberScheduleCascadeHelper.cascadeDeleteByIds(principal, listOf(scheduleId))
        }
    }

    // --- Private helpers ---

    private fun resolveEmployee(employeeId: Long?): ResolvedEmployee? {
        if (employeeId == null) return null
        val employee = employeeRepository.findById(employeeId).orElse(null)
            ?: return ResolvedEmployee(id = employeeId, name = null, employeeCode = null, orgName = null)
        return ResolvedEmployee(id = employee.id, name = employee.name, employeeCode = employee.employeeCode, orgName = employee.orgName)
    }

    private fun calculateTargetAmount(basePrice: BigDecimal?, dailyTargetCount: BigDecimal?): Long? {
        return if (basePrice != null && dailyTargetCount != null) (basePrice * dailyTargetCount).toLong() else null
    }

    private fun calculateActualAmount(primaryProductAmount: BigDecimal?, otherSalesAmount: BigDecimal?): Long? {
        return if (primaryProductAmount == null && otherSalesAmount == null) null
        else ((primaryProductAmount ?: BigDecimal.ZERO) + (otherSalesAmount ?: BigDecimal.ZERO)).toLong()
    }

    private fun validateBatchItem(
        index: Int,
        item: BatchUpdatePromotionEmployeeItem,
        promotionId: Long,
        promotion: Promotion,
        isAdmin: Boolean,
        employeeMap: MutableMap<Long, PromotionEmployee>
    ): BatchItemError? {
        // 빈 문자열 → null 정규화 (비교 시 일관성)
        val normalizedWorkType3 = item.workType3?.takeIf { it.isNotBlank() }

        // 1. 존재 여부
        val pe = promotionEmployeeRepository.findById(item.id).orElse(null)
            ?: return BatchItemError(index, item.employeeId, "NOT_FOUND", "행사조원을 찾을 수 없습니다")
        employeeMap[item.id] = pe

        // 2. 소속 행사 일치
        if (pe.promotionId != promotionId) {
            return BatchItemError(index, item.employeeId, "INVALID_PARAMETER", "유효하지 않은 파라미터")
        }

        // 3. 투입일 범위
        if (item.scheduleDate.isBefore(promotion.startDate) || item.scheduleDate.isAfter(promotion.endDate)) {
            return BatchItemError(
                index, item.employeeId, "SCHEDULE_DATE_OUT_OF_RANGE",
                "투입일(${item.scheduleDate})이 행사 기간(${promotion.startDate} ~ ${promotion.endDate})을 벗어납니다"
            )
        }

        // 3-1. 기준단가·목표수량·목표금액 필수 (목표금액은 기준단가 × 목표수량 파생값)
        if (item.basePrice == null) {
            return BatchItemError(index, item.employeeId, "VALUES_REQUIRED", "기준단가는 필수 항목입니다")
        }
        if (item.dailyTargetCount == null) {
            return BatchItemError(index, item.employeeId, "VALUES_REQUIRED", "목표수량은 필수 항목입니다")
        }
        // 목표금액(= 기준단가 × 목표수량)이 0 이면 미입력으로 간주하여 저장 차단
        val targetAmount = calculateTargetAmount(item.basePrice, item.dailyTargetCount)
        if (targetAmount == null || targetAmount == 0L) {
            return BatchItemError(index, item.employeeId, "VALUES_REQUIRED", "목표금액은 필수 항목입니다 (기준단가 × 목표수량)")
        }

        // 4. 근무상태 (null 허용)
        if (item.workStatus != null && item.workStatus.isNotBlank() && item.workStatus !in VALID_WORK_STATUSES) {
            return BatchItemError(index, item.employeeId, "INVALID_WORK_STATUS", "근무상태는 근무, 연차, 대휴 중 하나여야 합니다")
        }

        // 5. 근무유형3 (null 허용)
        if (normalizedWorkType3 != null && normalizedWorkType3 !in VALID_WORK_TYPE3) {
            return BatchItemError(index, item.employeeId, "INVALID_WORK_TYPE3", "근무유형3은 고정, 격고, 순회 중 하나여야 합니다")
        }

        // 6. 대표제품 vs 전문행사조 매칭 검증 (레거시 동등)
        validateProfessionalTeamMatch(promotion, item.employeeId)?.let { mismatchMessage ->
            return BatchItemError(index, item.employeeId, "TEAM_CATEGORY_MISMATCH", mismatchMessage)
        }

        // 7. 마감 보호
        if (pe.teamMemberScheduleId != null && pe.promoCloseByTm) {
            val resolvedForValidation = resolveEmployee(item.employeeId)
            val criticalChanged = pe.employeeId != (resolvedForValidation?.id ?: item.employeeId) ||
                pe.scheduleDate != item.scheduleDate ||
                pe.workType3?.displayName != normalizedWorkType3 ||
                pe.basePrice != item.basePrice ||
                pe.dailyTargetCount != item.dailyTargetCount

            if (criticalChanged && !isAdmin) {
                return BatchItemError(
                    index, item.employeeId, "CLOSED_EMPLOYEE_MODIFICATION",
                    "확정되었고 여사원이 마감한 행사조원은 수정할 수 없습니다"
                )
            }
        }

        return null
    }

    private fun findActivePromotion(promotionId: Long): Promotion {
        val promotion = promotionRepository.findById(promotionId)
            .orElseThrow { PromotionNotFoundException() }
        if (promotion.isDeleted) throw PromotionNotFoundException()
        return promotion
    }

    private fun findPromotionEmployeeById(id: Long): PromotionEmployee {
        return promotionEmployeeRepository.findById(id)
            .orElseThrow { PromotionEmployeeNotFoundException() }
    }

    private fun validateScheduleDateRange(scheduleDate: LocalDate?, promotion: Promotion) {
        if (scheduleDate == null) return
        if (scheduleDate.isBefore(promotion.startDate) || scheduleDate.isAfter(promotion.endDate)) {
            throw ScheduleDateOutOfRangeException(
                scheduleDate.toString(),
                promotion.startDate.toString(),
                promotion.endDate.toString()
            )
        }
    }

    /**
     * 대표제품 vs 전문행사조 매칭 검증.
     *
     * 레거시 PromotionEmployeeTriggerHandler beforeInsert/beforeUpdate 동등 —
     * 좌변 promotion.category1 == "라면"/"냉장"/"냉동"/"만두" 정확 일치 (레거시 Apex `Category1__c == '라면'` 동등),
     * 우변 사원 team 은 카테고리 키워드 또는 "카레" contains 부분 매칭 (예: "라면세일조" contains "라면" → OK).
     * 사원 전문행사조가 null 이면 "일반" 사원으로 간주 — 카테고리 무관 OK.
     * promotion.category1 이 null 또는 매칭 룰 외 값이면 검증 스킵.
     *
     * @return 매칭 실패 시 차단 메시지 (한글), 매칭 OK 시 null
     */
    private fun validateProfessionalTeamMatch(promotion: Promotion, employeeId: Long?): String? {
        val category1 = promotion.category1 ?: return null
        val employee = employeeId?.let { employeeRepository.findById(it).orElse(null) } ?: return null
        val team = employee.professionalPromotionTeam?.displayName ?: return null

        return when (category1) {
            CATEGORY_RAMEN ->
                if (!(team.contains(CATEGORY_RAMEN) || team.contains("카레"))) MSG_MISMATCH_RAMEN else null

            CATEGORY_REFRIGERATED ->
                if (!(team.contains(CATEGORY_REFRIGERATED) || team.contains("카레"))) MSG_MISMATCH_REFRIGERATED else null

            CATEGORY_FROZEN ->
                if (!(team.contains(CATEGORY_FROZEN) || team.contains("카레"))) MSG_MISMATCH_FROZEN else null

            CATEGORY_DUMPLING ->
                if (!(team.contains(CATEGORY_DUMPLING) || team.contains(CATEGORY_FROZEN) || team.contains("카레")))
                    MSG_MISMATCH_DUMPLING
                else null

            else -> null
        }
    }

    private fun validateWorkStatus(workStatus: String) {
        if (workStatus !in VALID_WORK_STATUSES) throw InvalidWorkStatusException()
    }

    private fun validateWorkType3(workType3: String?) {
        if (workType3 == null) return
        if (workType3 !in VALID_WORK_TYPE3) throw InvalidWorkType3Exception()
    }

    private fun validateClosedEmployeeModification(
        pe: PromotionEmployee,
        newEmployeeId: Long?,
        scheduleDate: LocalDate?,
        workType3: String?,
        basePrice: BigDecimal?,
        dailyTargetCount: BigDecimal?,
        isAdmin: Boolean
    ) {
        if (pe.teamMemberScheduleId == null || !pe.promoCloseByTm) return

        val criticalChanged = pe.employeeId != newEmployeeId ||
            pe.scheduleDate != scheduleDate ||
            pe.workType3?.displayName != workType3 ||
            pe.basePrice != basePrice ||
            pe.dailyTargetCount != dailyTargetCount

        if (criticalChanged && !isAdmin) throw ClosedEmployeeModificationException()
    }

    // Spec #693 — cascade helper 적용. PE.scheduleId NULL 동기화는 본 함수가 유지.
    private fun removeScheduleOnCriticalFieldChange(
        principal: WebUserPrincipal,
        pe: PromotionEmployee,
        newEmployeeId: Long?,
        scheduleDate: LocalDate?,
        workType3: String?
    ) {
        val scheduleId = pe.teamMemberScheduleId ?: return

        val criticalChanged = pe.employeeId != newEmployeeId ||
            pe.scheduleDate != scheduleDate ||
            pe.workType3?.displayName != workType3

        if (criticalChanged) {
            // cascade 내부 refreshIntegration 의 SELECT 가 auto-flush 를 트리거하는데, 이때 PE 가 삭제될
            // TeamMemberSchedule 을 (FK 값 + 로드된 연관 객체로) 여전히 참조하면 TransientPropertyValueException.
            // schedule hard delete 전에 PE 의 연관을 먼저 끊고 flush 하여 dangling reference 를 제거한다.
            pe.teamMemberScheduleId = null
            pe.teamMemberScheduleSfid = null
            pe.teamMemberSchedule = null
            promotionEmployeeRepository.saveAndFlush(pe)

            teamMemberScheduleCascadeHelper.cascadeDeleteByIds(principal, listOf(scheduleId))
        }
    }

}
