package com.otoki.powersales.platform.auth.sharing.entity

import com.otoki.powersales.common.salesforce.SFObject
import com.otoki.powersales.platform.auth.sharing.entity.PermissionSet
import com.otoki.powersales.platform.auth.sharing.entity.PermissionSetAssignment
import jakarta.persistence.Table
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/**
 * Spec #796 / #798 — auth/sharing 영역의 SOQL 출처 entity 2종이 SF SObject 매핑임을 박제.
 *
 * 본 entity 들은 `@SFField` 부착 패턴이 아닌 staging-style mirror (sfid + 한정 컬럼) 이므로
 * `@SFObject` value 와 `@Table` name 정합만 검증한다.
 *
 * - XML 메타 출처 entity (PermissionSetFlags, ProfileFlags, SharingRule*, RecordType, FLS, OWD 등) 는
 *   SF SObject 매핑이 아니므로 `@SFObject` 미부착이 정합.
 */
class PermissionSetSFAnnotationTest {

    @Nested
    @DisplayName("PermissionSet — @SFObject(\"PermissionSet\")")
    inner class PermissionSetAnnotation {

        @Test
        @DisplayName("@SFObject 값은 'PermissionSet'")
        fun sfObjectValue() {
            val annotation = PermissionSet::class.java.getAnnotation(SFObject::class.java)
            assertThat(annotation).isNotNull
            assertThat(annotation.value).isEqualTo("PermissionSet")
        }

        @Test
        @DisplayName("@Table name = 'permission_set'")
        fun tableName() {
            val table = PermissionSet::class.java.getAnnotation(Table::class.java)
            assertThat(table.name).isEqualTo("permission_set")
        }
    }

    @Nested
    @DisplayName("PermissionSetAssignment — @SFObject(\"PermissionSetAssignment\")")
    inner class PermissionSetAssignmentAnnotation {

        @Test
        @DisplayName("@SFObject 값은 'PermissionSetAssignment'")
        fun sfObjectValue() {
            val annotation = PermissionSetAssignment::class.java.getAnnotation(SFObject::class.java)
            assertThat(annotation).isNotNull
            assertThat(annotation.value).isEqualTo("PermissionSetAssignment")
        }

        @Test
        @DisplayName("@Table name = 'permission_set_assignment'")
        fun tableName() {
            val table = PermissionSetAssignment::class.java.getAnnotation(Table::class.java)
            assertThat(table.name).isEqualTo("permission_set_assignment")
        }
    }
}
