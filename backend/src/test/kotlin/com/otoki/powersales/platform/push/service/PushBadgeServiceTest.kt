package com.otoki.powersales.platform.push.service

import com.otoki.powersales.domain.org.employee.repository.EmployeeInfoRepository
import com.otoki.powersales.platform.push.dto.PushTargetEmployee
import com.otoki.powersales.platform.push.sender.PushTarget
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("PushBadgeService - 앱 아이콘 배지(미확인 푸시 건수) 관리")
class PushBadgeServiceTest {

    private val employeeInfoRepository: EmployeeInfoRepository = mockk(relaxed = true)
    private val service = PushBadgeService(employeeInfoRepository)

    @Test
    @DisplayName("대상 배지를 +1 한 뒤 사원별 절대값이 실린 발송 대상을 만든다")
    fun increaseAndBuildTargets() {
        val targets = listOf(
            PushTargetEmployee(1L, "tok-a"),
            PushTargetEmployee(2L, "tok-b"),
        )
        every { employeeInfoRepository.increasePushBadgeCount(any()) } returns 2
        every { employeeInfoRepository.findPushBadgeCounts(any()) } returns listOf(
            arrayOf<Any>(1L, 1),
            arrayOf<Any>(2L, 7),
        )

        val result = service.increaseAndBuildTargets(targets)

        // 증가 → 조회 순서로 계산된 값이 대상별로 매핑된다 (입력 순서 유지).
        verify(exactly = 1) { employeeInfoRepository.increasePushBadgeCount(listOf(1L, 2L)) }
        assertThat(result).containsExactly(
            PushTarget("tok-a", 1),
            PushTarget("tok-b", 7),
        )
    }

    @Test
    @DisplayName("배지 조회 결과가 없는 사원은 badge=null — 배지 없이 알림만 발송한다")
    fun badgeMissingFallsBackToNull() {
        every { employeeInfoRepository.increasePushBadgeCount(any()) } returns 0
        every { employeeInfoRepository.findPushBadgeCounts(any()) } returns emptyList()

        val result = service.increaseAndBuildTargets(listOf(PushTargetEmployee(9L, "tok-z")))

        assertThat(result).containsExactly(PushTarget("tok-z", null))
    }

    @Test
    @DisplayName("같은 사원이 여러 번 들어와도 카운터는 1회만 증가한다")
    fun deduplicatesEmployeeIds() {
        every { employeeInfoRepository.increasePushBadgeCount(any()) } returns 1
        every { employeeInfoRepository.findPushBadgeCounts(any()) } returns listOf(arrayOf<Any>(1L, 3))

        service.increaseAndBuildTargets(
            listOf(PushTargetEmployee(1L, "tok-a"), PushTargetEmployee(1L, "tok-a"))
        )

        verify(exactly = 1) { employeeInfoRepository.increasePushBadgeCount(listOf(1L)) }
    }

    @Test
    @DisplayName("대상이 없으면 DB 접근 없이 빈 목록")
    fun emptyTargets() {
        val result = service.increaseAndBuildTargets(emptyList())

        assertThat(result).isEmpty()
        verify(exactly = 0) { employeeInfoRepository.increasePushBadgeCount(any()) }
        verify(exactly = 0) { employeeInfoRepository.findPushBadgeCounts(any()) }
    }

    @Test
    @DisplayName("clear - 사원 배지 카운터를 0 으로 리셋한다")
    fun clear() {
        service.clear(5L)

        verify(exactly = 1) { employeeInfoRepository.clearPushBadgeCount(5L) }
    }
}
