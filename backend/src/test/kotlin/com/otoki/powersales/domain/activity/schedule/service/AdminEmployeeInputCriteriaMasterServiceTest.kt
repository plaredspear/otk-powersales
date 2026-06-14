package com.otoki.powersales.domain.activity.schedule.service

import com.otoki.powersales.domain.activity.schedule.service.AdminEmployeeInputCriteriaMasterService
import com.otoki.powersales.domain.foundation.account.entity.AccountCategoryMaster
import com.otoki.powersales.domain.foundation.account.repository.AccountCategoryMasterRepository
import com.otoki.powersales.domain.activity.schedule.dto.request.EmployeeInputCriteriaMasterCreateRequest
import com.otoki.powersales.domain.activity.schedule.dto.request.EmployeeInputCriteriaMasterUpdateRequest
import com.otoki.powersales.domain.activity.schedule.entity.EmployeeInputCriteriaMaster
import com.otoki.powersales.domain.activity.schedule.enums.TypeOfWork1
import com.otoki.powersales.domain.activity.schedule.exception.EmployeeInputCriteriaCategoryNotFoundException
import com.otoki.powersales.domain.activity.schedule.exception.EmployeeInputCriteriaDateRangeInvalidException
import com.otoki.powersales.domain.activity.schedule.exception.EmployeeInputCriteriaMasterNotFoundException
import com.otoki.powersales.domain.activity.schedule.exception.EmployeeInputCriteriaPeriodOverlapException
import com.otoki.powersales.domain.activity.schedule.repository.EmployeeInputCriteriaMasterRepository
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.util.Optional

@DisplayName("AdminEmployeeInputCriteriaMasterService 테스트")
class AdminEmployeeInputCriteriaMasterServiceTest {

    private val repository: EmployeeInputCriteriaMasterRepository = mockk()
    private val categoryRepository: AccountCategoryMasterRepository = mockk()
    private val service = AdminEmployeeInputCriteriaMasterService(repository, categoryRepository)

    @Nested
    @DisplayName("create - 신규 등록")
    inner class CreateTests {

        @Test
        @DisplayName("정상 등록 - 시작일/종료일 월 단위 자동 보정")
        fun create_success_normalizeDates() {
            val category = newCategory(id = 10L)
            every { categoryRepository.findById(10L) } returns Optional.of(category)
            every {
                repository.existsOverlapping(eq(10L), eq(TypeOfWork1.DISPLAY), any(), any(), eq(-1L))
            } returns false
            every { repository.save(any<EmployeeInputCriteriaMaster>()) } answers { firstArg<EmployeeInputCriteriaMaster>() }

            val request = EmployeeInputCriteriaMasterCreateRequest(
                categoryId = 10L,
                typeOfWork1 = TypeOfWork1.DISPLAY,
                startDate = LocalDate.of(2026, 5, 15),
                endDate = LocalDate.of(2026, 8, 10),
                boundary = BigDecimal("30"),
                fixed1PersonStandardAmount = BigDecimal("1000"),
                bifurcationHalfPersonStandard = BigDecimal("500"),
            )

            val result = service.create(request)

            assertThat(result.startDate).isEqualTo(LocalDate.of(2026, 5, 1))
            assertThat(result.endDate).isEqualTo(LocalDate.of(2026, 8, 31))
            assertThat(result.confirmed).isFalse()
        }

        @Test
        @DisplayName("종료일 < 시작일 - 차단")
        fun create_endBeforeStart_throws() {
            val category = newCategory(id = 10L)
            every { categoryRepository.findById(10L) } returns Optional.of(category)

            val request = EmployeeInputCriteriaMasterCreateRequest(
                categoryId = 10L,
                typeOfWork1 = TypeOfWork1.DISPLAY,
                startDate = LocalDate.of(2026, 8, 15),
                endDate = LocalDate.of(2026, 5, 10),
                boundary = BigDecimal("30"),
                fixed1PersonStandardAmount = BigDecimal("1000"),
                bifurcationHalfPersonStandard = BigDecimal("500"),
            )

            assertThatThrownBy { service.create(request) }
                .isInstanceOf(EmployeeInputCriteriaDateRangeInvalidException::class.java)
        }

        @Test
        @DisplayName("기간 중복 - 차단")
        fun create_overlap_throws() {
            val category = newCategory(id = 10L)
            every { categoryRepository.findById(10L) } returns Optional.of(category)
            every {
                repository.existsOverlapping(eq(10L), eq(TypeOfWork1.DISPLAY), any(), any(), eq(-1L))
            } returns true

            val request = EmployeeInputCriteriaMasterCreateRequest(
                categoryId = 10L,
                typeOfWork1 = TypeOfWork1.DISPLAY,
                startDate = LocalDate.of(2026, 5, 1),
                endDate = LocalDate.of(2026, 8, 31),
                boundary = BigDecimal("30"),
                fixed1PersonStandardAmount = BigDecimal("1000"),
                bifurcationHalfPersonStandard = BigDecimal("500"),
            )

            assertThatThrownBy { service.create(request) }
                .isInstanceOf(EmployeeInputCriteriaPeriodOverlapException::class.java)
        }

        @Test
        @DisplayName("카테고리 없음 - 차단")
        fun create_categoryMissing_throws() {
            every { categoryRepository.findById(99L) } returns Optional.empty()

            val request = EmployeeInputCriteriaMasterCreateRequest(
                categoryId = 99L,
                typeOfWork1 = TypeOfWork1.DISPLAY,
                startDate = LocalDate.of(2026, 5, 1),
                endDate = null,
                boundary = BigDecimal("30"),
                fixed1PersonStandardAmount = BigDecimal("1000"),
                bifurcationHalfPersonStandard = BigDecimal("500"),
            )

            assertThatThrownBy { service.create(request) }
                .isInstanceOf(EmployeeInputCriteriaCategoryNotFoundException::class.java)
        }
    }

    @Nested
    @DisplayName("update - 수정")
    inner class UpdateTests {

        @Test
        @DisplayName("정상 수정 - 종료일 변경")
        fun update_success() {
            val category = newCategory(id = 10L)
            val existing = newEntity(id = 1L, category = category)
            every { repository.findById(1L) } returns Optional.of(existing)
            every { categoryRepository.findById(10L) } returns Optional.of(category)
            every {
                repository.existsOverlapping(eq(10L), eq(TypeOfWork1.DISPLAY), any(), any(), eq(1L))
            } returns false

            val request = EmployeeInputCriteriaMasterUpdateRequest(
                categoryId = 10L,
                typeOfWork1 = TypeOfWork1.DISPLAY,
                startDate = LocalDate.of(2026, 5, 1),
                endDate = LocalDate.of(2026, 9, 30),
                boundary = BigDecimal("30"),
                fixed1PersonStandardAmount = BigDecimal("1500"),
                bifurcationHalfPersonStandard = BigDecimal("750"),
            )

            val result = service.update(1L, request)

            assertThat(result.endDate).isEqualTo(LocalDate.of(2026, 9, 30))
            assertThat(result.fixed1PersonStandardAmount).isEqualByComparingTo(BigDecimal("1500"))
        }

        @Test
        @DisplayName("존재하지 않는 id - 차단")
        fun update_notFound() {
            every { repository.findById(99L) } returns Optional.empty()

            val request = EmployeeInputCriteriaMasterUpdateRequest(
                categoryId = 10L,
                typeOfWork1 = TypeOfWork1.DISPLAY,
                startDate = LocalDate.of(2026, 5, 1),
                endDate = null,
                boundary = BigDecimal("30"),
                fixed1PersonStandardAmount = BigDecimal("1000"),
                bifurcationHalfPersonStandard = BigDecimal("500"),
            )

            assertThatThrownBy { service.update(99L, request) }
                .isInstanceOf(EmployeeInputCriteriaMasterNotFoundException::class.java)
        }
    }

    @Nested
    @DisplayName("confirm / bulkConfirm")
    inner class ConfirmTests {

        @Test
        @DisplayName("단건 확정")
        fun confirm_success() {
            val entity = newEntity(id = 1L, confirmed = false)
            every { repository.findById(1L) } returns Optional.of(entity)

            val result = service.confirm(1L)

            assertThat(result.confirmed).isTrue()
            assertThat(entity.confirmed).isTrue()
        }

        @Test
        @DisplayName("일괄 확정 - 빈 리스트 → 빈 결과")
        fun bulkConfirm_empty() {
            val result = service.bulkConfirm(emptyList())
            assertThat(result).isEmpty()
        }

        @Test
        @DisplayName("일괄 확정 - 다건")
        fun bulkConfirm_multiple() {
            val e1 = newEntity(id = 1L, confirmed = false)
            val e2 = newEntity(id = 2L, confirmed = false)
            every { repository.findAllById(listOf(1L, 2L)) } returns listOf(e1, e2)

            val result = service.bulkConfirm(listOf(1L, 2L))

            assertThat(result).hasSize(2)
            assertThat(e1.confirmed).isTrue()
            assertThat(e2.confirmed).isTrue()
        }
    }

    @Nested
    @DisplayName("list - status 필터")
    inner class ListTests {

        @Test
        @DisplayName("VALID 필터 - 오늘 사이 기간만 반환")
        fun list_validFilter() {
            val today = LocalDate.of(2026, 5, 18)
            val cat = newCategory(id = 10L)
            val valid = newEntity(id = 1L, category = cat, startDate = LocalDate.of(2026, 5, 1), endDate = LocalDate.of(2026, 12, 31))
            val planned = newEntity(id = 2L, category = cat, startDate = LocalDate.of(2026, 8, 1), endDate = LocalDate.of(2026, 12, 31))
            val ended = newEntity(id = 3L, category = cat, startDate = LocalDate.of(2025, 1, 1), endDate = LocalDate.of(2025, 12, 31))
            every { repository.findAllNotDeleted() } returns listOf(valid, planned, ended)

            val result = service.list(AdminEmployeeInputCriteriaMasterService.ValidStatusFilter.VALID, today)

            assertThat(result).hasSize(1)
            assertThat(result[0].id).isEqualTo(1L)
        }

        @Test
        @DisplayName("PLANNED 필터")
        fun list_plannedFilter() {
            val today = LocalDate.of(2026, 5, 18)
            val cat = newCategory(id = 10L)
            val valid = newEntity(id = 1L, category = cat, startDate = LocalDate.of(2026, 5, 1), endDate = null)
            val planned = newEntity(id = 2L, category = cat, startDate = LocalDate.of(2026, 8, 1), endDate = null)
            every { repository.findAllNotDeleted() } returns listOf(valid, planned)

            val result = service.list(AdminEmployeeInputCriteriaMasterService.ValidStatusFilter.PLANNED, today)

            assertThat(result).hasSize(1)
            assertThat(result[0].id).isEqualTo(2L)
        }
    }

    @Nested
    @DisplayName("delete")
    inner class DeleteTests {

        @Test
        @DisplayName("존재하지 않는 id - 차단")
        fun delete_notFound() {
            every { repository.findById(99L) } returns Optional.empty()

            assertThatThrownBy { service.delete(99L) }
                .isInstanceOf(EmployeeInputCriteriaMasterNotFoundException::class.java)
        }
    }

    private fun newCategory(id: Long) = AccountCategoryMaster(
        id = id,
        sfid = null,
        accountCode = "CAT-$id",
        name = "거래처유형$id",
        useSearch = true,
    )

    private fun newEntity(
        id: Long = 1L,
        category: AccountCategoryMaster = newCategory(10L),
        startDate: LocalDate = LocalDate.of(2026, 5, 1),
        endDate: LocalDate? = LocalDate.of(2026, 12, 31),
        confirmed: Boolean = false,
    ) = EmployeeInputCriteriaMaster(
        id = id,
        category = category,
        typeOfWork1 = TypeOfWork1.DISPLAY,
        startDate = startDate,
        endDate = endDate,
        boundary = BigDecimal("30"),
        fixed1PersonStandardAmount = BigDecimal("1000"),
        bifurcationHalfPersonStandard = BigDecimal("500"),
        confirmed = confirmed,
    )
}
