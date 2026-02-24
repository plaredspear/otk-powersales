package com.otoki.internal.repository

import com.otoki.internal.entity.DeviceVersion
import com.otoki.internal.entity.DeviceVersionId
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
class DeviceVersionRepositoryTest {

    @Autowired
    private lateinit var deviceVersionRepository: DeviceVersionRepository

    @Autowired
    private lateinit var testEntityManager: TestEntityManager

    @BeforeEach
    fun setUp() {
        deviceVersionRepository.deleteAll()
        testEntityManager.clear()
    }

    @Nested
    @DisplayName("복합 키 CRUD 테스트")
    inner class CompositeKeyCrudTests {

        @Test
        @DisplayName("DeviceVersion 저장 및 복합 키로 조회 - version/device 저장 후 8개 필드 일치 확인")
        fun saveAndFindByCompositeKey() {
            // Given
            val now = LocalDateTime.of(2026, 2, 24, 10, 0, 0)
            val deviceVersion = DeviceVersion(
                version = "1.0.0",
                device = "AOS",
                createDate = now,
                contents = "초기 릴리스",
                s3Key = "apk/v1.0.0/app.apk",
                fileUrl = "https://cdn.example.com/app.apk",
                s3KeyIpa = "ipa/v1.0.0/app.ipa",
                fileUrlIpa = "https://cdn.example.com/app.ipa"
            )
            testEntityManager.persistAndFlush(deviceVersion)
            testEntityManager.clear()

            // When
            val id = DeviceVersionId(version = "1.0.0", device = "AOS")
            val result = deviceVersionRepository.findById(id)

            // Then
            assertThat(result).isPresent
            val found = result.get()
            assertThat(found.version).isEqualTo("1.0.0")
            assertThat(found.device).isEqualTo("AOS")
            assertThat(found.createDate).isEqualTo(now)
            assertThat(found.contents).isEqualTo("초기 릴리스")
            assertThat(found.s3Key).isEqualTo("apk/v1.0.0/app.apk")
            assertThat(found.fileUrl).isEqualTo("https://cdn.example.com/app.apk")
            assertThat(found.s3KeyIpa).isEqualTo("ipa/v1.0.0/app.ipa")
            assertThat(found.fileUrlIpa).isEqualTo("https://cdn.example.com/app.ipa")
        }

        @Test
        @DisplayName("미존재 복합 키 조회 시 Optional.empty() 반환")
        fun findById_notFound_returnsEmpty() {
            // When
            val nonExistId = DeviceVersionId(version = "99.99.99", device = "NONE")
            val result = deviceVersionRepository.findById(nonExistId)

            // Then
            assertThat(result).isEmpty
        }
    }
}
