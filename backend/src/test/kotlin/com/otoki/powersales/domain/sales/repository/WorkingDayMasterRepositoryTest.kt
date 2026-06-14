package com.otoki.powersales.domain.sales.repository

import com.otoki.powersales.platform.common.config.QueryDslConfig
import com.otoki.powersales.domain.sales.entity.WorkingDayMaster
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles
import java.time.LocalDate

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@ActiveProfiles("test")
@Import(QueryDslConfig::class)
@DisplayName("WorkingDayMasterRepository 테스트")
class WorkingDayMasterRepositoryTest {

    @Autowired
    private lateinit var workingDayMasterRepository: WorkingDayMasterRepository

    @Autowired
    private lateinit var testEntityManager: TestEntityManager

    @BeforeEach
    fun setUp() {
        workingDayMasterRepository.deleteAll()
        testEntityManager.clear()
    }

    private fun persist(date: String, check: Double?, isDeleted: Boolean? = false): WorkingDayMaster =
        testEntityManager.persist(
            WorkingDayMaster(
                workingDate = LocalDate.parse(date),
                workingDateCheck = check,
                isDeleted = isDeleted,
            ),
        )

    @Test
    @DisplayName("findByWorkingDateRange - 구간 경계(start/end)를 포함하고, 구간 밖 일자는 제외한다")
    fun includesBoundariesExcludesOutside() {
        persist("2026-05-31", 1.0) // 구간 직전 — 제외
        persist("2026-06-01", 1.0) // start 경계 — 포함
        persist("2026-06-15", 1.0)
        persist("2026-06-30", 1.0) // end 경계 — 포함
        persist("2026-07-01", 1.0) // 구간 직후 — 제외
        testEntityManager.flush()
        testEntityManager.clear()

        val result = workingDayMasterRepository.findByWorkingDateRange(
            LocalDate.of(2026, 6, 1),
            LocalDate.of(2026, 6, 30),
        )

        assertThat(result.map { it.workingDate }).containsExactly(
            LocalDate.of(2026, 6, 1),
            LocalDate.of(2026, 6, 15),
            LocalDate.of(2026, 6, 30),
        )
    }

    @Test
    @DisplayName("findByWorkingDateRange - 일자 오름차순으로 정렬된다")
    fun ordersByWorkingDateAsc() {
        persist("2026-06-20", 1.0)
        persist("2026-06-05", 0.0)
        persist("2026-06-12", 1.0)
        testEntityManager.flush()
        testEntityManager.clear()

        val result = workingDayMasterRepository.findByWorkingDateRange(
            LocalDate.of(2026, 6, 1),
            LocalDate.of(2026, 6, 30),
        )

        assertThat(result.map { it.workingDate }).containsExactly(
            LocalDate.of(2026, 6, 5),
            LocalDate.of(2026, 6, 12),
            LocalDate.of(2026, 6, 20),
        )
    }

    @Test
    @DisplayName("findByWorkingDateRange - isDeleted=true 는 제외하고 false/null 은 포함한다")
    fun excludesSoftDeletedIncludesNull() {
        persist("2026-06-01", 1.0, isDeleted = false)
        persist("2026-06-02", 1.0, isDeleted = null)
        persist("2026-06-03", 1.0, isDeleted = true) // soft-delete — 제외
        testEntityManager.flush()
        testEntityManager.clear()

        val result = workingDayMasterRepository.findByWorkingDateRange(
            LocalDate.of(2026, 6, 1),
            LocalDate.of(2026, 6, 30),
        )

        assertThat(result.map { it.workingDate }).containsExactly(
            LocalDate.of(2026, 6, 1),
            LocalDate.of(2026, 6, 2),
        )
    }

    @Test
    @DisplayName("countWorkingDays - workingDateCheck=1 이고 soft-delete 아닌 row 만 카운트한다")
    fun countsOnlyWorkingDays() {
        persist("2026-06-01", 1.0)
        persist("2026-06-02", 1.0)
        persist("2026-06-06", 0.0) // 휴일 — 제외
        persist("2026-06-08", 1.0, isDeleted = true) // soft-delete — 제외
        testEntityManager.flush()
        testEntityManager.clear()

        val count = workingDayMasterRepository.countWorkingDays(
            LocalDate.of(2026, 6, 1),
            LocalDate.of(2026, 6, 30),
            1.0,
        )

        assertThat(count).isEqualTo(2)
    }
}
