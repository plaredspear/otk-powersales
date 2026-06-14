package com.otoki.powersales.promotion.service

import com.otoki.powersales.platform.common.enums.WorkingCategory1
import com.otoki.powersales.platform.common.enums.WorkingCategory3
import com.otoki.powersales.platform.common.enums.WorkingType
import com.otoki.powersales.promotion.dto.request.BatchUpdatePromotionEmployeeItem
import com.otoki.powersales.promotion.dto.request.BatchUpdatePromotionEmployeeRequest
import com.otoki.powersales.promotion.dto.request.PromotionEmployeeRequest
import com.otoki.powersales.promotion.dto.response.BatchItemError
import com.otoki.powersales.promotion.dto.response.BatchUpdatePromotionEmployeeResponse
import com.otoki.powersales.promotion.dto.response.PromotionEmployeeDetailResponse
import com.otoki.powersales.promotion.dto.response.PromotionEmployeeListResponse
import com.otoki.powersales.promotion.entity.Promotion
import com.otoki.powersales.promotion.entity.PromotionEmployee
import com.otoki.powersales.promotion.entity.QPromotion.Companion.promotion as qPromotion
import com.otoki.powersales.promotion.exception.*
import com.otoki.powersales.promotion.repository.PromotionEmployeeRepository
import com.otoki.powersales.promotion.repository.PromotionRepository
import com.otoki.powersales.admin.dto.DataScope
import com.otoki.powersales.platform.auth.entity.AppAuthority
import com.otoki.powersales.platform.auth.sharing.service.SharingRulePolicyEvaluator
import com.otoki.powersales.platform.auth.web.WebUserPrincipal
import com.otoki.powersales.employee.repository.EmployeeRepository
import com.otoki.powersales.schedule.repository.TeamMemberScheduleRepository
import com.otoki.powersales.schedule.service.TeamMemberScheduleCascadeHelper
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal

@Service
@Transactional(readOnly = true)
class AdminPromotionEmployeeService(
    private val promotionEmployeeRepository: PromotionEmployeeRepository,
    private val promotionRepository: PromotionRepository,
    private val employeeRepository: EmployeeRepository,
    private val teamMemberScheduleRepository: TeamMemberScheduleRepository,
    private val policyEvaluator: SharingRulePolicyEvaluator,
    private val teamMemberScheduleCascadeHelper: TeamMemberScheduleCascadeHelper,
) {

    /**
     * SF Sharing Rule 정책이 합성된 가시 PromotionEmployee 일람 (spec #782 P4-B — ControlledByParent).
     *
     * SF_SHARING_MODEL[DKRetail__PromotionEmployee__c] = ControlledByParent (parent = DKRetail__Promotion__c).
     * 본 메서드는 부모 Promotion entity 기준 [SharingRulePolicyEvaluator.buildPredicate] 호출 후 결과 Predicate 를
     * [PromotionEmployeeRepository.findAllAccessibleByParentPolicy] 에 전달.
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
        private val VALID_WORK_STATUSES = setOf("근무", "연차", "대휴")
        private val VALID_WORK_TYPE3 = setOf("고정", "격고", "순회")
        private const val BATCH_MAX_SIZE = 200
        private val DEFAULT_WORK_TYPE1 = WorkingCategory1.EVENT
        private val DEFAULT_WORK_STATUS = WorkingType.WORK
        // 레거시 동등: 여사원 마감 행 삭제 차단 우회 대상 사번
        private const val SPECIAL_BYPASS_EMPLOYEE_CODE = "00000009"

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

    private data class ResolvedEmployee(val id: Long?, val name: String?, val employeeCode: String?)

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
            PromotionEmployeeListResponse.from(pe, pe.employee?.name, pe.employee?.employeeCode)
        }
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
        return PromotionEmployeeDetailResponse.from(pe, pe.employee?.name, pe.employee?.employeeCode)
    }

    @Transactional
    fun createEmployee(promotionId: Long, request: PromotionEmployeeRequest): PromotionEmployeeDetailResponse {
        val promotion = findActivePromotion(promotionId)

        val resolved = resolveEmployee(request.employeeId)

        // 레거시 PromotionEmployeeTriggerHandler 동등: 대표제품 vs 전문행사조 매칭 검증
        validateProfessionalTeamMatch(promotion, resolved?.id ?: request.employeeId)
            ?.let { throw TeamCategoryMismatchException(it) }

        val pe = promotionEmployeeRepository.save(
            PromotionEmployee(
                promotionId = promotionId,
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

        return PromotionEmployeeDetailResponse.from(pe, resolved?.name, resolved?.employeeCode)
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

        return PromotionEmployeeDetailResponse.from(pe, resolved?.name, resolved?.employeeCode)
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
            PromotionEmployeeListResponse.from(pe, pe.employee?.name, pe.employee?.employeeCode)
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
        val scheduleId = pe.teamMemberScheduleId
        if (scheduleId != null) {
            teamMemberScheduleCascadeHelper.cascadeDeleteByIds(principal, listOf(scheduleId))
        }

        promotionEmployeeRepository.delete(pe)
        promotionEmployeeRepository.flush()
    }

    // --- Private helpers ---

    private fun resolveEmployee(employeeId: Long?): ResolvedEmployee? {
        if (employeeId == null) return null
        val employee = employeeRepository.findById(employeeId).orElse(null)
            ?: return ResolvedEmployee(id = employeeId, name = null, employeeCode = null)
        return ResolvedEmployee(id = employee.id, name = employee.name, employeeCode = employee.employeeCode)
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

    private fun validateScheduleDateRange(scheduleDate: java.time.LocalDate?, promotion: Promotion) {
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
        scheduleDate: java.time.LocalDate?,
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
        scheduleDate: java.time.LocalDate?,
        workType3: String?
    ) {
        val scheduleId = pe.teamMemberScheduleId ?: return

        val criticalChanged = pe.employeeId != newEmployeeId ||
            pe.scheduleDate != scheduleDate ||
            pe.workType3?.displayName != workType3

        if (criticalChanged) {
            teamMemberScheduleCascadeHelper.cascadeDeleteByIds(principal, listOf(scheduleId))
            pe.teamMemberScheduleId = null
        }
    }

}
