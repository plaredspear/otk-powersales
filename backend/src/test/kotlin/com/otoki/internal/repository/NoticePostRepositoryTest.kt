package com.otoki.internal.repository

import com.otoki.internal.entity.*
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager
import org.springframework.data.domain.PageRequest
import org.springframework.test.context.ActiveProfiles

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@ActiveProfiles("test")
@DisplayName("NoticePostRepository 테스트")
class NoticePostRepositoryTest {

    @Autowired
    private lateinit var noticePostRepository: NoticePostRepository

    @Autowired
    private lateinit var noticePostImageRepository: NoticePostImageRepository

    @Autowired
    private lateinit var testEntityManager: TestEntityManager

    private lateinit var testUser: User

    @BeforeEach
    fun setUp() {
        noticePostRepository.deleteAll()
        testEntityManager.clear()

        // 테스트 사용자 생성
        testUser = testEntityManager.persistAndFlush(
            User(
                employeeId = "10000001",
                password = "encoded",
                name = "관리자",
                department = "인사팀",
                branchName = "본사",
                role = UserRole.ADMIN,
                workerType = WorkerType.PATROL
            )
        )
    }

    @Test
    @DisplayName("전체 활성 게시물을 최신순으로 조회할 수 있다")
    fun findByIsActiveTrueOrderByCreatedAtDesc() {
        // Given
        val post1 = noticePostRepository.save(
            NoticePost(
                category = NoticeCategory.COMPANY,
                title = "진라면 포장지 변경",
                content = "진라면 포장지 디자인이 변경됩니다.",
                createdBy = testUser,
                isActive = true
            )
        )

        Thread.sleep(10) // createdAt 차이를 위한 대기

        val post2 = noticePostRepository.save(
            NoticePost(
                category = NoticeCategory.BRANCH,
                title = "지점 공지사항",
                content = "지점 관련 내용",
                createdBy = testUser,
                isActive = true
            )
        )

        // When
        val pageable = PageRequest.of(0, 10)
        val result = noticePostRepository.findByIsActiveTrueOrderByCreatedAtDesc(pageable)

        // Then
        assertThat(result.content).hasSize(2)
        assertThat(result.totalElements).isEqualTo(2)
        assertThat(result.content[0].id).isEqualTo(post2.id) // 최신순
        assertThat(result.content[1].id).isEqualTo(post1.id)
    }

    @Test
    @DisplayName("분류별 활성 게시물을 최신순으로 조회할 수 있다")
    fun findByCategoryAndIsActiveTrueOrderByCreatedAtDesc() {
        // Given
        val post1 = noticePostRepository.save(
            NoticePost(
                category = NoticeCategory.COMPANY,
                title = "진라면 포장지 변경",
                content = "진라면 포장지 디자인이 변경됩니다.",
                createdBy = testUser,
                isActive = true
            )
        )

        Thread.sleep(10)

        val post2 = noticePostRepository.save(
            NoticePost(
                category = NoticeCategory.COMPANY,
                title = "신제품 출시",
                content = "신제품이 출시됩니다.",
                createdBy = testUser,
                isActive = true
            )
        )

        // 다른 카테고리 게시물 (조회 대상 아님)
        noticePostRepository.save(
            NoticePost(
                category = NoticeCategory.BRANCH,
                title = "지점 공지",
                content = "지점 공지 내용",
                createdBy = testUser,
                isActive = true
            )
        )

        // When
        val pageable = PageRequest.of(0, 10)
        val result = noticePostRepository.findByCategoryAndIsActiveTrueOrderByCreatedAtDesc(
            NoticeCategory.COMPANY,
            pageable
        )

        // Then
        assertThat(result.content).hasSize(2)
        assertThat(result.totalElements).isEqualTo(2)
        assertThat(result.content[0].id).isEqualTo(post2.id) // 최신순
        assertThat(result.content[1].id).isEqualTo(post1.id)
    }

    @Test
    @DisplayName("전체 검색(제목+내용)과 페이지네이션을 수행할 수 있다")
    fun findBySearchWithPaging() {
        // Given
        noticePostRepository.save(
            NoticePost(
                category = NoticeCategory.COMPANY,
                title = "진라면 포장지 변경",
                content = "진라면 포장지 디자인이 변경됩니다.",
                createdBy = testUser,
                isActive = true
            )
        )

        noticePostRepository.save(
            NoticePost(
                category = NoticeCategory.BRANCH,
                title = "매장 안내",
                content = "포장지 관리 방법을 안내합니다.",
                createdBy = testUser,
                isActive = true
            )
        )

        noticePostRepository.save(
            NoticePost(
                category = NoticeCategory.COMPANY,
                title = "신제품 공지",
                content = "신제품 출시 안내",
                createdBy = testUser,
                isActive = true
            )
        )

        // When - "포장지" 키워드로 검색 (제목 또는 내용에 포함)
        val pageable = PageRequest.of(0, 10)
        val result = noticePostRepository.findBySearchWithPaging("포장지", pageable)

        // Then
        assertThat(result.content).hasSize(2) // 제목 또는 내용에 "포장지" 포함
        assertThat(result.totalElements).isEqualTo(2)
    }

    @Test
    @DisplayName("분류별 검색(제목+내용)과 페이지네이션을 수행할 수 있다")
    fun findByCategoryAndSearchWithPaging() {
        // Given
        noticePostRepository.save(
            NoticePost(
                category = NoticeCategory.COMPANY,
                title = "진라면 포장지 변경",
                content = "진라면 포장지 디자인이 변경됩니다.",
                createdBy = testUser,
                isActive = true
            )
        )

        noticePostRepository.save(
            NoticePost(
                category = NoticeCategory.COMPANY,
                title = "신제품 출시",
                content = "새로운 라면 제품이 출시됩니다.",
                createdBy = testUser,
                isActive = true
            )
        )

        noticePostRepository.save(
            NoticePost(
                category = NoticeCategory.BRANCH,
                title = "지점 라면 재고",
                content = "지점 라면 재고 관리 방법",
                createdBy = testUser,
                isActive = true
            )
        )

        // When - COMPANY 분류 + "라면" 키워드로 검색
        val pageable = PageRequest.of(0, 10)
        val result = noticePostRepository.findByCategoryAndSearchWithPaging(
            NoticeCategory.COMPANY,
            "라면",
            pageable
        )

        // Then
        assertThat(result.content).hasSize(2) // COMPANY 분류에서 "라면" 포함
        assertThat(result.totalElements).isEqualTo(2)
    }

    @Test
    @DisplayName("ID로 활성 게시물을 조회할 수 있다")
    fun findByIdAndIsActiveTrue() {
        // Given
        val post = noticePostRepository.save(
            NoticePost(
                category = NoticeCategory.COMPANY,
                title = "진라면 포장지 변경",
                content = "진라면 포장지 디자인이 변경됩니다.",
                createdBy = testUser,
                isActive = true
            )
        )

        // When
        val found = noticePostRepository.findByIdAndIsActiveTrue(post.id)

        // Then
        assertThat(found).isNotNull
        assertThat(found!!.id).isEqualTo(post.id)
        assertThat(found.title).isEqualTo("진라면 포장지 변경")
    }

    @Test
    @DisplayName("비활성 게시물은 조회되지 않는다")
    fun findByIdAndIsActiveTrue_InactivePost() {
        // Given
        val inactivePost = noticePostRepository.save(
            NoticePost(
                category = NoticeCategory.COMPANY,
                title = "삭제된 게시물",
                content = "삭제된 내용",
                createdBy = testUser,
                isActive = false
            )
        )

        // When
        val found = noticePostRepository.findByIdAndIsActiveTrue(inactivePost.id)

        // Then
        assertThat(found).isNull()
    }

    @Test
    @DisplayName("게시물별 이미지를 정렬 순서대로 조회할 수 있다")
    fun findByPostIdOrderBySortOrderAsc() {
        // Given
        val post = noticePostRepository.save(
            NoticePost(
                category = NoticeCategory.COMPANY,
                title = "진라면 포장지 변경",
                content = "진라면 포장지 디자인이 변경됩니다.",
                createdBy = testUser,
                isActive = true
            )
        )

        val image3 = noticePostImageRepository.save(
            NoticePostImage(
                post = post,
                url = "https://storage.example.com/notices/4/img3.jpg",
                sortOrder = 3
            )
        )

        val image1 = noticePostImageRepository.save(
            NoticePostImage(
                post = post,
                url = "https://storage.example.com/notices/4/img1.jpg",
                sortOrder = 1
            )
        )

        val image2 = noticePostImageRepository.save(
            NoticePostImage(
                post = post,
                url = "https://storage.example.com/notices/4/img2.jpg",
                sortOrder = 2
            )
        )

        // When
        val images = noticePostImageRepository.findByPostIdOrderBySortOrderAsc(post.id)

        // Then
        assertThat(images).hasSize(3)
        assertThat(images[0].id).isEqualTo(image1.id) // sortOrder = 1
        assertThat(images[1].id).isEqualTo(image2.id) // sortOrder = 2
        assertThat(images[2].id).isEqualTo(image3.id) // sortOrder = 3
    }

    @Test
    @DisplayName("날짜 범위 밖의 데이터는 조회되지 않는다")
    fun findByCategoryAndIsActiveTrue_OutOfRange() {
        // Given
        noticePostRepository.save(
            NoticePost(
                category = NoticeCategory.COMPANY,
                title = "오래된 공지",
                content = "오래된 공지 내용",
                createdBy = testUser,
                isActive = false // 비활성
            )
        )

        // When
        val pageable = PageRequest.of(0, 10)
        val result = noticePostRepository.findByCategoryAndIsActiveTrueOrderByCreatedAtDesc(
            NoticeCategory.COMPANY,
            pageable
        )

        // Then - 비활성 게시물은 조회되지 않음
        assertThat(result.content).isEmpty()
    }
}
