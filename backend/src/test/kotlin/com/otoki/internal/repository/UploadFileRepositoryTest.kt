package com.otoki.internal.repository

import com.otoki.internal.entity.UploadFile
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager
import org.springframework.test.context.ActiveProfiles
import java.time.LocalDateTime

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@ActiveProfiles("test")
class UploadFileRepositoryTest {

    @Autowired
    private lateinit var uploadFileRepository: UploadFileRepository

    @Autowired
    private lateinit var testEntityManager: TestEntityManager

    @BeforeEach
    fun setUp() {
        uploadFileRepository.deleteAll()
        testEntityManager.clear()
    }

    @Nested
    @DisplayName("기본 CRUD 테스트")
    inner class BasicCrudTests {

        @Test
        @DisplayName("UploadFile 저장 및 조회 - auto-increment PK 자동 생성 확인")
        fun saveAndFindById() {
            // Given
            val now = LocalDateTime.of(2026, 2, 24, 10, 0, 0)
            val uploadFile = UploadFile(
                sfid = "a0B5g00000TEST01",
                name = "test-image.png",
                recordId = "REC-001",
                uniqueKey = "uploads/2026/02/test-image.png",
                size = "1024",
                isDeleted = false,
                createdDate = now,
                systemModStamp = now,
                hcLastOp = "SYNCED",
                hcErr = null
            )
            val saved = testEntityManager.persistAndFlush(uploadFile)
            testEntityManager.clear()

            // When
            val result = uploadFileRepository.findById(saved.id)

            // Then
            assertThat(result).isPresent
            val found = result.get()
            assertThat(found.id).isGreaterThan(0)
            assertThat(found.sfid).isEqualTo("a0B5g00000TEST01")
            assertThat(found.name).isEqualTo("test-image.png")
            assertThat(found.recordId).isEqualTo("REC-001")
            assertThat(found.uniqueKey).isEqualTo("uploads/2026/02/test-image.png")
            assertThat(found.size).isEqualTo("1024")
            assertThat(found.isDeleted).isEqualTo(false)
            assertThat(found.createdDate).isEqualTo(now)
            assertThat(found.systemModStamp).isEqualTo(now)
            assertThat(found.hcLastOp).isEqualTo("SYNCED")
            assertThat(found.hcErr).isNull()
        }

        @Test
        @DisplayName("존재하지 않는 ID 조회 시 empty 반환")
        fun findById_notFound_returnsEmpty() {
            // When
            val result = uploadFileRepository.findById(99999L)

            // Then
            assertThat(result).isEmpty
        }
    }
}
