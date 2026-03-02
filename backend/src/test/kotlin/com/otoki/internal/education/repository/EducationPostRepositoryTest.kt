package com.otoki.internal.education.repository

import com.otoki.internal.education.entity.*
import com.otoki.internal.common.entity.*
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
import com.otoki.internal.common.config.QueryDslConfig
import java.time.LocalDateTime

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@ActiveProfiles("test")
@Import(QueryDslConfig::class)
@DisplayName("EducationPostRepository 테스트")
class EducationPostRepositoryTest {

    @Autowired
    private lateinit var educationPostRepository: EducationPostRepository

    @Autowired
    private lateinit var educationPostAttachmentRepository: EducationPostAttachmentRepository

    @Autowired
    private lateinit var testEntityManager: TestEntityManager

    @BeforeEach
    fun setUp() {
        educationPostRepository.deleteAll()
        testEntityManager.clear()
    }

    @Nested
    @DisplayName("findByEduCodeOrderByInstDateDesc - 카테고리별 게시물 조회")
    inner class FindByEduCodeTests {

        @Test
        @DisplayName("정상 조회 - 카테고리별 게시물 최신순 정렬")
        fun findByEduCode_success() {
            // Given
            val post1 = persistPost(
                eduId = "EDU001",
                eduCode = "TASTING_MANUAL",
                eduTitle = "진짬뽕 시식 매뉴얼",
                instDate = LocalDateTime.of(2020, 8, 10, 0, 0)
            )
            val post2 = persistPost(
                eduId = "EDU002",
                eduCode = "TASTING_MANUAL",
                eduTitle = "미숫가루 시식 매뉴얼",
                instDate = LocalDateTime.of(2020, 8, 11, 0, 0)
            )
            // 다른 카테고리 (조회 대상 아님)
            persistPost(
                eduId = "EDU003",
                eduCode = "CS_SAFETY",
                eduTitle = "안전 수칙"
            )

            // When
            val pageable = PageRequest.of(0, 10)
            val result = educationPostRepository.findByEduCodeOrderByInstDateDesc(
                "TASTING_MANUAL", pageable
            )

            // Then
            assertThat(result.content).hasSize(2)
            assertThat(result.totalElements).isEqualTo(2)
            assertThat(result.content[0].eduId).isEqualTo("EDU002") // 최신순
            assertThat(result.content[1].eduId).isEqualTo("EDU001")
        }
    }

    @Nested
    @DisplayName("findByEduCodeAndSearchWithPaging - 카테고리별 검색")
    inner class FindByEduCodeAndSearchTests {

        @Test
        @DisplayName("정상 검색 - 제목+내용 LIKE 검색")
        fun findByEduCodeAndSearch_success() {
            // Given
            persistPost(
                eduId = "EDU001",
                eduCode = "TASTING_MANUAL",
                eduTitle = "진짬뽕 시식 매뉴얼",
                eduContent = "진짬뽕 시식 방법을 안내합니다."
            )
            persistPost(
                eduId = "EDU002",
                eduCode = "TASTING_MANUAL",
                eduTitle = "미숫가루 매뉴얼",
                eduContent = "미숫가루 시식 방법을 안내합니다."
            )
            persistPost(
                eduId = "EDU003",
                eduCode = "TASTING_MANUAL",
                eduTitle = "라면 매뉴얼",
                eduContent = "라면 조리 방법을 안내합니다."
            )

            // When - "시식" 키워드로 검색
            val pageable = PageRequest.of(0, 10)
            val result = educationPostRepository.findByEduCodeAndSearchWithPaging(
                "TASTING_MANUAL", "시식", pageable
            )

            // Then
            assertThat(result.content).hasSize(2)
            assertThat(result.totalElements).isEqualTo(2)
        }
    }

    @Nested
    @DisplayName("EducationPostAttachment - 첨부파일 조회")
    inner class AttachmentTests {

        @Test
        @DisplayName("정상 조회 - 게시글별 첨부파일 목록")
        fun findByEduId_success() {
            // Given
            persistPost(eduId = "EDU001", eduCode = "TASTING_MANUAL", eduTitle = "진짬뽕")

            testEntityManager.persistAndFlush(
                EducationPostAttachment(
                    eduId = "EDU001",
                    eduFileKey = "file-key-001",
                    eduFileType = "pdf",
                    eduFileOrgNm = "guide.pdf"
                )
            )
            testEntityManager.persistAndFlush(
                EducationPostAttachment(
                    eduId = "EDU001",
                    eduFileKey = "file-key-002",
                    eduFileType = "docx",
                    eduFileOrgNm = "manual.docx"
                )
            )
            testEntityManager.clear()

            // When
            val attachments = educationPostAttachmentRepository.findByEduId("EDU001")

            // Then
            assertThat(attachments).hasSize(2)
            assertThat(attachments.map { it.eduFileOrgNm }).containsExactlyInAnyOrder("guide.pdf", "manual.docx")
        }
    }

    private fun persistPost(
        eduId: String,
        eduCode: String,
        eduTitle: String,
        eduContent: String = "내용",
        instDate: LocalDateTime = LocalDateTime.now()
    ): EducationPost {
        val post = EducationPost(
            eduId = eduId,
            eduTitle = eduTitle,
            eduContent = eduContent,
            eduCode = eduCode,
            instDate = instDate
        )
        val persisted = testEntityManager.persistAndFlush(post)
        testEntityManager.clear()
        return persisted
    }
}
