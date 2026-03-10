package com.otoki.internal.admin.service

import com.otoki.internal.admin.dto.request.PromotionEmployeeRequest
import com.otoki.internal.admin.dto.response.PromotionEmployeeDetailResponse
import com.otoki.internal.admin.dto.response.PromotionEmployeeListResponse
import com.otoki.internal.promotion.entity.PromotionEmployee
import com.otoki.internal.promotion.exception.InvalidWorkStatusException
import com.otoki.internal.promotion.exception.InvalidWorkType3Exception
import com.otoki.internal.promotion.exception.PromotionEmployeeNotFoundException
import com.otoki.internal.promotion.exception.PromotionNotFoundException
import com.otoki.internal.promotion.repository.PromotionEmployeeRepository
import com.otoki.internal.promotion.repository.PromotionRepository
import com.otoki.internal.sap.repository.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class AdminPromotionEmployeeService(
    private val promotionEmployeeRepository: PromotionEmployeeRepository,
    private val promotionRepository: PromotionRepository,
    private val userRepository: UserRepository
) {

    companion object {
        private val VALID_WORK_STATUSES = setOf("근무", "연차", "대휴")
        private val VALID_WORK_TYPE3 = setOf("고정", "격고", "순회")
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
        validateWorkStatus(request.workStatus)
        validateWorkType3(request.workType3)

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
                dailyTargetCount = request.dailyTargetCount
            )
        )

        val employeeName = resolveEmployeeName(pe.employeeSfid)
        return PromotionEmployeeDetailResponse.from(pe, employeeName)
    }

    @Transactional
    fun updateEmployee(id: Long, request: PromotionEmployeeRequest): PromotionEmployeeDetailResponse {
        val pe = findEmployeeById(id)
        validateWorkStatus(request.workStatus)
        validateWorkType3(request.workType3)

        pe.update(
            employeeSfid = request.employeeSfid,
            scheduleDate = request.scheduleDate,
            workStatus = request.workStatus,
            workType1 = request.workType1,
            workType3 = request.workType3,
            workType4 = request.workType4,
            professionalPromotionTeam = request.professionalPromotionTeam,
            basePrice = request.basePrice,
            dailyTargetCount = request.dailyTargetCount
        )

        promotionEmployeeRepository.save(pe)

        val employeeName = resolveEmployeeName(pe.employeeSfid)
        return PromotionEmployeeDetailResponse.from(pe, employeeName)
    }

    @Transactional
    fun deleteEmployee(id: Long) {
        val pe = findEmployeeById(id)
        promotionEmployeeRepository.delete(pe)
    }

    private fun findActivePromotion(promotionId: Long) {
        val promotion = promotionRepository.findById(promotionId)
            .orElseThrow { PromotionNotFoundException() }
        if (promotion.isDeleted) throw PromotionNotFoundException()
    }

    private fun findEmployeeById(id: Long): PromotionEmployee {
        return promotionEmployeeRepository.findById(id)
            .orElseThrow { PromotionEmployeeNotFoundException() }
    }

    private fun validateWorkStatus(workStatus: String) {
        if (workStatus !in VALID_WORK_STATUSES) throw InvalidWorkStatusException()
    }

    private fun validateWorkType3(workType3: String) {
        if (workType3 !in VALID_WORK_TYPE3) throw InvalidWorkType3Exception()
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
