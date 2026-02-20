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
@DisplayName("EducationPostRepository 테스트")
class EducationPostRepositoryTest {

    @Autowired
    private lateinit var educationPostRepository: EducationPostRepository

    @Autowired
    private lateinit var educationPostImageRepository: EducationPostImageRepository

    @Autowired
    private lateinit var educationPostAttachmentRepository: EducationPostAttachmentRepository

    @Autowired
    private lateinit var testEntityManager: TestEntityManager

    private lateinit var testUser: User
    private lateinit var testPost1: EducationPost
    private lateinit var testPost2: EducationPost

    @BeforeEach
    fun setUp() {
        educationPostRepository.deleteAll()
        testEntityManager.clear()

        // 테스트 사용자 생성
        testUser = testEntityManager.persistAndFlush(
            User(
                employeeId = "10000001",
                password = "encoded",
                name = "관리자",
                orgName = "본사",
                appAuthority = "지점장"
            )
        )
    }

    @Test
    @DisplayName("카테고리별 활성 게시물을 최신순으로 조회할 수 있다")
    fun findByCategoryAndIsActiveTrueOrderByCreatedAtDesc() {
        // Given
        val post1 = educationPostRepository.save(
            EducationPost(
                category = EducationCategory.TASTING_MANUAL,
                title = "진짬뽕 시식 매뉴얼",
                content = "진짬뽕 시식 방법을 안내합니다.",
                createdBy = testUser,
                isActive = true
            )
        )

        Thread.sleep(10) // createdAt 차이를 위한 대기

        val post2 = educationPostRepository.save(
            EducationPost(
                category = EducationCategory.TASTING_MANUAL,
                title = "미숫가루 시식 매뉴얼",
                content = "미숫가루 시식 방법을 안내합니다.",
                createdBy = testUser,
                isActive = true
            )
        )

        // 다른 카테고리 게시물 (조회 대상 아님)
        educationPostRepository.save(
            EducationPost(
                category = EducationCategory.CS_SAFETY,
                title = "안전 수칙",
                content = "안전 수칙 내용",
                createdBy = testUser,
                isActive = true
            )
        )

        // When
        val pageable = PageRequest.of(0, 10)
        val result = educationPostRepository.findByCategoryAndIsActiveTrueOrderByCreatedAtDesc(
            EducationCategory.TASTING_MANUAL,
            pageable
        )

        // Then
        assertThat(result.content).hasSize(2)
        assertThat(result.totalElements).isEqualTo(2)
        assertThat(result.content[0].id).isEqualTo(post2.id) // 최신순
        assertThat(result.content[1].id).isEqualTo(post1.id)
    }

    @Test
    @DisplayName("카테고리별 검색(제목+내용)과 페이지네이션을 수행할 수 있다")
    fun findByCategoryAndSearchWithPaging() {
        // Given
        educationPostRepository.save(
            EducationPost(
                category = EducationCategory.TASTING_MANUAL,
                title = "진짬뽕 시식 매뉴얼",
                content = "진짬뽕 시식 방법을 안내합니다.",
                createdBy = testUser,
                isActive = true
            )
        )

        educationPostRepository.save(
            EducationPost(
                category = EducationCategory.TASTING_MANUAL,
                title = "미숫가루 매뉴얼",
                content = "미숫가루 시식 방법을 안내합니다.",
                createdBy = testUser,
                isActive = true
            )
        )

        educationPostRepository.save(
            EducationPost(
                category = EducationCategory.TASTING_MANUAL,
                title = "라면 매뉴얼",
                content = "라면 조리 방법을 안내합니다.",
                createdBy = testUser,
                isActive = true
            )
        )

        // When - "시식" 키워드로 검색 (제목 또는 내용에 포함)
        val pageable = PageRequest.of(0, 10)
        val result = educationPostRepository.findByCategoryAndSearchWithPaging(
            EducationCategory.TASTING_MANUAL,
            "시식",
            pageable
        )

        // Then
        assertThat(result.content).hasSize(2) // "진짬뽕", "미숫가루"만 검색됨
        assertThat(result.totalElements).isEqualTo(2)
    }

    @Test
    @DisplayName("ID로 활성 게시물을 조회할 수 있다")
    fun findByIdAndIsActiveTrue() {
        // Given
        val post = educationPostRepository.save(
            EducationPost(
                category = EducationCategory.TASTING_MANUAL,
                title = "진짬뽕 시식 매뉴얼",
                content = "진짬뽕 시식 방법을 안내합니다.",
                createdBy = testUser,
                isActive = true
            )
        )

        // When
        val found = educationPostRepository.findByIdAndIsActiveTrue(post.id)

        // Then
        assertThat(found).isNotNull
        assertThat(found!!.id).isEqualTo(post.id)
        assertThat(found.title).isEqualTo("진짬뽕 시식 매뉴얼")
    }

    @Test
    @DisplayName("비활성 게시물은 조회되지 않는다")
    fun findByIdAndIsActiveTrue_InactivePost() {
        // Given
        val inactivePost = educationPostRepository.save(
            EducationPost(
                category = EducationCategory.TASTING_MANUAL,
                title = "삭제된 게시물",
                content = "삭제된 내용",
                createdBy = testUser,
                isActive = false
            )
        )

        // When
        val found = educationPostRepository.findByIdAndIsActiveTrue(inactivePost.id)

        // Then
        assertThat(found).isNull()
    }

    @Test
    @DisplayName("게시물별 이미지를 정렬 순서대로 조회할 수 있다")
    fun findByPostIdOrderBySortOrderAsc() {
        // Given
        val post = educationPostRepository.save(
            EducationPost(
                category = EducationCategory.TASTING_MANUAL,
                title = "진짬뽕 시식 매뉴얼",
                content = "진짬뽕 시식 방법을 안내합니다.",
                createdBy = testUser,
                isActive = true
            )
        )

        val image3 = educationPostImageRepository.save(
            EducationPostImage(
                post = post,
                url = "https://storage.example.com/img3.jpg",
                sortOrder = 3
            )
        )

        val image1 = educationPostImageRepository.save(
            EducationPostImage(
                post = post,
                url = "https://storage.example.com/img1.jpg",
                sortOrder = 1
            )
        )

        val image2 = educationPostImageRepository.save(
            EducationPostImage(
                post = post,
                url = "https://storage.example.com/img2.jpg",
                sortOrder = 2
            )
        )

        // When
        val images = educationPostImageRepository.findByPostIdOrderBySortOrderAsc(post.id)

        // Then
        assertThat(images).hasSize(3)
        assertThat(images[0].id).isEqualTo(image1.id) // sortOrder = 1
        assertThat(images[1].id).isEqualTo(image2.id) // sortOrder = 2
        assertThat(images[2].id).isEqualTo(image3.id) // sortOrder = 3
    }

    @Test
    @DisplayName("게시물별 첨부파일을 조회할 수 있다")
    fun findByPostId() {
        // Given
        val post = educationPostRepository.save(
            EducationPost(
                category = EducationCategory.TASTING_MANUAL,
                title = "진짬뽕 시식 매뉴얼",
                content = "진짬뽕 시식 방법을 안내합니다.",
                createdBy = testUser,
                isActive = true
            )
        )

        educationPostAttachmentRepository.save(
            EducationPostAttachment(
                post = post,
                fileName = "guide.pdf",
                fileUrl = "https://storage.example.com/guide.pdf",
                fileSize = 2048576,
                contentType = "application/pdf"
            )
        )

        educationPostAttachmentRepository.save(
            EducationPostAttachment(
                post = post,
                fileName = "manual.docx",
                fileUrl = "https://storage.example.com/manual.docx",
                fileSize = 1024000,
                contentType = "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
            )
        )

        // When
        val attachments = educationPostAttachmentRepository.findByPostId(post.id)

        // Then
        assertThat(attachments).hasSize(2)
        assertThat(attachments.map { it.fileName }).containsExactlyInAnyOrder("guide.pdf", "manual.docx")
    }
}
