package com.otoki.internal.entity

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
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
@DisplayName("PushMessage Entity 매핑 테스트")
class PushMessageTest {

    @Autowired
    private lateinit var testEntityManager: TestEntityManager

    @Test
    @DisplayName("PushMessage 생성/조회 - 기본 필드 매핑 확인")
    fun createPushMessage_basicFields() {
        // Given
        val now = LocalDateTime.now()
        val pushMessage = PushMessage(
            name = "공지사항 알림",
            message = "신제품 출시 안내",
            scheduleDate = now
        )

        // When
        val persisted = testEntityManager.persistAndFlush(pushMessage)
        testEntityManager.clear()
        val found = testEntityManager.find(PushMessage::class.java, persisted.id)

        // Then
        assertThat(found).isNotNull
        assertThat(found.id).isGreaterThan(0)
        assertThat(found.name).isEqualTo("공지사항 알림")
        assertThat(found.message).isEqualTo("신제품 출시 안내")
        assertThat(found.scheduleDate).isEqualTo(now)
    }

    @Test
    @DisplayName("PushMessage 생성 - nullable 필드 null 허용 확인")
    fun createPushMessage_nullableFields() {
        // Given
        val pushMessage = PushMessage()

        // When
        val persisted = testEntityManager.persistAndFlush(pushMessage)
        testEntityManager.clear()
        val found = testEntityManager.find(PushMessage::class.java, persisted.id)

        // Then
        assertThat(found).isNotNull
        assertThat(found.sfid).isNull()
        assertThat(found.name).isNull()
        assertThat(found.message).isNull()
        assertThat(found.scheduleDate).isNull()
        assertThat(found.isDeleted).isNull()
        assertThat(found.createdDate).isNull()
        assertThat(found.systemModStamp).isNull()
        assertThat(found.hcLastOp).isNull()
        assertThat(found.hcErr).isNull()
    }

    @Test
    @DisplayName("PushMessage 생성 - SF 공통 필드 매핑 확인")
    fun createPushMessage_sfCommonFields() {
        // Given
        val now = LocalDateTime.now()
        val pushMessage = PushMessage(
            sfid = "a0B5g000001ABC",
            isDeleted = false,
            createdDate = now,
            systemModStamp = now,
            hcLastOp = "SYNCED",
            hcErr = null
        )

        // When
        val persisted = testEntityManager.persistAndFlush(pushMessage)
        testEntityManager.clear()
        val found = testEntityManager.find(PushMessage::class.java, persisted.id)

        // Then
        assertThat(found.sfid).isEqualTo("a0B5g000001ABC")
        assertThat(found.isDeleted).isFalse()
        assertThat(found.createdDate).isEqualTo(now)
        assertThat(found.systemModStamp).isEqualTo(now)
        assertThat(found.hcLastOp).isEqualTo("SYNCED")
        assertThat(found.hcErr).isNull()
    }
}
