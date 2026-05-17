package com.otoki.powersales.schedule.service

import com.otoki.powersales.account.repository.AccountCategoryMasterRepository
import com.otoki.powersales.schedule.dto.request.EmployeeInputCriteriaMasterCreateRequest
import com.otoki.powersales.schedule.dto.request.EmployeeInputCriteriaMasterUpdateRequest
import com.otoki.powersales.schedule.dto.response.EmployeeInputCriteriaMasterResponse
import com.otoki.powersales.schedule.entity.EmployeeInputCriteriaMaster
import com.otoki.powersales.schedule.enums.TypeOfWork1
import com.otoki.powersales.schedule.exception.EmployeeInputCriteriaCategoryNotFoundException
import com.otoki.powersales.schedule.exception.EmployeeInputCriteriaDateRangeInvalidException
import com.otoki.powersales.schedule.exception.EmployeeInputCriteriaMasterNotFoundException
import com.otoki.powersales.schedule.exception.EmployeeInputCriteriaPeriodOverlapException
import com.otoki.powersales.schedule.repository.EmployeeInputCriteriaMasterRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

@Service
@Transactional(readOnly = true)
class AdminEmployeeInputCriteriaMasterService(
    private val repository: EmployeeInputCriteriaMasterRepository,
    private val categoryRepository: AccountCategoryMasterRepository,
) {

    enum class ValidStatusFilter { ALL, VALID, PLANNED, ENDED }

    fun list(status: ValidStatusFilter, today: LocalDate = LocalDate.now()): List<EmployeeInputCriteriaMasterResponse> {
        val all = repository.findAllNotDeleted()
        val filtered = when (status) {
            ValidStatusFilter.ALL -> all
            ValidStatusFilter.VALID -> all.filter { isValid(it, today) }
            ValidStatusFilter.PLANNED -> all.filter { isPlanned(it, today) }
            ValidStatusFilter.ENDED -> all.filter { isEnded(it, today) }
        }
        return filtered.map { EmployeeInputCriteriaMasterResponse.from(it) }
    }

    fun get(id: Long): EmployeeInputCriteriaMasterResponse =
        EmployeeInputCriteriaMasterResponse.from(findEntityOrThrow(id))

    @Transactional
    fun create(request: EmployeeInputCriteriaMasterCreateRequest): EmployeeInputCriteriaMasterResponse {
        val category = categoryRepository.findById(request.categoryId)
            .orElseThrow { EmployeeInputCriteriaCategoryNotFoundException() }

        val normalizedStart = normalizeStartDate(request.startDate)
        val normalizedEnd = normalizeEndDate(request.endDate)
        validateDateRange(normalizedStart, normalizedEnd)
        validateNoOverlap(
            categoryId = category.id,
            typeOfWork1 = request.typeOfWork1,
            startDate = normalizedStart,
            endDate = normalizedEnd,
            excludeId = -1L,
        )

        val entity = EmployeeInputCriteriaMaster(
            category = category,
            categorySfid = category.sfid,
            typeOfWork1 = request.typeOfWork1,
            startDate = normalizedStart,
            endDate = normalizedEnd,
            boundary = request.boundary,
            fixed1PersonStandardAmount = request.fixed1PersonStandardAmount,
            bifurcationHalfPersonStandard = request.bifurcationHalfPersonStandard,
            confirmed = false,
        )
        return EmployeeInputCriteriaMasterResponse.from(repository.save(entity))
    }

    @Transactional
    fun update(id: Long, request: EmployeeInputCriteriaMasterUpdateRequest): EmployeeInputCriteriaMasterResponse {
        val entity = findEntityOrThrow(id)
        val category = categoryRepository.findById(request.categoryId)
            .orElseThrow { EmployeeInputCriteriaCategoryNotFoundException() }

        val normalizedStart = normalizeStartDate(request.startDate)
        val normalizedEnd = normalizeEndDate(request.endDate)
        validateDateRange(normalizedStart, normalizedEnd)
        validateNoOverlap(
            categoryId = category.id,
            typeOfWork1 = request.typeOfWork1,
            startDate = normalizedStart,
            endDate = normalizedEnd,
            excludeId = entity.id,
        )

        entity.category = category
        entity.categorySfid = category.sfid
        entity.typeOfWork1 = request.typeOfWork1
        entity.startDate = normalizedStart
        entity.endDate = normalizedEnd
        entity.boundary = request.boundary
        entity.fixed1PersonStandardAmount = request.fixed1PersonStandardAmount
        entity.bifurcationHalfPersonStandard = request.bifurcationHalfPersonStandard
        return EmployeeInputCriteriaMasterResponse.from(entity)
    }

    @Transactional
    fun confirm(id: Long): EmployeeInputCriteriaMasterResponse {
        val entity = findEntityOrThrow(id)
        entity.confirmed = true
        return EmployeeInputCriteriaMasterResponse.from(entity)
    }

    @Transactional
    fun bulkConfirm(ids: List<Long>): List<EmployeeInputCriteriaMasterResponse> {
        if (ids.isEmpty()) return emptyList()
        val entities = repository.findAllById(ids)
        entities.forEach { it.confirmed = true }
        return entities.map { EmployeeInputCriteriaMasterResponse.from(it) }
    }

    @Transactional
    fun delete(id: Long) {
        val entity = findEntityOrThrow(id)
        repository.delete(entity)
    }

    private fun findEntityOrThrow(id: Long): EmployeeInputCriteriaMaster =
        repository.findById(id).orElseThrow { EmployeeInputCriteriaMasterNotFoundException() }

    private fun normalizeStartDate(date: LocalDate): LocalDate = date.withDayOfMonth(1)

    private fun normalizeEndDate(date: LocalDate?): LocalDate? =
        date?.with(TemporalAdjusters.lastDayOfMonth())

    private fun validateDateRange(start: LocalDate, end: LocalDate?) {
        if (end != null && end.isBefore(start)) {
            throw EmployeeInputCriteriaDateRangeInvalidException()
        }
    }

    private fun validateNoOverlap(
        categoryId: Long,
        typeOfWork1: TypeOfWork1?,
        startDate: LocalDate,
        endDate: LocalDate?,
        excludeId: Long,
    ) {
        if (repository.existsOverlapping(categoryId, typeOfWork1, startDate, endDate, excludeId)) {
            throw EmployeeInputCriteriaPeriodOverlapException()
        }
    }

    private fun isValid(entity: EmployeeInputCriteriaMaster, today: LocalDate): Boolean {
        val start = entity.startDate ?: return false
        if (start.isAfter(today)) return false
        val end = entity.endDate ?: return true
        return !end.isBefore(today)
    }

    private fun isPlanned(entity: EmployeeInputCriteriaMaster, today: LocalDate): Boolean {
        val start = entity.startDate ?: return false
        return start.isAfter(today)
    }

    private fun isEnded(entity: EmployeeInputCriteriaMaster, today: LocalDate): Boolean {
        val end = entity.endDate ?: return false
        return end.isBefore(today)
    }
}
