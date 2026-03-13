package com.otoki.internal.admin.service

import com.otoki.internal.admin.dto.request.BatchUpdatePromotionEmployeeItem
import com.otoki.internal.admin.dto.request.BatchUpdatePromotionEmployeeRequest
import com.otoki.internal.admin.dto.request.PromotionEmployeeRequest
import com.otoki.internal.admin.dto.response.BatchItemError
import com.otoki.internal.admin.dto.response.BatchUpdatePromotionEmployeeResponse
import com.otoki.internal.admin.dto.response.PromotionEmployeeDetailResponse
import com.otoki.internal.admin.dto.response.PromotionEmployeeListResponse
import com.otoki.internal.promotion.entity.Promotion
import com.otoki.internal.promotion.entity.PromotionEmployee
import com.otoki.internal.promotion.exception.*
import com.otoki.internal.promotion.repository.PromotionEmployeeRepository
import com.otoki.internal.promotion.repository.PromotionRepository
import com.otoki.internal.sap.entity.UserRole
import com.otoki.internal.sap.repository.UserRepository
import com.otoki.internal.schedule.repository.ScheduleRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class AdminPromotionEmployeeService(
    private val promotionEmployeeRepository: PromotionEmployeeRepository,
    private val promotionRepository: PromotionRepository,
    private val userRepository: UserRepository,
    private val scheduleRepository: ScheduleRepository
) {

    companion object {
        private val VALID_WORK_STATUSES = setOf("근무", "연차", "대휴")
        private val VALID_WORK_TYPE3 = setOf("고정", "격고", "순회")
        private const val BATCH_MAX_SIZE = 200

        private val CATEGORY_TEAM_RULES: Map<String, List<String>> = mapOf(
            "라면" to listOf("라면"),
            "냉장" to listOf("냉장"),
            "냉동" to listOf("냉동"),
            "만두" to listOf("만두", "냉동")
        )

        private val CATEGORY_TEAM_MESSAGES: Map<String, String> = mapOf(
            "라면" to "대표제품이 라면인 행사에는 전문행사조가 라면세일조 혹은 일반인 사원만 배정 가능합니다",
            "냉장" to "대표제품이 냉장인 행사에는 전문행사조가 프레시세일조_냉장 혹은 일반인 사원만 배정 가능합니다",
            "냉동" to "대표제품이 냉동인 행사에는 전문행사조가 프레시세일조_냉동 혹은 일반인 사원만 배정 가능합니다",
            "만두" to "대표제품이 만두인 행사에는 전문행사조가 프레시세일조_냉동, 프레시세일조_만두 혹은 일반인 사원만 배정 가능합니다"
        )
    }

    fun getEmployees(promotionId: Long): List<PromotionEmployeeListResponse> {
        findActivePromotion(promotionId)

        val employees = promotionEmployeeRepository.findByPromotionIdOrderByScheduleDateAsc(promotionId)
        val employeeNameMap = resolveEmployeeNames(employees)

        return employees.map { pe ->
            PromotionEmployeeListResponse.from(pe, employeeNameMap[pe.employeeSfid])
        }
    }

    fun getEmployee(id: Long): PromotionEmployeeDetailResponse {
        val pe = findEmployeeById(id)
        val employeeName = resolveEmployeeName(pe.employeeSfid)
        return PromotionEmployeeDetailResponse.from(pe, employeeName)
    }

    @Transactional
    fun createEmployee(promotionId: Long, request: PromotionEmployeeRequest): PromotionEmployeeDetailResponse {
        findActivePromotion(promotionId)

        val pe = promotionEmployeeRepository.save(
            PromotionEmployee(
                promotionId = promotionId,
                employeeSfid = request.employeeSfid,
                scheduleDate = request.scheduleDate,
                workStatus = request.workStatus,
                workType1 = request.workType1,
                workType3 = request.workType3,
                workType4 = request.workType4,
                professionalPromotionTeam = request.professionalPromotionTeam,
                basePrice = request.basePrice,
                dailyTargetCount = request.dailyTargetCount,
                targetAmount = request.targetAmount,
                actualAmount = request.actualAmount,
                primaryProductAmount = request.primaryProductAmount,
                primarySalesQuantity = request.primarySalesQuantity,
                primarySalesPrice = request.primarySalesPrice,
                otherSalesAmount = request.otherSalesAmount,
                otherSalesQuantity = request.otherSalesQuantity,
                s3ImageUniqueKey = request.s3ImageUniqueKey
            )
        )

        recalculatePromotionAmounts(promotionId)

        val employeeName = resolveEmployeeName(pe.employeeSfid)
        return PromotionEmployeeDetailResponse.from(pe, employeeName)
    }

    @Transactional
    fun updateEmployee(id: Long, userId: Long, request: PromotionEmployeeRequest): PromotionEmployeeDetailResponse {
        val pe = findEmployeeById(id)

        // 빈 문자열 → null 정규화
        val normalizedEmployeeSfid = request.employeeSfid?.takeIf { it.isNotBlank() }
        val normalizedWorkType3 = request.workType3?.takeIf { it.isNotBlank() }

        validateWorkStatus(request.workStatus!!)
        validateWorkType3(normalizedWorkType3)

        // 1-2-A: 마감 보호 — 핵심필드 수정 차단 (ADMIN 예외)
        val user = userRepository.findById(userId)
            .orElseThrow { IllegalStateException("사용자를 찾을 수 없습니다: $userId") }
        validateClosedEmployeeModification(
            pe, normalizedEmployeeSfid, request.scheduleDate!!, normalizedWorkType3,
            request.basePrice, request.dailyTargetCount, user.role == UserRole.ADMIN
        )

        // 1-1: 전문행사조 매칭 검증
        val promotion = findActivePromotion(pe.promotionId)
        validateScheduleDateRange(request.scheduleDate!!, promotion)
        validateTeamCategory(promotion, request.professionalPromotionTeam)

        // 1-5: 핵심필드 변경 시 스케줄 삭제
        removeScheduleOnCriticalFieldChange(
            pe, normalizedEmployeeSfid, request.scheduleDate!!, normalizedWorkType3,
            request.professionalPromotionTeam
        )

        pe.update(
            employeeSfid = normalizedEmployeeSfid,
            scheduleDate = request.scheduleDate!!,
            workStatus = request.workStatus!!,
            workType1 = request.workType1!!,
            workType3 = normalizedWorkType3,
            workType4 = request.workType4,
            professionalPromotionTeam = request.professionalPromotionTeam,
            basePrice = request.basePrice,
            dailyTargetCount = request.dailyTargetCount,
            targetAmount = request.targetAmount,
            actualAmount = request.actualAmount,
            primaryProductAmount = request.primaryProductAmount,
            primarySalesQuantity = request.primarySalesQuantity,
            primarySalesPrice = request.primarySalesPrice,
            otherSalesAmount = request.otherSalesAmount,
            otherSalesQuantity = request.otherSalesQuantity,
            s3ImageUniqueKey = request.s3ImageUniqueKey
        )

        promotionEmployeeRepository.save(pe)

        recalculatePromotionAmounts(pe.promotionId)

        val employeeName = resolveEmployeeName(pe.employeeSfid)
        return PromotionEmployeeDetailResponse.from(pe, employeeName)
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
        val user = userRepository.findById(userId)
            .orElseThrow { IllegalStateException("사용자를 찾을 수 없습니다: $userId") }
        val isAdmin = user.role == UserRole.ADMIN

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
            val normalizedEmployeeSfid = item.employeeSfid?.takeIf { it.isNotBlank() }
            val normalizedWorkType3 = item.workType3?.takeIf { it.isNotBlank() }

            removeScheduleOnCriticalFieldChange(
                pe, normalizedEmployeeSfid, item.scheduleDate, normalizedWorkType3,
                item.professionalPromotionTeam
            )

            pe.update(
                employeeSfid = normalizedEmployeeSfid,
                scheduleDate = item.scheduleDate,
                workStatus = item.workStatus,
                workType1 = item.workType1,
                workType3 = normalizedWorkType3,
                workType4 = item.workType4,
                professionalPromotionTeam = item.professionalPromotionTeam,
                basePrice = item.basePrice,
                dailyTargetCount = item.dailyTargetCount,
                targetAmount = item.targetAmount,
                actualAmount = item.actualAmount,
                primaryProductAmount = item.primaryProductAmount,
                primarySalesQuantity = item.primarySalesQuantity,
                primarySalesPrice = item.primarySalesPrice,
                otherSalesAmount = item.otherSalesAmount,
                otherSalesQuantity = item.otherSalesQuantity,
                s3ImageUniqueKey = item.s3ImageUniqueKey
            )

            promotionEmployeeRepository.save(pe)
        }

        // 롤업 재계산 (1회)
        recalculatePromotionAmounts(promotionId)

        // 전체 행사사원 목록 조회하여 응답
        val allEmployees = promotionEmployeeRepository.findByPromotionIdOrderByScheduleDateAsc(promotionId)
        val employeeNameMap = resolveEmployeeNames(allEmployees)
        val responseItems = allEmployees.map { pe ->
            PromotionEmployeeListResponse.from(pe, employeeNameMap[pe.employeeSfid])
        }

        return BatchUpdatePromotionEmployeeResponse(
            updatedCount = request.items.size,
            items = responseItems
        )
    }

    @Transactional
    fun deleteEmployee(id: Long) {
        val pe = findEmployeeById(id)
        val promotionId = pe.promotionId

        // 1-2-B: 마감 보호 — 삭제 차단
        if (pe.scheduleId != null && pe.promoCloseByTm) {
            throw ClosedEmployeeDeleteException()
        }

        // 1-4-B: 연쇄 삭제 — 스케줄 삭제
        if (pe.scheduleId != null) {
            scheduleRepository.deleteAllByIdIn(listOf(pe.scheduleId!!))
        }

        promotionEmployeeRepository.delete(pe)
        promotionEmployeeRepository.flush()

        recalculatePromotionAmounts(promotionId)
    }

    // --- Private helpers ---

    private fun validateBatchItem(
        index: Int,
        item: BatchUpdatePromotionEmployeeItem,
        promotionId: Long,
        promotion: Promotion,
        isAdmin: Boolean,
        employeeMap: MutableMap<Long, PromotionEmployee>
    ): BatchItemError? {
        // 빈 문자열 → null 정규화 (비교 시 일관성)
        val normalizedEmployeeSfid = item.employeeSfid?.takeIf { it.isNotBlank() }
        val normalizedWorkType3 = item.workType3?.takeIf { it.isNotBlank() }

        // 1. 존재 여부
        val pe = promotionEmployeeRepository.findById(item.id).orElse(null)
            ?: return BatchItemError(index, item.employeeSfid, "NOT_FOUND", "행사조원을 찾을 수 없습니다")
        employeeMap[item.id] = pe

        // 2. 소속 행사 일치
        if (pe.promotionId != promotionId) {
            return BatchItemError(index, item.employeeSfid, "INVALID_PARAMETER", "유효하지 않은 파라미터")
        }

        // 3. 투입일 범위
        if (item.scheduleDate.isBefore(promotion.startDate) || item.scheduleDate.isAfter(promotion.endDate)) {
            return BatchItemError(
                index, item.employeeSfid, "SCHEDULE_DATE_OUT_OF_RANGE",
                "투입일(${item.scheduleDate})이 행사 기간(${promotion.startDate} ~ ${promotion.endDate})을 벗어납니다"
            )
        }

        // 4. 근무상태
        if (item.workStatus !in VALID_WORK_STATUSES) {
            return BatchItemError(index, item.employeeSfid, "INVALID_WORK_STATUS", "근무상태는 근무, 연차, 대휴 중 하나여야 합니다")
        }

        // 5. 근무유형3 (null 허용)
        if (normalizedWorkType3 != null && normalizedWorkType3 !in VALID_WORK_TYPE3) {
            return BatchItemError(index, item.employeeSfid, "INVALID_WORK_TYPE3", "근무유형3은 고정, 격고, 순회 중 하나여야 합니다")
        }

        // 6. 전문행사조-카테고리 매칭
        val team = item.professionalPromotionTeam
        if (!team.isNullOrBlank() && team != "일반") {
            val category = promotion.category
            if (category != null) {
                val allowedKeywords = CATEGORY_TEAM_RULES[category]
                if (allowedKeywords != null) {
                    val matches = allowedKeywords.any { keyword -> team.contains(keyword) }
                    if (!matches) {
                        return BatchItemError(
                            index, item.employeeSfid, "TEAM_CATEGORY_MISMATCH",
                            CATEGORY_TEAM_MESSAGES[category] ?: "전문행사조가 행사 카테고리와 일치하지 않습니다"
                        )
                    }
                }
            }
        }

        // 7. 마감 보호
        if (pe.scheduleId != null && pe.promoCloseByTm) {
            val criticalChanged = pe.employeeSfid != normalizedEmployeeSfid ||
                pe.scheduleDate != item.scheduleDate ||
                pe.workType3 != normalizedWorkType3 ||
                pe.basePrice != item.basePrice ||
                pe.dailyTargetCount != item.dailyTargetCount

            if (criticalChanged && !isAdmin) {
                return BatchItemError(
                    index, item.employeeSfid, "CLOSED_EMPLOYEE_MODIFICATION",
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

    private fun findEmployeeById(id: Long): PromotionEmployee {
        return promotionEmployeeRepository.findById(id)
            .orElseThrow { PromotionEmployeeNotFoundException() }
    }

    private fun validateScheduleDateRange(scheduleDate: java.time.LocalDate, promotion: Promotion) {
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

    private fun validateTeamCategory(promotion: Promotion, team: String?) {
        if (team.isNullOrBlank() || team == "일반") return
        val category = promotion.category ?: return
        val allowedKeywords = CATEGORY_TEAM_RULES[category] ?: return

        val matches = allowedKeywords.any { keyword -> team.contains(keyword) }
        if (!matches) {
            throw TeamCategoryMismatchException(
                CATEGORY_TEAM_MESSAGES[category] ?: "전문행사조가 행사 카테고리와 일치하지 않습니다"
            )
        }
    }

    private fun validateClosedEmployeeModification(
        pe: PromotionEmployee,
        employeeSfid: String?,
        scheduleDate: java.time.LocalDate,
        workType3: String?,
        basePrice: Long?,
        dailyTargetCount: Int?,
        isAdmin: Boolean
    ) {
        if (pe.scheduleId == null || !pe.promoCloseByTm) return

        val criticalChanged = pe.employeeSfid != employeeSfid ||
            pe.scheduleDate != scheduleDate ||
            pe.workType3 != workType3 ||
            pe.basePrice != basePrice ||
            pe.dailyTargetCount != dailyTargetCount

        if (criticalChanged && !isAdmin) throw ClosedEmployeeModificationException()
    }

    private fun removeScheduleOnCriticalFieldChange(
        pe: PromotionEmployee,
        employeeSfid: String?,
        scheduleDate: java.time.LocalDate,
        workType3: String?,
        professionalPromotionTeam: String?
    ) {
        if (pe.scheduleId == null) return
        if (pe.professionalPromotionTeam != professionalPromotionTeam) return

        val criticalChanged = pe.employeeSfid != employeeSfid ||
            pe.scheduleDate != scheduleDate ||
            pe.workType3 != workType3

        if (criticalChanged) {
            scheduleRepository.deleteAllByIdIn(listOf(pe.scheduleId!!))
            pe.scheduleId = null
        }
    }

    private fun recalculatePromotionAmounts(promotionId: Long) {
        val promotion = promotionRepository.findById(promotionId)
            .orElseThrow { PromotionNotFoundException() }
        val sumTarget = promotionEmployeeRepository.sumTargetAmountByPromotionId(promotionId)
        val sumActual = promotionEmployeeRepository.sumActualAmountByPromotionId(promotionId)
        promotion.updateAmounts(sumTarget, sumActual)
        promotionRepository.save(promotion)
    }

    private fun resolveEmployeeNames(employees: List<PromotionEmployee>): Map<String?, String> {
        val sfids = employees.mapNotNull { it.employeeSfid }.distinct()
        if (sfids.isEmpty()) return emptyMap()
        return userRepository.findBySfidIn(sfids).associate { it.sfid!! to it.name }
    }

    private fun resolveEmployeeName(sfid: String?): String? {
        if (sfid == null) return null
        return userRepository.findBySfidIn(listOf(sfid)).firstOrNull()?.name
    }
}
