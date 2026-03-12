package com.otoki.internal.admin.service

import com.otoki.internal.admin.dto.request.PromotionEmployeeRequest
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
        val promotion = findActivePromotion(promotionId)
        validateScheduleDateRange(request.scheduleDate, promotion)
        validateWorkStatus(request.workStatus)
        validateWorkType3(request.workType3)
        validateTeamCategory(promotion, request.professionalPromotionTeam)

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
                actualAmount = request.actualAmount
            )
        )

        recalculatePromotionAmounts(promotionId)

        val employeeName = resolveEmployeeName(pe.employeeSfid)
        return PromotionEmployeeDetailResponse.from(pe, employeeName)
    }

    @Transactional
    fun updateEmployee(id: Long, userId: Long, request: PromotionEmployeeRequest): PromotionEmployeeDetailResponse {
        val pe = findEmployeeById(id)
        validateWorkStatus(request.workStatus)
        validateWorkType3(request.workType3)

        // 1-2-A: 마감 보호 — 핵심필드 수정 차단 (ADMIN 예외)
        val user = userRepository.findById(userId)
            .orElseThrow { IllegalStateException("사용자를 찾을 수 없습니다: $userId") }
        validateClosedEmployeeModification(pe, request, user.role == UserRole.ADMIN)

        // 1-1: 전문행사조 매칭 검증
        val promotion = findActivePromotion(pe.promotionId)
        validateScheduleDateRange(request.scheduleDate, promotion)
        validateTeamCategory(promotion, request.professionalPromotionTeam)

        // 1-5: 핵심필드 변경 시 스케줄 삭제
        removeScheduleOnCriticalFieldChange(pe, request)

        pe.update(
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
            actualAmount = request.actualAmount
        )

        promotionEmployeeRepository.save(pe)

        recalculatePromotionAmounts(pe.promotionId)

        val employeeName = resolveEmployeeName(pe.employeeSfid)
        return PromotionEmployeeDetailResponse.from(pe, employeeName)
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

    private fun validateWorkType3(workType3: String) {
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

    private fun validateClosedEmployeeModification(pe: PromotionEmployee, request: PromotionEmployeeRequest, isAdmin: Boolean) {
        if (pe.scheduleId == null || !pe.promoCloseByTm) return

        val criticalChanged = pe.employeeSfid != request.employeeSfid ||
            pe.scheduleDate != request.scheduleDate ||
            pe.workType3 != request.workType3 ||
            pe.basePrice != request.basePrice ||
            pe.dailyTargetCount != request.dailyTargetCount

        if (criticalChanged && !isAdmin) throw ClosedEmployeeModificationException()
    }

    private fun removeScheduleOnCriticalFieldChange(pe: PromotionEmployee, request: PromotionEmployeeRequest) {
        if (pe.scheduleId == null) return
        if (pe.professionalPromotionTeam != request.professionalPromotionTeam) return

        val criticalChanged = pe.employeeSfid != request.employeeSfid ||
            pe.scheduleDate != request.scheduleDate ||
            pe.workType3 != request.workType3

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

    private fun resolveEmployeeNames(employees: List<PromotionEmployee>): Map<String, String> {
        val sfids = employees.map { it.employeeSfid }.distinct()
        if (sfids.isEmpty()) return emptyMap()
        return userRepository.findBySfidIn(sfids).associate { it.sfid!! to it.name }
    }

    private fun resolveEmployeeName(sfid: String): String? {
        return userRepository.findBySfidIn(listOf(sfid)).firstOrNull()?.name
    }
}
