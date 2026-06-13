package com.otoki.powersales.common.repository

import com.otoki.powersales.common.config.QueryDslConfig
import com.otoki.powersales.common.entity.AgreementHistory
import com.otoki.powersales.domain.support.agreement.entity.AgreementWord
import com.otoki.powersales.employee.entity.Employee
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

/**
 * AgreementHistoryRepository 테스트 (스펙 #583 G3)
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@ActiveProfiles("test")
@Import(QueryDslConfig::class)
class AgreementHistoryRepositoryTest {

    @Autowired
    private lateinit var agreementHistoryRepository: AgreementHistoryRepository

    @Autowired
    private lateinit var testEntityManager: TestEntityManager

    private var agreementWordId: Long = 0
    private var employeeIdA: Long = 0
    private var employeeIdB: Long = 0

    @BeforeEach
    fun setUp() {
        agreementHistoryRepository.deleteAll()
        testEntityManager.clear()
        // FK 제약 만족용 AgreementWord + Employee 사전 생성
        val word = testEntityManager.persistAndFlush(
            AgreementWord(name = "AGR-TEST", contents = "테스트", active = true, isDeleted = false)
        )
        agreementWordId = word.id.toLong()
        employeeIdA = testEntityManager.persistAndFlush(
            Employee(employeeCode = "T0000001", password = "x", name = "직원A", orgName = "테스트지점")
        ).id
        employeeIdB = testEntityManager.persistAndFlush(
            Employee(employeeCode = "T0000002", password = "x", name = "직원B", orgName = "테스트지점")
        ).id
        testEntityManager.clear()
    }

    @Test
    @DisplayName("T3 - 동일 사번에 다건 존재 시 agreement_date desc, id desc 순으로 1건 반환")
    fun findFirstByEmployeeId_returnsLatestByDateThenId() {
        // Given
        persistHistory(employeeIdA, LocalDate.of(2025, 1, 1)) // 가장 오래된 일자
        val recentDateOlderId = persistHistory(employeeIdA, LocalDate.of(2025, 2, 1)) // 같은 일자 중 id 작음
        val recentDateNewerId = persistHistory(employeeIdA, LocalDate.of(2025, 2, 1)) // 같은 일자 중 id 큼 → 기대값
        testEntityManager.clear()

        // When
        val result = agreementHistoryRepository
            .findFirstByEmployeeIdAndIsDeletedFalseOrderByAgreementDateDescIdDesc(employeeIdA)

        // Then
        assertThat(result).isNotNull
        assertThat(result!!.id).isEqualTo(recentDateNewerId)
        assertThat(result.id).isGreaterThan(recentDateOlderId)
    }

    @Test
    @DisplayName("T3-2 - 사번에 동의 이력이 없으면 null 반환")
    fun findFirstByEmployeeId_returnsNullWhenNoRows() {
        // When
        val result = agreementHistoryRepository
            .findFirstByEmployeeIdAndIsDeletedFalseOrderByAgreementDateDescIdDesc(employeeIdB)

        // Then
        assertThat(result).isNull()
    }

    @Test
    @DisplayName("T3-3 - is_deleted=true row 는 제외")
    fun findFirstByEmployeeId_excludesDeleted() {
        // Given
        val activeRowId = persistHistory(employeeIdB, LocalDate.of(2025, 1, 1), isDeleted = false)
        persistHistory(employeeIdB, LocalDate.of(2025, 3, 1), isDeleted = true) // 더 최신 but 삭제됨
        testEntityManager.clear()

        // When
        val result = agreementHistoryRepository
            .findFirstByEmployeeIdAndIsDeletedFalseOrderByAgreementDateDescIdDesc(employeeIdB)

        // Then
        assertThat(result).isNotNull
        assertThat(result!!.id).isEqualTo(activeRowId)
    }

    private fun persistHistory(
        employeeId: Long,
        agreementDate: LocalDate,
        isDeleted: Boolean = false,
    ): Long {
        val entity = AgreementHistory(
            employeeId = employeeId,
            agreementFlag = true,
            agreementDate = agreementDate,
            agreementWordId = agreementWordId,
            isDeleted = isDeleted,
        )
        return testEntityManager.persistAndFlush(entity).id
    }
}
