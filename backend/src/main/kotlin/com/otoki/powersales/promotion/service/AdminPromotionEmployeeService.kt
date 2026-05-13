package com.otoki.powersales.promotion.service

import com.otoki.powersales.promotion.dto.request.BatchUpdatePromotionEmployeeItem
import com.otoki.powersales.promotion.dto.request.BatchUpdatePromotionEmployeeRequest
import com.otoki.powersales.promotion.dto.request.PromotionEmployeeRequest
import com.otoki.powersales.promotion.dto.response.BatchItemError
import com.otoki.powersales.promotion.dto.response.BatchUpdatePromotionEmployeeResponse
import com.otoki.powersales.promotion.dto.response.PromotionEmployeeDetailResponse
import com.otoki.powersales.promotion.dto.response.PromotionEmployeeListResponse
import com.otoki.powersales.promotion.entity.Promotion
import com.otoki.powersales.promotion.entity.PromotionEmployee
import com.otoki.powersales.promotion.exception.*
import com.otoki.powersales.promotion.repository.PromotionEmployeeRepository
import com.otoki.powersales.promotion.repository.PromotionRepository
import com.otoki.powersales.auth.entity.UserRole
import com.otoki.powersales.employee.repository.EmployeeRepository
import com.otoki.powersales.schedule.repository.TeamMemberScheduleRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class AdminPromotionEmployeeService(
    private val promotionEmployeeRepository: PromotionEmployeeRepository,
    private val promotionRepository: PromotionRepository,
    private val employeeRepository: EmployeeRepository,
    private val teamMemberScheduleRepository: TeamMemberScheduleRepository
) {

    companion object {
        private val VALID_WORK_STATUSES = setOf("근무", "연차", "대휴")
        private val VALID_WORK_TYPE3 = setOf("고정", "격고", "순회")
        private const val BATCH_MAX_SIZE = 200
        private const val DEFAULT_WORK_TYPE1 = "행사"
        private const val DEFAULT_WORK_STATUS = "근무"
    }

    private data class ResolvedEmployee(val id: Long?, val name: String?, val employeeCode: String?)

    fun getEmployees(promotionId: Long): List<PromotionEmployeeListResponse> {
        findActivePromotion(promotionId)

        val employees = promotionEmployeeRepository.findWithEmployeeByPromotionId(promotionId)

        return employees.map { pe ->
            PromotionEmployeeListResponse.from(pe, pe.employee?.name, pe.employee?.employeeCode)
        }
    }

    fun getEmployee(id: Long): PromotionEmployeeDetailResponse {
        val pe = findPromotionEmployeeById(id)
        return PromotionEmployeeDetailResponse.from(pe, pe.employee?.name, pe.employee?.employeeCode)
    }

    @Transactional
    fun createEmployee(promotionId: Long, request: PromotionEmployeeRequest): PromotionEmployeeDetailResponse {
        findActivePromotion(promotionId)

        val resolved = resolveEmployee(request.employeeId)

        val pe = promotionEmployeeRepository.save(
            PromotionEmployee(
                promotionId = promotionId,
                employeeId = resolved?.id ?: request.employeeId,
                scheduleDate = request.scheduleDate,
                workStatus = request.workStatus ?: DEFAULT_WORK_STATUS,
                workType1 = request.workType1 ?: DEFAULT_WORK_TYPE1,
                workType3 = request.workType3,
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
    fun updateEmployee(id: Long, userId: Long, request: PromotionEmployeeRequest): PromotionEmployeeDetailResponse {
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
            request.basePrice, request.dailyTargetCount, employee.role == UserRole.BRANCH_MANAGER
        )

        val promotion = pe.promotion ?: throw PromotionNotFoundException()
        if (promotion.isDeleted) throw PromotionNotFoundException()
        validateScheduleDateRange(request.scheduleDate, promotion)

        // 1-5: 핵심필드 변경 시 스케줄 삭제
        removeScheduleOnCriticalFieldChange(
            pe, resolved?.id, request.scheduleDate, normalizedWorkType3
        )

        pe.update(
            employeeId = resolved?.id,
            scheduleDate = request.scheduleDate,
            workStatus = request.workStatus ?: pe.workStatus,
            workType1 = request.workType1 ?: pe.workType1,
            workType3 = normalizedWorkType3,
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
        val isAdmin = employee.role == UserRole.BRANCH_MANAGER

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
                pe, resolved?.id, item.scheduleDate, normalizedWorkType3
            )

            pe.update(
                employeeId = resolved?.id,
                scheduleDate = item.scheduleDate,
                workStatus = item.workStatus ?: pe.workStatus ?: DEFAULT_WORK_STATUS,
                workType1 = item.workType1 ?: pe.workType1 ?: DEFAULT_WORK_TYPE1,
                workType3 = normalizedWorkType3,
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
    fun deleteEmployee(id: Long) {
        val pe = findPromotionEmployeeById(id)
        val promotionId = pe.promotionId

        // 1-2-B: 마감 보호 — 삭제 차단
        if (pe.teamMemberScheduleId != null && pe.promoCloseByTm) {
            throw ClosedEmployeeDeleteException()
        }

        // 1-4-B: 연쇄 삭제 — 스케줄 삭제
        if (pe.teamMemberScheduleId != null) {
            teamMemberScheduleRepository.deleteAllByIdIn(listOf(pe.teamMemberScheduleId!!))
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

    private fun calculateTargetAmount(basePrice: Long?, dailyTargetCount: Int?): Long? {
        return if (basePrice != null && dailyTargetCount != null) basePrice * dailyTargetCount else null
    }

    private fun calculateActualAmount(primaryProductAmount: Long?, otherSalesAmount: Long?): Long? {
        return if (primaryProductAmount == null && otherSalesAmount == null) null
        else (primaryProductAmount ?: 0) + (otherSalesAmount ?: 0)
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

        // 7. 마감 보호
        if (pe.teamMemberScheduleId != null && pe.promoCloseByTm) {
            val resolvedForValidation = resolveEmployee(item.employeeId)
            val criticalChanged = pe.employeeId != (resolvedForValidation?.id ?: item.employeeId) ||
                pe.scheduleDate != item.scheduleDate ||
                pe.workType3 != normalizedWorkType3 ||
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
        basePrice: Long?,
        dailyTargetCount: Int?,
        isAdmin: Boolean
    ) {
        if (pe.teamMemberScheduleId == null || !pe.promoCloseByTm) return

        val criticalChanged = pe.employeeId != newEmployeeId ||
            pe.scheduleDate != scheduleDate ||
            pe.workType3 != workType3 ||
            pe.basePrice != basePrice ||
            pe.dailyTargetCount != dailyTargetCount

        if (criticalChanged && !isAdmin) throw ClosedEmployeeModificationException()
    }

    private fun removeScheduleOnCriticalFieldChange(
        pe: PromotionEmployee,
        newEmployeeId: Long?,
        scheduleDate: java.time.LocalDate?,
        workType3: String?
    ) {
        if (pe.teamMemberScheduleId == null) return

        val criticalChanged = pe.employeeId != newEmployeeId ||
            pe.scheduleDate != scheduleDate ||
            pe.workType3 != workType3

        if (criticalChanged) {
            teamMemberScheduleRepository.deleteAllByIdIn(listOf(pe.teamMemberScheduleId!!))
            pe.teamMemberScheduleId = null
        }
    }

}
