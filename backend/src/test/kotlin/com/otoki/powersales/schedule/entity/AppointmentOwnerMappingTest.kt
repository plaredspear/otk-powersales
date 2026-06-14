package com.otoki.powersales.schedule.entity

import com.otoki.powersales.platform.common.salesforce.SFField
import com.otoki.powersales.platform.common.salesforce.SFSchemaUtils
import com.otoki.powersales.domain.org.employee.entity.Group
import com.otoki.powersales.user.entity.User
import jakarta.persistence.JoinColumn
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/**
 * Spec #755 — Appointment.OwnerId R-2 polymorphic 매핑 검증.
 *
 * SF `Appointment__c.OwnerId.referenceTo = [Group, User]` polymorphic 의 R-2 정합:
 *   - owner_sfid (varchar 18) sync buffer
 *   - owner_user_id (User FK)
 *   - owner_group_id (Group FK)
 *   - XOR CHECK 제약 (DB 레벨 — DataIntegrityViolationException 발생은 통합 테스트 책임)
 */
@DisplayName("Appointment OwnerId polymorphic 매핑 (Spec #755)")
class AppointmentOwnerMappingTest {

    @Nested
    @DisplayName("AC1 — @SFField('OwnerId') sync buffer")
    inner class OwnerSfidMapping {

        @Test
        @DisplayName("ownerSfid 필드 @SFField('OwnerId') 부착")
        fun ownerSfidSfField() {
            val field = Appointment::class.java.declaredFields.first { it.name == "ownerSfid" }
            val sfField = field.getAnnotation(SFField::class.java)
            assertThat(sfField).isNotNull
            assertThat(sfField.value).isEqualTo("OwnerId")
        }

        @Test
        @DisplayName("SF 매핑에 OwnerId → owner_sfid 등재")
        fun sfMappingContainsOwnerId() {
            val mapping = SFSchemaUtils.getSFMapping(Appointment::class.java)
            assertThat(mapping["OwnerId"]).isEqualTo("owner_sfid")
        }
    }

    @Nested
    @DisplayName("AC2 — polymorphic FK 분기 (owner_user_id / owner_group_id)")
    inner class PolymorphicFk {

        @Test
        @DisplayName("ownerUser 필드 @JoinColumn(name='owner_user_id') + User 타입")
        fun ownerUserJoinColumn() {
            val field = Appointment::class.java.declaredFields.first { it.name == "ownerUser" }
            val join = field.getAnnotation(JoinColumn::class.java)
            assertThat(join).isNotNull
            assertThat(join.name).isEqualTo("owner_user_id")
            assertThat(field.type).isEqualTo(User::class.java)
        }

        @Test
        @DisplayName("ownerGroup 필드 @JoinColumn(name='owner_group_id') + Group 타입")
        fun ownerGroupJoinColumn() {
            val field = Appointment::class.java.declaredFields.first { it.name == "ownerGroup" }
            val join = field.getAnnotation(JoinColumn::class.java)
            assertThat(join).isNotNull
            assertThat(join.name).isEqualTo("owner_group_id")
            assertThat(field.type).isEqualTo(Group::class.java)
        }
    }
}
