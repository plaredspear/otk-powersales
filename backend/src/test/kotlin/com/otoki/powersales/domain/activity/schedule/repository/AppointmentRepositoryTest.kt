package com.otoki.powersales.domain.activity.schedule.repository

import com.otoki.powersales.domain.activity.schedule.entity.Appointment
import com.otoki.powersales.platform.common.config.QueryDslConfig
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles
import java.time.LocalDate
import java.time.LocalDateTime

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@ActiveProfiles("test")
@Import(QueryDslConfig::class)
@DisplayName("AppointmentRepository 테스트")
class AppointmentRepositoryTest {

    @Autowired
    private lateinit var appointmentRepository: AppointmentRepository

    @Autowired
    private lateinit var testEntityManager: TestEntityManager

    private val employeeCode = "20050352"

    @Test
    @DisplayName("동일 발령일 다건 - 가장 나중에 적재된 발령을 반환")
    fun sameAppointDateReturnsLatestCreated() {
        // 운영 실사례 재현: 같은 발령일(2025-05-01)로 옛 직무코드(A049) 행이 먼저 적재되고
        // 승진 반영분(A034) 이 나중에 적재된 상황. 발령일만으로 정렬하면 A049 를 집을 수 있다.
        persistAppointment(
            jobCode = "A049",
            appointDate = LocalDate.of(2025, 5, 1),
            createdAt = LocalDateTime.of(2025, 5, 2, 5, 53, 45)
        )
        persistAppointment(
            jobCode = "A034",
            appointDate = LocalDate.of(2025, 5, 1),
            createdAt = LocalDateTime.of(2025, 5, 16, 20, 30, 18)
        )

        val found = appointmentRepository
            .findFirstByEmployeeCodeOrderByAppointDateDescCreatedAtDesc(employeeCode)

        assertThat(found).isNotNull
        assertThat(found!!.jobCode).isEqualTo("A034")
    }

    @Test
    @DisplayName("발령일 상이 - 적재 순서와 무관하게 발령일이 가장 늦은 발령을 반환")
    fun latestAppointDateWins() {
        // 발령일이 1차 정렬키임을 확인 — 나중에 적재된 행이라도 발령일이 이르면 선택되지 않는다.
        persistAppointment(
            jobCode = "A034",
            appointDate = LocalDate.of(2026, 5, 1),
            createdAt = LocalDateTime.of(2026, 5, 4, 20, 30, 12)
        )
        persistAppointment(
            jobCode = "A049",
            appointDate = LocalDate.of(2025, 5, 1),
            createdAt = LocalDateTime.of(2026, 6, 1, 0, 0, 0)
        )

        val found = appointmentRepository
            .findFirstByEmployeeCodeOrderByAppointDateDescCreatedAtDesc(employeeCode)

        assertThat(found).isNotNull
        assertThat(found!!.appointDate).isEqualTo(LocalDate.of(2026, 5, 1))
        assertThat(found.jobCode).isEqualTo("A034")
    }

    @Test
    @DisplayName("타 사원 발령은 조회 대상 아님")
    fun otherEmployeeExcluded() {
        persistAppointment(
            jobCode = "A034",
            appointDate = LocalDate.of(2026, 5, 1),
            createdAt = LocalDateTime.of(2026, 5, 4, 20, 30, 12),
            code = "99999999"
        )

        val found = appointmentRepository
            .findFirstByEmployeeCodeOrderByAppointDateDescCreatedAtDesc(employeeCode)

        assertThat(found).isNull()
    }

    private fun persistAppointment(
        jobCode: String,
        appointDate: LocalDate,
        createdAt: LocalDateTime,
        code: String = employeeCode
    ): Appointment {
        val appointment = Appointment(
            employeeCode = code,
            empCodeExist = true,
            afterOrgCode = "5842",
            afterOrgName = "제주지점",
            jobCode = jobCode,
            appointDate = appointDate,
            ordDetailNode = "승진"
        )
        // 적재 시각을 명시 지정한다 — auditing 자동 부여에 기대면 연속 persist 가 동일 값을 받아
        // 정렬 검증이 무의미해진다.
        appointment.createdAt = createdAt
        appointment.updatedAt = createdAt
        return testEntityManager.persistAndFlush(appointment)
    }
}
