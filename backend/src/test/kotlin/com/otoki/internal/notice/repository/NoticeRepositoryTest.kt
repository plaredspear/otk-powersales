package com.otoki.internal.notice.repository

import com.otoki.internal.common.config.QueryDslConfig
import com.otoki.internal.notice.entity.Notice
import com.otoki.internal.notice.entity.NoticeCategory
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager
import org.springframework.context.annotation.Import
import org.springframework.data.domain.PageRequest
import org.springframework.test.context.ActiveProfiles
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@Import(QueryDslConfig::class)
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

    private fun persistNotice(
        name: String = "테스트 공지",
        category: NoticeCategory? = NoticeCategory.COMPANY,
        branch: String? = null,
        contents: String? = null,
        isDeleted: Boolean? = null,
        createdDate: LocalDateTime? = LocalDateTime.now()
    ): Notice {
        val notice = Notice(
            name = name,
            category = category,
            branch = branch,
            contents = contents,
            isDeleted = isDeleted,
            createdDate = createdDate
        )
        val persisted = testEntityManager.persistAndFlush(notice)
        testEntityManager.clear()
        return persisted
    }

    @Nested
    @DisplayName("findRecentNotices - 최근 공지 조회")
    inner class FindRecentNoticesTests {

        @Test
        @DisplayName("지점공지 + 전체공지를 함께 조회한다")
        fun branchAndAll() {
            // Given
            val now = LocalDateTime.now()
            persistNotice(name = "부산1지점 공지", category = NoticeCategory.BRANCH, branch = "부산1지점", createdDate = now.minusHours(1))
            persistNotice(name = "전체 공지", category = NoticeCategory.COMPANY, createdDate = now.minusHours(2))
            persistNotice(name = "서울1지점 공지", category = NoticeCategory.BRANCH, branch = "서울1지점", createdDate = now.minusHours(3))

            val since = LocalDateTime.of(LocalDate.now().minusDays(7), LocalTime.MIN)

            // When
            val result = noticeRepository.findRecentNotices("부산1지점", since)

            // Then
            assertThat(result).hasSize(2)
            assertThat(result.map { it.name }).containsExactly("부산1지점 공지", "전체 공지")
        }

        @Test
        @DisplayName("교육 공지도 함께 조회된다")
        fun includesEducation() {
            // Given
            val now = LocalDateTime.now()
            persistNotice(name = "교육 공지", category = NoticeCategory.EDUCATION, createdDate = now.minusHours(1))
            persistNotice(name = "전체 공지", category = NoticeCategory.COMPANY, createdDate = now.minusHours(2))

            val since = LocalDateTime.of(LocalDate.now().minusDays(7), LocalTime.MIN)

            // When
            val result = noticeRepository.findRecentNotices("부산1지점", since)

            // Then
            assertThat(result).hasSize(2)
            assertThat(result.map { it.name }).containsExactly("교육 공지", "전체 공지")
        }

        @Test
        @DisplayName("최신순으로 정렬되어 반환된다")
        fun orderedByCreatedDateDesc() {
            // Given
            val now = LocalDateTime.now()
            persistNotice(name = "가장 오래된 공지", category = NoticeCategory.COMPANY, createdDate = now.minusDays(3))
            persistNotice(name = "중간 공지", category = NoticeCategory.COMPANY, createdDate = now.minusDays(1))
            persistNotice(name = "최신 공지", category = NoticeCategory.COMPANY, createdDate = now.minusHours(1))

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
        @DisplayName("최대 5개만 반환된다")
        fun maxFive() {
            // Given
            val now = LocalDateTime.now()
            for (i in 1..8) {
                persistNotice(name = "공지 $i", category = NoticeCategory.COMPANY, createdDate = now.minusHours(i.toLong()))
            }

            val since = LocalDateTime.of(LocalDate.now().minusDays(7), LocalTime.MIN)

            // When
            val result = noticeRepository.findRecentNotices("부산1지점", since)

            // Then
            assertThat(result).hasSize(5)
        }

        @Test
        @DisplayName("1주일 이전 공지는 조회되지 않는다")
        fun oldNoticesExcluded() {
            // Given
            val now = LocalDateTime.now()
            persistNotice(name = "오래된 공지", category = NoticeCategory.COMPANY, createdDate = now.minusDays(10))
            persistNotice(name = "최근 공지", category = NoticeCategory.COMPANY, createdDate = now.minusDays(1))

            val since = LocalDateTime.of(LocalDate.now().minusDays(7), LocalTime.MIN)

            // When
            val result = noticeRepository.findRecentNotices("부산1지점", since)

            // Then
            assertThat(result).hasSize(1)
            assertThat(result[0].name).isEqualTo("최근 공지")
        }

        @Test
        @DisplayName("공지가 없으면 빈 목록을 반환한다")
        fun noNotices() {
            // Given
            val since = LocalDateTime.of(LocalDate.now().minusDays(7), LocalTime.MIN)

            // When
            val result = noticeRepository.findRecentNotices("부산1지점", since)

            // Then
            assertThat(result).isEmpty()
        }

        @Test
        @DisplayName("다른 지점의 지점공지는 조회되지 않는다")
        fun otherBranchExcluded() {
            // Given
            val now = LocalDateTime.now()
            persistNotice(name = "서울1지점 전용 공지", category = NoticeCategory.BRANCH, branch = "서울1지점", createdDate = now.minusHours(1))

            val since = LocalDateTime.of(LocalDate.now().minusDays(7), LocalTime.MIN)

            // When
            val result = noticeRepository.findRecentNotices("부산1지점", since)

            // Then
            assertThat(result).isEmpty()
        }
    }

    @Nested
    @DisplayName("findNotices - 공지 목록 조회 (QueryDSL)")
    inner class FindNoticesTests {

        private val pageable = PageRequest.of(0, 10)

        @Test
        @DisplayName("전체 조회 (category=null) - COMPANY + 해당 branch BRANCH 공지 반환, isDeleted 제외")
        fun findAll() {
            // Given
            persistNotice(name = "전체 공지", category = NoticeCategory.COMPANY)
            persistNotice(name = "서울지점 공지", category = NoticeCategory.BRANCH, branch = "서울지점")
            persistNotice(name = "부산지점 공지", category = NoticeCategory.BRANCH, branch = "부산지점")
            persistNotice(name = "삭제된 공지", category = NoticeCategory.COMPANY, isDeleted = true)

            // When
            val result = noticeRepository.findNotices(null, null, "서울지점", pageable)

            // Then
            assertThat(result.totalElements).isEqualTo(2)
            assertThat(result.content.map { it.name }).containsExactlyInAnyOrder("전체 공지", "서울지점 공지")
        }

        @Test
        @DisplayName("회사공지 필터 (category=COMPANY) - COMPANY 공지만 반환")
        fun filterCompany() {
            // Given
            persistNotice(name = "전체 공지", category = NoticeCategory.COMPANY)
            persistNotice(name = "서울지점 공지", category = NoticeCategory.BRANCH, branch = "서울지점")

            // When
            val result = noticeRepository.findNotices(NoticeCategory.COMPANY, null, "서울지점", pageable)

            // Then
            assertThat(result.totalElements).isEqualTo(1)
            assertThat(result.content[0].name).isEqualTo("전체 공지")
        }

        @Test
        @DisplayName("지점공지 필터 (category=BRANCH) - 해당 지점 BRANCH 공지만 반환")
        fun filterBranch() {
            // Given
            persistNotice(name = "전체 공지", category = NoticeCategory.COMPANY)
            persistNotice(name = "서울지점 공지", category = NoticeCategory.BRANCH, branch = "서울지점")
            persistNotice(name = "부산지점 공지", category = NoticeCategory.BRANCH, branch = "부산지점")

            // When
            val result = noticeRepository.findNotices(NoticeCategory.BRANCH, null, "서울지점", pageable)

            // Then
            assertThat(result.totalElements).isEqualTo(1)
            assertThat(result.content[0].name).isEqualTo("서울지점 공지")
        }

        @Test
        @DisplayName("교육 필터 (category=EDUCATION) - EDUCATION 공지만 반환")
        fun filterEducation() {
            // Given
            persistNotice(name = "전체 공지", category = NoticeCategory.COMPANY)
            persistNotice(name = "교육 공지", category = NoticeCategory.EDUCATION)

            // When
            val result = noticeRepository.findNotices(NoticeCategory.EDUCATION, null, "서울지점", pageable)

            // Then
            assertThat(result.totalElements).isEqualTo(1)
            assertThat(result.content[0].name).isEqualTo("교육 공지")
        }

        @Test
        @DisplayName("텍스트 검색 (제목) - name에 검색어가 포함된 공지 반환")
        fun searchByTitle() {
            // Given
            persistNotice(name = "신제품 안내 공지", category = NoticeCategory.COMPANY)
            persistNotice(name = "기타 공지", category = NoticeCategory.COMPANY)

            // When
            val result = noticeRepository.findNotices(null, "안내", "서울지점", pageable)

            // Then
            assertThat(result.totalElements).isEqualTo(1)
            assertThat(result.content[0].name).isEqualTo("신제품 안내 공지")
        }

        @Test
        @DisplayName("텍스트 검색 (본문) - contents에 검색어가 포함된 공지 반환")
        fun searchByContents() {
            // Given
            persistNotice(name = "공지1", category = NoticeCategory.COMPANY, contents = "중요한 내용입니다")
            persistNotice(name = "공지2", category = NoticeCategory.COMPANY, contents = "기타 정보")

            // When
            val result = noticeRepository.findNotices(null, "내용", "서울지점", pageable)

            // Then
            assertThat(result.totalElements).isEqualTo(1)
            assertThat(result.content[0].name).isEqualTo("공지1")
        }

        @Test
        @DisplayName("검색 결과 없음 - 빈 Page 반환")
        fun searchNoResults() {
            // Given
            persistNotice(name = "일반 공지", category = NoticeCategory.COMPANY)

            // When
            val result = noticeRepository.findNotices(null, "존재하지않는검색어", "서울지점", pageable)

            // Then
            assertThat(result.totalElements).isEqualTo(0)
            assertThat(result.content).isEmpty()
        }

        @Test
        @DisplayName("페이지네이션 - 15건 중 page=0, size=10이면 10건 반환")
        fun pagination() {
            // Given
            val now = LocalDateTime.now()
            for (i in 1..15) {
                persistNotice(name = "공지 $i", category = NoticeCategory.COMPANY, createdDate = now.minusMinutes(i.toLong()))
            }

            // When
            val result = noticeRepository.findNotices(null, null, "서울지점", PageRequest.of(0, 10))

            // Then
            assertThat(result.content).hasSize(10)
            assertThat(result.totalElements).isEqualTo(15)
            assertThat(result.totalPages).isEqualTo(2)
        }

        @Test
        @DisplayName("isDeleted=true 제외 - 삭제된 공지가 결과에서 제외됨")
        fun excludeDeleted() {
            // Given
            persistNotice(name = "정상 공지", category = NoticeCategory.COMPANY, isDeleted = false)
            persistNotice(name = "삭제된 공지", category = NoticeCategory.COMPANY, isDeleted = true)

            // When
            val result = noticeRepository.findNotices(null, null, "서울지점", pageable)

            // Then
            assertThat(result.totalElements).isEqualTo(1)
            assertThat(result.content[0].name).isEqualTo("정상 공지")
        }

        @Test
        @DisplayName("isDeleted=null 포함 - isDeleted가 null인 공지가 결과에 포함됨")
        fun includeNullDeleted() {
            // Given
            persistNotice(name = "isDeleted=null 공지", category = NoticeCategory.COMPANY, isDeleted = null)
            persistNotice(name = "isDeleted=false 공지", category = NoticeCategory.COMPANY, isDeleted = false)

            // When
            val result = noticeRepository.findNotices(null, null, "서울지점", pageable)

            // Then
            assertThat(result.totalElements).isEqualTo(2)
            assertThat(result.content.map { it.name })
                .containsExactlyInAnyOrder("isDeleted=null 공지", "isDeleted=false 공지")
        }
    }
}
