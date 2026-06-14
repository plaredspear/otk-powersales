package com.otoki.powersales.platform.push.entity

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles
import com.otoki.powersales.platform.common.config.QueryDslConfig
import com.otoki.powersales.platform.push.entity.PushMessage
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@ActiveProfiles("test")
@Import(QueryDslConfig::class)
@DisplayName("PushMessage Entity 매핑 테스트")
class PushMessageTest {

    @Autowired
    private lateinit var testEntityManager: TestEntityManager

    @Test
    @DisplayName("PushMessage 생성/조회 - 기본 필드 매핑 확인")
    fun createPushMessage_basicFields() {
        // Given
        val now = LocalDateTime.now().truncatedTo(ChronoUnit.MICROS)
        val pushMessage = PushMessage(
            name = "공지사항 알림",
            message = "신제품 출시 안내",
            scheduleDate = now
        )

        // When
        val persisted = testEntityManager.persistAndFlush(pushMessage)
        testEntityManager.clear()
        val found = testEntityManager.find(PushMessage::class.java, persisted.id)!!

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
        val found = testEntityManager.find(PushMessage::class.java, persisted.id)!!

        // Then
        assertThat(found).isNotNull
        assertThat(found.sfid).isNull()
        assertThat(found.name).isNull()
        assertThat(found.message).isNull()
        assertThat(found.scheduleDate).isNull()
        assertThat(found.isDeleted).isNull()
        assertThat(found.createdAt).isNotNull()
        assertThat(found.updatedAt).isNotNull()
    }

    @Test
    @DisplayName("PushMessage 생성 - SF 공통 필드 매핑 확인")
    fun createPushMessage_sfCommonFields() {
        // Given
        val pushMessage = PushMessage(
            sfid = "a0B5g000001ABC",
            isDeleted = false
        )

        // When
        val persisted = testEntityManager.persistAndFlush(pushMessage)
        testEntityManager.clear()
        val found = testEntityManager.find(PushMessage::class.java, persisted.id)!!

        // Then — JPA save 경로는 AuditingEntityListener 가 createdAt/updatedAt 을
        // 자동 채운다. 본 테스트의 관심사는 sfid/isDeleted 매핑이므로 timestamp 는
        // 비어있지 않은지만 확인한다.
        assertThat(found.sfid).isEqualTo("a0B5g000001ABC")
        assertThat(found.isDeleted).isFalse()
        assertThat(found.createdAt).isNotNull()
        assertThat(found.updatedAt).isNotNull()
    }
}
