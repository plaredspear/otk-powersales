package com.otoki.internal.notice.repository

import com.otoki.internal.notice.entity.Notice
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager
import org.springframework.test.context.ActiveProfiles
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@ActiveProfiles("test")
class NoticeRepositoryTest {

    @Autowired
    private lateinit var noticeRepository: NoticeRepository

    @Autowired
    private lateinit var testEntityManager: TestEntityManager

    @BeforeEach
    fun setUp() {
        noticeRepository.deleteAll()
        testEntityManager.clear()
    }

    @Test
    @DisplayName("findRecentNotices - 지점공지 + 전체공지를 함께 조회한다")
    fun findRecentNotices_branchAndAll() {
        // Given
        val now = LocalDateTime.now()
        val branchNotice = Notice(
            name = "부산1지점 공지",
            category = "BRANCH",
            branch = "부산1지점",
            createdDate = now.minusHours(1)
        )
        val allNotice = Notice(
            name = "전체 공지",
            category = "ALL",
            createdDate = now.minusHours(2)
        )
        val otherBranchNotice = Notice(
            name = "서울1지점 공지",
            category = "BRANCH",
            branch = "서울1지점",
            createdDate = now.minusHours(3)
        )
        testEntityManager.persistAndFlush(branchNotice)
        testEntityManager.persistAndFlush(allNotice)
        testEntityManager.persistAndFlush(otherBranchNotice)
        testEntityManager.clear()

        val since = LocalDateTime.of(LocalDate.now().minusDays(7), LocalTime.MIN)

        // When
        val result = noticeRepository.findRecentNotices("부산1지점", since)

        // Then
        assertThat(result).hasSize(2)
        assertThat(result.map { it.name }).containsExactly("부산1지점 공지", "전체 공지")
    }

    @Test
    @DisplayName("findRecentNotices - 최신순으로 정렬되어 반환된다")
    fun findRecentNotices_orderedByCreatedDateDesc() {
        // Given
        val now = LocalDateTime.now()
        val notice1 = Notice(
            name = "가장 오래된 공지",
            category = "ALL",
            createdDate = now.minusDays(3)
        )
        val notice2 = Notice(
            name = "중간 공지",
            category = "ALL",
            createdDate = now.minusDays(1)
        )
        val notice3 = Notice(
            name = "최신 공지",
            category = "ALL",
            createdDate = now.minusHours(1)
        )
        testEntityManager.persistAndFlush(notice1)
        testEntityManager.persistAndFlush(notice2)
        testEntityManager.persistAndFlush(notice3)
        testEntityManager.clear()

        val since = LocalDateTime.of(LocalDate.now().minusDays(7), LocalTime.MIN)

        // When
        val result = noticeRepository.findRecentNotices("부산1지점", since)

        // Then
        assertThat(result).hasSize(3)
        assertThat(result[0].name).isEqualTo("최신 공지")
        assertThat(result[1].name).isEqualTo("중간 공지")
        assertThat(result[2].name).isEqualTo("가장 오래된 공지")
    }

    @Test
    @DisplayName("findRecentNotices - 최대 5개만 반환된다")
    fun findRecentNotices_maxFive() {
        // Given
        val now = LocalDateTime.now()
        for (i in 1..8) {
            val notice = Notice(
                name = "공지 $i",
                category = "ALL",
                createdDate = now.minusHours(i.toLong())
            )
            testEntityManager.persistAndFlush(notice)
        }
        testEntityManager.clear()

        val since = LocalDateTime.of(LocalDate.now().minusDays(7), LocalTime.MIN)

        // When
        val result = noticeRepository.findRecentNotices("부산1지점", since)

        // Then
        assertThat(result).hasSize(5)
    }

    @Test
    @DisplayName("findRecentNotices - 1주일 이전 공지는 조회되지 않는다")
    fun findRecentNotices_oldNoticesExcluded() {
        // Given
        val now = LocalDateTime.now()
        val oldNotice = Notice(
            name = "오래된 공지",
            category = "ALL",
            createdDate = now.minusDays(10)
        )
        val recentNotice = Notice(
            name = "최근 공지",
            category = "ALL",
            createdDate = now.minusDays(1)
        )
        testEntityManager.persistAndFlush(oldNotice)
        testEntityManager.persistAndFlush(recentNotice)
        testEntityManager.clear()

        val since = LocalDateTime.of(LocalDate.now().minusDays(7), LocalTime.MIN)

        // When
        val result = noticeRepository.findRecentNotices("부산1지점", since)

        // Then
        assertThat(result).hasSize(1)
        assertThat(result[0].name).isEqualTo("최근 공지")
    }

    @Test
    @DisplayName("findRecentNotices - 공지가 없으면 빈 목록을 반환한다")
    fun findRecentNotices_noNotices() {
        // Given
        val since = LocalDateTime.of(LocalDate.now().minusDays(7), LocalTime.MIN)

        // When
        val result = noticeRepository.findRecentNotices("부산1지점", since)

        // Then
        assertThat(result).isEmpty()
    }

    @Test
    @DisplayName("findRecentNotices - 다른 지점의 지점공지는 조회되지 않는다")
    fun findRecentNotices_otherBranchExcluded() {
        // Given
        val now = LocalDateTime.now()
        val notice = Notice(
            name = "서울1지점 전용 공지",
            category = "BRANCH",
            branch = "서울1지점",
            createdDate = now.minusHours(1)
        )
        testEntityManager.persistAndFlush(notice)
        testEntityManager.clear()

        val since = LocalDateTime.of(LocalDate.now().minusDays(7), LocalTime.MIN)

        // When
        val result = noticeRepository.findRecentNotices("부산1지점", since)

        // Then
        assertThat(result).isEmpty()
    }
}
