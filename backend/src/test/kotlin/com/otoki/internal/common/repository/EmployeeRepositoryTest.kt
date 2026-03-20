package com.otoki.internal.common.repository

import com.otoki.internal.sap.entity.Employee
import com.otoki.internal.sap.entity.UserRole
import com.otoki.internal.sap.repository.EmployeeRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles
import com.otoki.internal.common.config.QueryDslConfig

/**
 * UserRepository 테스트
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@ActiveProfiles("test")
@Import(QueryDslConfig::class)
class EmployeeRepositoryTest {

    @Autowired
    private lateinit var employeeRepository: EmployeeRepository

    @Autowired
    private lateinit var testEntityManager: TestEntityManager

    @BeforeEach
    fun setUp() {
        // 각 테스트 전에 데이터 초기화
        employeeRepository.deleteAll()
        testEntityManager.clear()
    }

    @Test
    @DisplayName("findByEmployeeNumber - 존재하는 사번으로 사용자를 조회하면 User를 반환한다")
    fun findByEmployeeNumber_WithExistingEmployeeNumber_ReturnsUser() {
        // Given
        val testEmployee = createTestEmployee(
            employeeNumber = "20010585",
            name = "홍길동",
            orgName = "부산1지점"
        )
        testEntityManager.persistAndFlush(testEmployee)
        testEntityManager.clear()

        // When
        val result = employeeRepository.findByEmployeeNumber("20010585")

        // Then
        assertThat(result).isPresent
        assertThat(result.get().employeeNumber).isEqualTo("20010585")
        assertThat(result.get().name).isEqualTo("홍길동")
        assertThat(result.get().orgName).isEqualTo("부산1지점")
    }

    @Test
    @DisplayName("findByEmployeeNumber - 존재하지 않는 사번으로 조회하면 Optional.empty()를 반환한다")
    fun findByEmployeeNumber_WithNonExistingEmployeeNumber_ReturnsEmpty() {
        // Given
        val testEmployee = createTestEmployee(employeeNumber = "20010585")
        testEntityManager.persistAndFlush(testEmployee)
        testEntityManager.clear()

        // When
        val result = employeeRepository.findByEmployeeNumber("99999999")

        // Then
        assertThat(result).isEmpty
    }

    @Test
    @DisplayName("existsByEmployeeNumber - 존재하는 사번으로 확인하면 true를 반환한다")
    fun existsByEmployeeNumber_WithExistingEmployeeNumber_ReturnsTrue() {
        // Given
        val testEmployee = createTestEmployee(employeeNumber = "20010585")
        testEntityManager.persistAndFlush(testEmployee)
        testEntityManager.clear()

        // When
        val result = employeeRepository.existsByEmployeeNumber("20010585")

        // Then
        assertThat(result).isTrue()
    }

    @Test
    @DisplayName("existsByEmployeeNumber - 존재하지 않는 사번으로 확인하면 false를 반환한다")
    fun existsByEmployeeNumber_WithNonExistingEmployeeNumber_ReturnsFalse() {
        // Given
        val testEmployee = createTestEmployee(employeeNumber = "20010585")
        testEntityManager.persistAndFlush(testEmployee)
        testEntityManager.clear()

        // When
        val result = employeeRepository.existsByEmployeeNumber("99999999")

        // Then
        assertThat(result).isFalse()
    }

    @Test
    @DisplayName("findByOrgName - 해당 조직에 사용자가 있으면 사용자 목록을 반환한다")
    fun findByOrgName_WithExistingOrg_ReturnsUserList() {
        // Given
        val employee1 = createTestEmployee(
            employeeNumber = "20010585",
            name = "홍길동",
            orgName = "부산1지점"
        )
        val employee2 = createTestEmployee(
            employeeNumber = "20010586",
            name = "김영희",
            orgName = "부산1지점"
        )
        val employee3 = createTestEmployee(
            employeeNumber = "20010587",
            name = "이철수",
            orgName = "서울1지점"
        )
        testEntityManager.persistAndFlush(employee1)
        testEntityManager.persistAndFlush(employee2)
        testEntityManager.persistAndFlush(employee3)
        testEntityManager.clear()

        // When
        val result = employeeRepository.findByOrgName("부산1지점")

        // Then
        assertThat(result).hasSize(2)
        assertThat(result.map { it.employeeNumber }).containsExactlyInAnyOrder("20010585", "20010586")
        assertThat(result.map { it.name }).containsExactlyInAnyOrder("홍길동", "김영희")
        assertThat(result.all { it.orgName == "부산1지점" }).isTrue()
    }

    @Test
    @DisplayName("findByOrgName - 해당 조직에 사용자가 없으면 빈 목록을 반환한다")
    fun findByOrgName_WithNonExistingOrg_ReturnsEmptyList() {
        // Given
        val employee1 = createTestEmployee(
            employeeNumber = "20010585",
            orgName = "부산1지점"
        )
        testEntityManager.persistAndFlush(employee1)
        testEntityManager.clear()

        // When
        val result = employeeRepository.findByOrgName("대구1지점")

        // Then
        assertThat(result).isEmpty()
    }

    @Test
    @DisplayName("findByOrgName - 다양한 역할의 사용자가 같은 조직에 있으면 모두 조회된다")
    fun findByOrgName_WithVariousRoles_ReturnsAllUsers() {
        // Given
        val emp = createTestEmployee(
            employeeNumber = "20010585",
            name = "일반사원",
            orgName = "서울1지점",
            appAuthority = null
        )
        val leader = createTestEmployee(
            employeeNumber = "20010586",
            name = "팀장",
            orgName = "서울1지점",
            appAuthority = "조장"
        )
        val admin = createTestEmployee(
            employeeNumber = "20010587",
            name = "관리자",
            orgName = "서울1지점",
            appAuthority = "지점장"
        )
        testEntityManager.persistAndFlush(emp)
        testEntityManager.persistAndFlush(leader)
        testEntityManager.persistAndFlush(admin)
        testEntityManager.clear()

        // When
        val result = employeeRepository.findByOrgName("서울1지점")

        // Then
        assertThat(result).hasSize(3)
        assertThat(result.map { it.role }).containsExactlyInAnyOrder(
            UserRole.USER,
            UserRole.LEADER,
            UserRole.ADMIN
        )
    }

    /**
     * 테스트용 User 생성 헬퍼 함수
     */
    private fun createTestEmployee(
        employeeNumber: String = "20010585",
        name: String = "홍길동",
        orgName: String = "부산1지점",
        appAuthority: String? = null
    ): Employee {
        return Employee(
            employeeNumber = employeeNumber,
            password = "encodedPassword",
            name = name,
            orgName = orgName,
            appAuthority = appAuthority
        )
    }
}
