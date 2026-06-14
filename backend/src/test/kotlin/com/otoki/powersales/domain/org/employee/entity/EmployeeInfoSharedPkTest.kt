package com.otoki.powersales.domain.org.employee.entity

import com.otoki.powersales.domain.org.employee.entity.Employee
import com.otoki.powersales.domain.org.employee.entity.EmployeeInfo
import com.otoki.powersales.platform.common.config.QueryDslConfig
import jakarta.persistence.EntityManager
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles

/**
 * Employee ↔ EmployeeInfo PK 공유 1:1 (@MapsId, EmployeeInfo owning) 정합 검증.
 *
 * employee_info.employee_id = employee.employee_id 공유 PK 로,
 * Employee 영속 시 cascade 로 EmployeeInfo 가 동일 PK 로 함께 INSERT 되는지 검증한다.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@ActiveProfiles("test")
@Import(QueryDslConfig::class)
class EmployeeInfoSharedPkTest {

    @Autowired
    private lateinit var testEntityManager: TestEntityManager

    @Autowired
    private lateinit var entityManager: EntityManager

    @Test
    @DisplayName("사번 보유 Employee 저장 시 EmployeeInfo 가 동일 PK(employee_id) 로 cascade 적재된다")
    fun sharedPkCascadePersist() {
        // Given
        val employee = Employee(employeeCode = "E001", name = "홍길동").apply {
            password = "encoded_pw"
            deviceUuid = "uuid-001"
            fcmToken = "fcm-001"
        }

        // When
        val saved = testEntityManager.persistAndFlush(employee)
        testEntityManager.clear()

        // Then — employee_info.employee_id == employee.employee_id (공유 PK)
        val info = testEntityManager.find(EmployeeInfo::class.java, saved.id)
        assertThat(info).isNotNull
        assertThat(info!!.employeeId).isEqualTo(saved.id)
        assertThat(info.employeeCode).isEqualTo("E001")
        assertThat(info.password).isEqualTo("encoded_pw")
        assertThat(info.deviceUuid).isEqualTo("uuid-001")
        assertThat(info.fcmToken).isEqualTo("fcm-001")
    }

    @Test
    @DisplayName("재조회 시 delegate property 로 EmployeeInfo 값이 hydrate 된다")
    fun delegateHydrateOnReload() {
        // Given
        val employee = Employee(employeeCode = "E002", name = "김철수").apply {
            password = "pw-002"
            passwordChangeRequired = false
        }
        val saved = testEntityManager.persistAndFlush(employee)
        testEntityManager.clear()

        // When
        val reloaded = testEntityManager.find(Employee::class.java, saved.id)

        // Then — delegate getter 가 employeeInfo 를 통해 값 반환
        assertThat(reloaded).isNotNull
        assertThat(reloaded!!.password).isEqualTo("pw-002")
        assertThat(reloaded.passwordChangeRequired).isFalse()
    }

    @Test
    @DisplayName("사번 미보유(employeeCode=null) Employee 는 EmployeeInfo 를 갖지 않는다")
    fun noEmployeeInfoForNullEmployeeCode() {
        // Given
        val employee = Employee(employeeCode = null, name = "외부위탁사원")

        // When
        val saved = testEntityManager.persistAndFlush(employee)
        testEntityManager.clear()

        // Then
        val info = testEntityManager.find(EmployeeInfo::class.java, saved.id)
        assertThat(info).isNull()
        assertThat(saved.password).isEqualTo("")
    }

    @Test
    @DisplayName("fetch join 없이 Employee 조회 시 employeeInfo 는 LAZY (미초기화) 상태다")
    fun employeeInfoIsLazyWithoutFetchJoin() {
        // Given
        val employee = Employee(employeeCode = "E003", name = "이영희").apply { password = "pw-003" }
        val saved = testEntityManager.persistAndFlush(employee)
        testEntityManager.clear()

        // When — fetch join 없이 PK 조회
        val reloaded = testEntityManager.find(Employee::class.java, saved.id)
        val util = entityManager.entityManagerFactory.persistenceUnitUtil

        // Then — employeeInfo 미접근 시 LAZY 미초기화 (eager fallback 아님)
        assertThat(util.isLoaded(reloaded, "employeeInfo"))
            .`as`("inverse OneToOne employeeInfo 는 접근 전까지 미초기화여야 한다")
            .isFalse()

        // 접근하면 초기화 + 값 hydrate
        assertThat(reloaded!!.password).isEqualTo("pw-003")
        assertThat(util.isLoaded(reloaded, "employeeInfo")).isTrue()
    }
}
