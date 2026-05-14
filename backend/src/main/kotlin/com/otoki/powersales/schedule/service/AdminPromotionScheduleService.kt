package com.otoki.powersales.schedule.service

import com.otoki.powersales.account.repository.AccountRepository
import com.otoki.powersales.common.enums.WorkingCategory1
import com.otoki.powersales.common.enums.WorkingCategory3
import com.otoki.powersales.schedule.dto.request.PromotionScheduleBulkDeleteRequest
import com.otoki.powersales.schedule.dto.request.PromotionScheduleBulkUpdateItem
import com.otoki.powersales.schedule.dto.request.PromotionScheduleBulkUpdateRequest
import com.otoki.powersales.schedule.dto.response.PromotionScheduleBulkDeleteResponse
import com.otoki.powersales.schedule.dto.response.PromotionScheduleBulkUpdateResponse
import com.otoki.powersales.schedule.dto.response.PromotionScheduleItem
import com.otoki.powersales.schedule.dto.response.PromotionScheduleListResponse
import com.otoki.powersales.schedule.dto.response.PromotionScheduleMember
import com.otoki.powersales.schedule.dto.response.SchedulePeriod
import com.otoki.powersales.promotion.entity.Promotion
import com.otoki.powersales.promotion.exception.PromotionNotFoundException
import com.otoki.powersales.promotion.repository.PromotionEmployeeRepository
import com.otoki.powersales.promotion.repository.PromotionRepository
import com.otoki.powersales.schedule.entity.TeamMemberSchedule
import com.otoki.powersales.schedule.exception.PromotionScheduleBulkDeleteInvalidSizeException
import com.otoki.powersales.schedule.exception.PromotionScheduleBulkDuplicateException
import com.otoki.powersales.schedule.exception.PromotionScheduleBulkInvalidSizeException
import com.otoki.powersales.schedule.exception.PromotionScheduleInvalidWorkingCategoryException
import com.otoki.powersales.schedule.exception.PromotionScheduleNotFoundPartialException
import com.otoki.powersales.schedule.exception.PromotionScheduleNotInPromotionException
import com.otoki.powersales.schedule.exception.PromotionScheduleWorkingDateOutOfPromotionException
import com.otoki.powersales.schedule.exception.TeamScheduleAccountNotFoundException
import com.otoki.powersales.schedule.exception.TeamScheduleNotFoundException
import com.otoki.powersales.schedule.repository.TeamMemberScheduleRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import kotlin.collections.get

/**
 * 행사 단위 일정 일괄 변경/삭제 서비스 (Spec #571 P1-B).
 *
 * 단건 변경은 `AdminTeamScheduleService` 가 담당하며, 본 서비스는 한 행사의 배치원 일정 N건을
 * 단일 트랜잭션 안에서 변경/삭제한다. 충돌/존재 검증은 `TeamScheduleValidator` 를 공유한다.
 */
@Service
@Transactional(readOnly = true)
class AdminPromotionScheduleService(
    private val promotionRepository: PromotionRepository,
    private val promotionEmployeeRepository: PromotionEmployeeRepository,
    private val teamMemberScheduleRepository: TeamMemberScheduleRepository,
    private val accountRepository: AccountRepository,
    private val teamScheduleValidator: TeamScheduleValidator
) {

    companion object {
        private val VALID_WORKING_CATEGORY1 = setOf("행사", "진열")
        private val VALID_WORKING_CATEGORY3 = setOf("고정", "순회", "격고")
        private const val BULK_MAX_SIZE = 500
    }

    fun getSchedules(
        promotionId: Long,
        startDate: LocalDate?,
        endDate: LocalDate?
    ): PromotionScheduleListResponse {
        val promotion = findActivePromotion(promotionId)
        val effectiveStart = startDate ?: promotion.startDate
        val effectiveEnd = endDate ?: promotion.endDate

        val promotionEmployees = promotionEmployeeRepository.findWithEmployeeByPromotionId(promotionId)
        val employeeIds = promotionEmployees.mapNotNull { it.employeeId }.distinct()

        val schedules: List<TeamMemberSchedule> = if (employeeIds.isEmpty()) {
            emptyList()
        } else {
            teamMemberScheduleRepository.findMonthlyByEmployeeIds(employeeIds, effectiveStart, effectiveEnd)
                .filter { schedule ->
                    val pe = schedule.promotionEmployee
                    pe != null && pe.promotionId == promotionId
                }
        }

        val schedulesByEmployeeId = schedules.groupBy { it.employee?.id }

        val members = promotionEmployees.mapNotNull { pe ->
            val employee = pe.employee ?: return@mapNotNull null
            val items = (schedulesByEmployeeId[employee.id] ?: emptyList())
                .sortedBy { it.workingDate ?: LocalDate.MIN }
                .mapNotNull { schedule ->
                    val account = schedule.account ?: return@mapNotNull null
                    val workingDate = schedule.workingDate ?: return@mapNotNull null
                    PromotionScheduleItem(
                        scheduleId = schedule.id,
                        workingDate = workingDate,
                        accountId = account.id,
                        accountCode = account.externalKey,
                        accountName = account.name ?: "",
                        workingCategory1 = schedule.workingCategory1?.displayName,
                        workingCategory3 = schedule.workingCategory3?.displayName,
                        workingCategory4 = schedule.workingCategory4
                    )
                }
            PromotionScheduleMember(
                promotionEmployeeId = pe.id,
                employeeId = employee.id,
                employeeNumber = employee.employeeCode,
                employeeName = employee.name,
                schedules = items
            )
        }

        val totalScheduleCount = members.sumOf { it.schedules.size }

        return PromotionScheduleListResponse(
            promotionId = promotion.id,
            schedulePeriod = SchedulePeriod(startDate = effectiveStart, endDate = effectiveEnd),
            members = members,
            totalMemberCount = members.size,
            totalScheduleCount = totalScheduleCount
        )
    }

    @Transactional
    fun bulkUpdate(
        promotionId: Long,
        request: PromotionScheduleBulkUpdateRequest
    ): PromotionScheduleBulkUpdateResponse {
        val promotion = findActivePromotion(promotionId)

        val items = request.items
        if (items.isEmpty() || items.size > BULK_MAX_SIZE) {
            throw PromotionScheduleBulkInvalidSizeException()
        }

        items.forEach { validateCategory(it) }

        val scheduleIds = items.map { it.scheduleId!! }
        val schedules = teamMemberScheduleRepository.findAllById(scheduleIds)
        val schedulesById = schedules.associateBy { it.id }

        val missing = scheduleIds.filter { it !in schedulesById }
        if (missing.isNotEmpty()) {
            throw TeamScheduleNotFoundException()
        }

        val targetSchedules = items.map { schedulesById[it.scheduleId]!! }

        targetSchedules.forEach { schedule ->
            val pe = schedule.promotionEmployee
                ?: throw PromotionScheduleNotInPromotionException()
            if (pe.promotionId != promotionId) {
                throw PromotionScheduleNotInPromotionException()
            }
        }

        val pairsInRequest = items.map { item ->
            val schedule = schedulesById[item.scheduleId]!!
            val employeeId = schedule.employee?.id
                ?: throw TeamScheduleNotFoundException()
            employeeId to item.workingDate!!
        }
        if (pairsInRequest.toSet().size != pairsInRequest.size) {
            throw PromotionScheduleBulkDuplicateException()
        }

        items.forEach { item ->
            val workingDate = item.workingDate!!
            if (workingDate.isBefore(promotion.startDate) || workingDate.isAfter(promotion.endDate)) {
                throw PromotionScheduleWorkingDateOutOfPromotionException(workingDate)
            }
        }

        val accountIds = items.map { it.accountId!! }.distinct()
        val accounts = accountRepository.findAllById(accountIds).associateBy { it.id }
        if (accounts.size != accountIds.size) {
            throw TeamScheduleAccountNotFoundException()
        }

        val excludeIds = scheduleIds.toSet()
        items.forEach { item ->
            val schedule = schedulesById[item.scheduleId]!!
            val employeeId = schedule.employee?.id ?: throw TeamScheduleNotFoundException()
            teamScheduleValidator.validateScheduleConflict(
                employeeId = employeeId,
                workingDate = item.workingDate!!,
                workingCategory3 = WorkingCategory3.fromDisplayNameOrNull(item.workingCategory3),
                excludeIds = excludeIds
            )
        }

        items.forEach { item ->
            val schedule = schedulesById[item.scheduleId]!!
            val account = accounts[item.accountId]!!
            schedule.account = account
            schedule.workingDate = item.workingDate
            schedule.workingCategory1 = WorkingCategory1.fromDisplayNameOrNull(item.workingCategory1)
            schedule.workingCategory3 = WorkingCategory3.fromDisplayNameOrNull(item.workingCategory3)
            schedule.workingCategory4 = item.workingCategory4
        }

        teamMemberScheduleRepository.saveAll(targetSchedules)

        return PromotionScheduleBulkUpdateResponse(
            updatedCount = items.size,
            scheduleIds = scheduleIds
        )
    }

    @Transactional
    fun bulkDelete(
        promotionId: Long,
        request: PromotionScheduleBulkDeleteRequest
    ): PromotionScheduleBulkDeleteResponse {
        findActivePromotion(promotionId)

        val ids = request.scheduleIds.distinct()
        if (ids.isEmpty() || ids.size > BULK_MAX_SIZE) {
            throw PromotionScheduleBulkDeleteInvalidSizeException()
        }

        val schedules = teamMemberScheduleRepository.findAllById(ids)
        val foundIds = schedules.map { it.id }.toSet()
        val missing = ids.filter { it !in foundIds }
        if (missing.isNotEmpty()) {
            throw PromotionScheduleNotFoundPartialException(missing)
        }

        schedules.forEach { schedule ->
            val pe = schedule.promotionEmployee
                ?: throw PromotionScheduleNotInPromotionException()
            if (pe.promotionId != promotionId) {
                throw PromotionScheduleNotInPromotionException()
            }
        }

        teamMemberScheduleRepository.deleteAll(schedules)

        return PromotionScheduleBulkDeleteResponse(deletedCount = schedules.size)
    }

    // --- Private helpers ---

    private fun findActivePromotion(promotionId: Long): Promotion {
        val promotion = promotionRepository.findById(promotionId)
            .orElseThrow { PromotionNotFoundException() }
        if (promotion.isDeleted) throw PromotionNotFoundException()
        return promotion
    }

    private fun validateCategory(item: PromotionScheduleBulkUpdateItem) {
        val cat1 = item.workingCategory1
        if (cat1 != null && cat1 !in VALID_WORKING_CATEGORY1) {
            throw PromotionScheduleInvalidWorkingCategoryException(cat1)
        }
        val cat3 = item.workingCategory3
        if (cat3 != null && cat3 !in VALID_WORKING_CATEGORY3) {
            throw PromotionScheduleInvalidWorkingCategoryException(cat3)
        }
    }
}
