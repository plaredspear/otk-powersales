package com.otoki.internal.repository

import com.otoki.internal.entity.User
import com.otoki.internal.entity.UserRole
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager
import org.springframework.test.context.ActiveProfiles

/**
 * UserRepository 테스트
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@ActiveProfiles("test")
class UserRepositoryTest {

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var testEntityManager: TestEntityManager

    @BeforeEach
    fun setUp() {
        // 각 테스트 전에 데이터 초기화
        userRepository.deleteAll()
        testEntityManager.clear()
    }

    @Test
    @DisplayName("findByEmployeeId - 존재하는 사번으로 사용자를 조회하면 User를 반환한다")
    fun findByEmployeeId_WithExistingEmployeeId_ReturnsUser() {
        // Given
        val testUser = createTestUser(
            employeeId = "20010585",
            name = "홍길동",
            orgName = "부산1지점"
        )
        testEntityManager.persistAndFlush(testUser)
        testEntityManager.clear()

        // When
        val result = userRepository.findByEmployeeId("20010585")

        // Then
        assertThat(result).isPresent
        assertThat(result.get().employeeId).isEqualTo("20010585")
        assertThat(result.get().name).isEqualTo("홍길동")
        assertThat(result.get().orgName).isEqualTo("부산1지점")
    }

    @Test
    @DisplayName("findByEmployeeId - 존재하지 않는 사번으로 조회하면 Optional.empty()를 반환한다")
    fun findByEmployeeId_WithNonExistingEmployeeId_ReturnsEmpty() {
        // Given
        val testUser = createTestUser(employeeId = "20010585")
        testEntityManager.persistAndFlush(testUser)
        testEntityManager.clear()

        // When
        val result = userRepository.findByEmployeeId("99999999")

        // Then
        assertThat(result).isEmpty
    }

    @Test
    @DisplayName("existsByEmployeeId - 존재하는 사번으로 확인하면 true를 반환한다")
    fun existsByEmployeeId_WithExistingEmployeeId_ReturnsTrue() {
        // Given
        val testUser = createTestUser(employeeId = "20010585")
        testEntityManager.persistAndFlush(testUser)
        testEntityManager.clear()

        // When
        val result = userRepository.existsByEmployeeId("20010585")

        // Then
        assertThat(result).isTrue()
    }

    @Test
    @DisplayName("existsByEmployeeId - 존재하지 않는 사번으로 확인하면 false를 반환한다")
    fun existsByEmployeeId_WithNonExistingEmployeeId_ReturnsFalse() {
        // Given
        val testUser = createTestUser(employeeId = "20010585")
        testEntityManager.persistAndFlush(testUser)
        testEntityManager.clear()

        // When
        val result = userRepository.existsByEmployeeId("99999999")

        // Then
        assertThat(result).isFalse()
    }

    @Test
    @DisplayName("findByOrgName - 해당 조직에 사용자가 있으면 사용자 목록을 반환한다")
    fun findByOrgName_WithExistingOrg_ReturnsUserList() {
        // Given
        val user1 = createTestUser(
            employeeId = "20010585",
            name = "홍길동",
            orgName = "부산1지점"
        )
        val user2 = createTestUser(
            employeeId = "20010586",
            name = "김영희",
            orgName = "부산1지점"
        )
        val user3 = createTestUser(
            employeeId = "20010587",
            name = "이철수",
            orgName = "서울1지점"
        )
        testEntityManager.persistAndFlush(user1)
        testEntityManager.persistAndFlush(user2)
        testEntityManager.persistAndFlush(user3)
        testEntityManager.clear()

        // When
        val result = userRepository.findByOrgName("부산1지점")

        // Then
        assertThat(result).hasSize(2)
        assertThat(result.map { it.employeeId }).containsExactlyInAnyOrder("20010585", "20010586")
        assertThat(result.map { it.name }).containsExactlyInAnyOrder("홍길동", "김영희")
        assertThat(result.all { it.orgName == "부산1지점" }).isTrue()
    }

    @Test
    @DisplayName("findByOrgName - 해당 조직에 사용자가 없으면 빈 목록을 반환한다")
    fun findByOrgName_WithNonExistingOrg_ReturnsEmptyList() {
        // Given
        val user1 = createTestUser(
            employeeId = "20010585",
            orgName = "부산1지점"
        )
        testEntityManager.persistAndFlush(user1)
        testEntityManager.clear()

        // When
        val result = userRepository.findByOrgName("대구1지점")

        // Then
        assertThat(result).isEmpty()
    }

    @Test
    @DisplayName("findByOrgName - 다양한 역할의 사용자가 같은 조직에 있으면 모두 조회된다")
    fun findByOrgName_WithVariousRoles_ReturnsAllUsers() {
        // Given
        val user = createTestUser(
            employeeId = "20010585",
            name = "일반사원",
            orgName = "서울1지점",
            appAuthority = null
        )
        val leader = createTestUser(
            employeeId = "20010586",
            name = "팀장",
            orgName = "서울1지점",
            appAuthority = "조장"
        )
        val admin = createTestUser(
            employeeId = "20010587",
            name = "관리자",
            orgName = "서울1지점",
            appAuthority = "지점장"
        )
        testEntityManager.persistAndFlush(user)
        testEntityManager.persistAndFlush(leader)
        testEntityManager.persistAndFlush(admin)
        testEntityManager.clear()

        // When
        val result = userRepository.findByOrgName("서울1지점")

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
    private fun createTestUser(
        employeeId: String = "20010585",
        name: String = "홍길동",
        orgName: String = "부산1지점",
        appAuthority: String? = null
    ): User {
        return User(
            employeeId = employeeId,
            password = "encodedPassword",
            name = name,
            orgName = orgName,
            appAuthority = appAuthority
        )
    }
}
