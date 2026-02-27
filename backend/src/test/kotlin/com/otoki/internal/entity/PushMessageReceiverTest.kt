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
import java.time.temporal.ChronoUnit

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@ActiveProfiles("test")
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
            employeeId = "a0C5g000002DEF",
            messageId = "a0B5g000001ABC"
        )

        // When
        val persisted = testEntityManager.persistAndFlush(receiver)
        testEntityManager.clear()
        val found = testEntityManager.find(PushMessageReceiver::class.java, persisted.id)

        // Then
        assertThat(found).isNotNull
        assertThat(found.id).isGreaterThan(0)
        assertThat(found.name).isEqualTo("RCV-001")
        assertThat(found.employeeId).isEqualTo("a0C5g000002DEF")
        assertThat(found.messageId).isEqualTo("a0B5g000001ABC")
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
        assertThat(found.messageId).isNull()
        assertThat(found.isDeleted).isNull()
        assertThat(found.createdDate).isNull()
        assertThat(found.systemModStamp).isNull()
        assertThat(found.hcLastOp).isNull()
        assertThat(found.hcErr).isNull()
    }

    @Test
    @DisplayName("PushMessageReceiver 생성 - SF 공통 필드 매핑 확인")
    fun createPushMessageReceiver_sfCommonFields() {
        // Given
        val now = LocalDateTime.now().truncatedTo(ChronoUnit.MICROS)
        val receiver = PushMessageReceiver(
            sfid = "a0D5g000003GHI",
            isDeleted = false,
            createdDate = now,
            systemModStamp = now,
            hcLastOp = "SYNCED",
            hcErr = null
        )

        // When
        val persisted = testEntityManager.persistAndFlush(receiver)
        testEntityManager.clear()
        val found = testEntityManager.find(PushMessageReceiver::class.java, persisted.id)

        // Then
        assertThat(found.sfid).isEqualTo("a0D5g000003GHI")
        assertThat(found.isDeleted).isFalse()
        assertThat(found.createdDate).isEqualTo(now)
        assertThat(found.systemModStamp).isEqualTo(now)
        assertThat(found.hcLastOp).isEqualTo("SYNCED")
        assertThat(found.hcErr).isNull()
    }

    @Test
    @DisplayName("PushMessageReceiver - messageId FK 없이 임의 sfid 저장 가능")
    fun createPushMessageReceiver_noFkConstraint() {
        // Given
        val receiver = PushMessageReceiver(
            employeeId = "NON_EXISTENT_EMP",
            messageId = "NON_EXISTENT_MSG"
        )

        // When
        val persisted = testEntityManager.persistAndFlush(receiver)
        testEntityManager.clear()
        val found = testEntityManager.find(PushMessageReceiver::class.java, persisted.id)

        // Then
        assertThat(found).isNotNull
        assertThat(found.employeeId).isEqualTo("NON_EXISTENT_EMP")
        assertThat(found.messageId).isEqualTo("NON_EXISTENT_MSG")
    }
}
