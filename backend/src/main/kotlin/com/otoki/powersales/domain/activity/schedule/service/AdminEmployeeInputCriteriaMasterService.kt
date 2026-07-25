package com.otoki.powersales.domain.activity.schedule.service

import com.otoki.powersales.domain.activity.schedule.dto.request.EmployeeInputCriteriaMasterCreateRequest
import com.otoki.powersales.domain.activity.schedule.dto.request.EmployeeInputCriteriaMasterUpdateRequest
import com.otoki.powersales.domain.activity.schedule.dto.response.AccountCategoryOption
import com.otoki.powersales.domain.activity.schedule.dto.response.EmployeeInputCriteriaMasterFilterMeta
import com.otoki.powersales.domain.activity.schedule.dto.response.EmployeeInputCriteriaMasterFilterOption
import com.otoki.powersales.domain.activity.schedule.dto.response.EmployeeInputCriteriaMasterFilterType
import com.otoki.powersales.domain.activity.schedule.dto.response.EmployeeInputCriteriaMasterFormMetaResponse
import com.otoki.powersales.domain.activity.schedule.dto.response.EmployeeInputCriteriaMasterListDefaults
import com.otoki.powersales.domain.activity.schedule.dto.response.EmployeeInputCriteriaMasterListMetaResponse
import com.otoki.powersales.domain.activity.schedule.dto.response.EmployeeInputCriteriaMasterResponse
import com.otoki.powersales.domain.activity.schedule.dto.response.TypeOfWork1Option
import com.otoki.powersales.domain.activity.schedule.entity.EmployeeInputCriteriaMaster
import com.otoki.powersales.domain.activity.schedule.enums.TypeOfWork1
import com.otoki.powersales.domain.activity.schedule.exception.EmployeeInputCriteriaCategoryNotFoundException
import com.otoki.powersales.domain.activity.schedule.exception.EmployeeInputCriteriaConfirmedEditDeniedException
import com.otoki.powersales.domain.activity.schedule.exception.EmployeeInputCriteriaDateRangeInvalidException
import com.otoki.powersales.domain.activity.schedule.exception.EmployeeInputCriteriaMasterNotFoundException
import com.otoki.powersales.domain.activity.schedule.exception.EmployeeInputCriteriaPeriodOverlapException
import com.otoki.powersales.domain.activity.schedule.repository.EmployeeInputCriteriaMasterRepository
import com.otoki.powersales.domain.foundation.account.repository.AccountCategoryMasterRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

@Service
@Transactional(readOnly = true)
class AdminEmployeeInputCriteriaMasterService(
    private val repository: EmployeeInputCriteriaMasterRepository,
    private val categoryRepository: AccountCategoryMasterRepository,
) {

    enum class ValidStatusFilter(val displayName: String) {
        ALL("전체"),
        VALID("유효"),
        PLANNED("예정"),
        ENDED("종료"),
    }

    /**
     * 폼(등록/수정 모달) 렌더링용 메타.
     *
     * 구분(거래처유형마스터) 옵션 + 근무형태1 옵션을 한 번에 내려준다.
     * 기존 `/account-categories` lookup 을 흡수하며, 컨트롤러가 repository 를 직접 호출하던 구조를
     * service 경유로 정리한다.
     */
    fun getFormMeta(): EmployeeInputCriteriaMasterFormMetaResponse {
        val accountCategories = categoryRepository.findAll()
            .filter { it.isDeleted != true }
            .sortedBy { it.accountCode }
            .map {
                AccountCategoryOption(
                    value = it.id,
                    accountCode = it.accountCode ?: "",
                    name = it.name ?: "",
                )
            }

        val typeOfWork1Options = TypeOfWork1.entries
            .map { TypeOfWork1Option(value = it.displayName, name = it.displayName) }

        return EmployeeInputCriteriaMasterFormMetaResponse(
            accountCategories = accountCategories,
            typeOfWork1Options = typeOfWork1Options,
        )
    }

    /**
     * 목록 화면 조회 조건 로드.
     *
     * 상태 필터(전체/유효/예정/종료) 옵션과 기본값을 내려준다. 옵션 값은 목록 API 의
     * `status` 파라미터([ValidStatusFilter]) 와 동일한 enum 이름을 쓴다.
     */
    fun getListMeta(): EmployeeInputCriteriaMasterListMetaResponse {
        val statusOptions = ValidStatusFilter.entries
            .map { EmployeeInputCriteriaMasterFilterOption(value = it.name, label = it.displayName) }

        return EmployeeInputCriteriaMasterListMetaResponse(
            filters = listOf(
                EmployeeInputCriteriaMasterFilterMeta(
                    key = "status",
                    type = EmployeeInputCriteriaMasterFilterType.SELECT,
                    options = statusOptions,
                ),
            ),
            defaults = EmployeeInputCriteriaMasterListDefaults(
                status = ValidStatusFilter.ALL.name,
            ),
        )
    }

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

    /**
     * @param isSystemAdmin 확정 후 편집 제한의 예외 여부. 컨트롤러가 principal 로부터 산출해 주입한다
     *                      (service 가 ambient security context 에 의존하지 않도록 explicit parameter).
     */
    @Transactional
    fun update(
        id: Long,
        request: EmployeeInputCriteriaMasterUpdateRequest,
        isSystemAdmin: Boolean = false,
    ): EmployeeInputCriteriaMasterResponse {
        val entity = findEntityOrThrow(id)
        val category = categoryRepository.findById(request.categoryId)
            .orElseThrow { EmployeeInputCriteriaCategoryNotFoundException() }

        val normalizedStart = normalizeStartDate(request.startDate)
        val normalizedEnd = normalizeEndDate(request.endDate)
        validateDateRange(normalizedStart, normalizedEnd)
        validateConfirmedEditable(entity, category.id, request, normalizedStart, isSystemAdmin)
        validateNoOverlap(
            categoryId = category.id,
            typeOfWork1 = request.typeOfWork1,
            startDate = normalizedStart,
            endDate = normalizedEnd,
            excludeId = entity.id,
        )

        // sfid 는 SF 데이터 마이그레이션 보조 필드 — runtime 에서 박지 않음 (정책).
        entity.category = category
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

    /**
     * 확정 후 편집 제한 — SF ValidationRule `EditDisableForEmployeeMaster` 동등.
     *
     * 확정된 레코드는 **종료일만** 변경 가능하고 나머지 키 필드는 잠긴다. 레거시 룰의 `ISCHANGED` 목록에서
     * `EndDate__c` 만 의도적으로 제외된 것을 그대로 재현한다.
     *
     * 레거시 예외는 `$UserRole.Name = "영업지원실"` 또는 `$Profile.Name = "시스템 관리자"` 2종이었으나,
     * 신규는 시스템 관리자 단일 기준으로 운영한다(사용자 결정). 예외 대상은 확정 레코드도 전 필드 편집 가능.
     */
    private fun validateConfirmedEditable(
        entity: EmployeeInputCriteriaMaster,
        categoryId: Long,
        request: EmployeeInputCriteriaMasterUpdateRequest,
        normalizedStart: LocalDate,
        isSystemAdmin: Boolean,
    ) {
        if (!entity.confirmed || isSystemAdmin) return

        val changed = entity.category?.id != categoryId ||
            entity.typeOfWork1 != request.typeOfWork1 ||
            entity.startDate != normalizedStart ||
            isAmountChanged(entity.boundary, request.boundary) ||
            isAmountChanged(entity.fixed1PersonStandardAmount, request.fixed1PersonStandardAmount) ||
            isAmountChanged(entity.bifurcationHalfPersonStandard, request.bifurcationHalfPersonStandard)

        if (changed) throw EmployeeInputCriteriaConfirmedEditDeniedException()
    }

    /**
     * BigDecimal 변경 판정 — scale 무시 비교.
     *
     * `equals` 는 scale 까지 비교해 `30` 과 `30.0` 을 다른 값으로 보므로 [BigDecimal.compareTo] 를 쓴다.
     * entity 측은 nullable (마이그레이션 유입분에 null 존재 가능) 이고 request 측은 필수라,
     * null → 값 은 변경으로 판정한다.
     */
    private fun isAmountChanged(current: BigDecimal?, requested: BigDecimal): Boolean {
        if (current == null) return true
        return current.compareTo(requested) != 0
    }

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
