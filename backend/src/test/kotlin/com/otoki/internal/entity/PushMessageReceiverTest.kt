package com.otoki.internal.entity

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles
import com.otoki.internal.common.config.QueryDslConfig
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@ActiveProfiles("test")
@Import(QueryDslConfig::class)
@DisplayName("PushMessageReceiver Entity 매핑 테스트")
class PushMessageReceiverTest {

    @Autowired
    private lateinit var testEntityManager: TestEntityManager

    @Test
    @DisplayName("PushMessageReceiver 생성/조회 - 기본 필드 매핑 확인")
    fun createPushMessageReceiver_basicFields() {
        // Given
        val receiver = PushMessageReceiver(
            name = "RCV-001",
            employeeId = 100L,
            pushMessageId = 200
        )

        // When
        val persisted = testEntityManager.persistAndFlush(receiver)
        testEntityManager.clear()
        val found = testEntityManager.find(PushMessageReceiver::class.java, persisted.id)

        // Then
        assertThat(found).isNotNull
        assertThat(found.id).isGreaterThan(0)
        assertThat(found.name).isEqualTo("RCV-001")
        assertThat(found.employeeId).isEqualTo(100L)
        assertThat(found.pushMessageId).isEqualTo(200)
    }

    @Test
    @DisplayName("PushMessageReceiver 생성 - nullable 필드 null 허용 확인")
    fun createPushMessageReceiver_nullableFields() {
        // Given
        val receiver = PushMessageReceiver()

        // When
        val persisted = testEntityManager.persistAndFlush(receiver)
        testEntityManager.clear()
        val found = testEntityManager.find(PushMessageReceiver::class.java, persisted.id)

        // Then
        assertThat(found).isNotNull
        assertThat(found.sfid).isNull()
        assertThat(found.name).isNull()
        assertThat(found.employeeId).isNull()
        assertThat(found.pushMessageId).isNull()
        assertThat(found.employeeSfid).isNull()
        assertThat(found.pushMessageSfid).isNull()
        assertThat(found.isDeleted).isNull()
        assertThat(found.createdAt).isNotNull()
        assertThat(found.updatedAt).isNotNull()
    }

    @Test
    @DisplayName("PushMessageReceiver 생성 - SF 공통 필드 매핑 확인")
    fun createPushMessageReceiver_sfCommonFields() {
        // Given
        val now = LocalDateTime.now().truncatedTo(ChronoUnit.MICROS)
        val receiver = PushMessageReceiver(
            sfid = "a0D5g000003GHI",
            isDeleted = false
        ).apply {
            createdAt = now
            updatedAt = now
        }

        // When
        val persisted = testEntityManager.persistAndFlush(receiver)
        testEntityManager.clear()
        val found = testEntityManager.find(PushMessageReceiver::class.java, persisted.id)

        // Then
        assertThat(found.sfid).isEqualTo("a0D5g000003GHI")
        assertThat(found.isDeleted).isFalse()
        assertThat(found.createdAt).isEqualTo(now)
        assertThat(found.updatedAt).isEqualTo(now)
    }

    @Test
    @DisplayName("PushMessageReceiver - PK 참조 FK 없이 임의 ID 저장 가능")
    fun createPushMessageReceiver_noFkConstraint() {
        // Given
        val receiver = PushMessageReceiver(
            employeeId = 99999L,
            pushMessageId = 88888
        )

        // When
        val persisted = testEntityManager.persistAndFlush(receiver)
        testEntityManager.clear()
        val found = testEntityManager.find(PushMessageReceiver::class.java, persisted.id)

        // Then
        assertThat(found).isNotNull
        assertThat(found.employeeId).isEqualTo(99999L)
        assertThat(found.pushMessageId).isEqualTo(88888)
    }
}
