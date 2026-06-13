package com.otoki.powersales.claim.entity

import com.otoki.powersales.claim.entity.converter.ClaimChannelConverter
import com.otoki.powersales.claim.entity.converter.ClaimStatusConverter
import com.otoki.powersales.claim.entity.converter.ClaimType1Converter
import com.otoki.powersales.claim.entity.converter.ClaimType2Converter
import com.otoki.powersales.claim.enums.ClaimChannel
import com.otoki.powersales.claim.enums.ClaimStatus
import com.otoki.powersales.claim.enums.ClaimType1
import com.otoki.powersales.claim.enums.ClaimType2
import com.otoki.powersales.common.salesforce.SFField
import com.otoki.powersales.common.salesforce.SFObject
import com.otoki.powersales.common.salesforce.SFSchemaUtils
import com.otoki.powersales.domain.foundation.product.entity.Product
import jakarta.persistence.Column
import jakarta.persistence.Convert
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/**
 * Spec #606 / #705 / #743 / sf-meta-diff Q2~Q8 — Claim ↔ Salesforce `DKRetail__Claim__c` 어노테이션 부착 검증.
 *
 * 단일 권위: Salesforce Object 메타 (`DKRetail__Claim__c`)
 *
 * 검증 분류:
 *   - AC1: 클래스 `@SFObject` 무변경
 *   - AC2: `@SFField` 매핑 키셋 — Formula `@SFField` 4건 제거 (`product_code`/`barcode`/`phone`/`product_status`) 반영
 *   - AC3: PK / FK 미부착 — owner polymorphic 분리 (ownerUser/ownerGroup) 반영
 *   - AC5: ClaimStatus enum SF 정합 (DRAFT/SENT/SEND_FAILED + Converter) — #705 §6 Q4
 *   - AC6: ClaimChannel enum + Converter
 *   - AC7: Reference R-2 FK relation 검증 — Q2 polymorphic owner (ownerUser/ownerGroup → User?/Group?) + Q3/Q4 audit FK User 타입 전환
 *   - AC8: ClaimType1/ClaimType2 enum 정합 + Converter — #743 (#741 옵션 C 적용)
 */
@DisplayName("Claim SF 어노테이션 검증 (Spec #606 / #705 / #743)")
class ClaimSFAnnotationTest {

    @Nested
    @DisplayName("AC1 — 클래스 @SFObject 무변경")
    inner class ClassAnnotation {

        @Test
        @DisplayName("@SFObject 값은 'DKRetail__Claim__c'")
        fun sfObjectValue() {
            val annotation = Claim::class.java.getAnnotation(SFObject::class.java)
            assertThat(annotation).isNotNull
            assertThat(annotation.value).isEqualTo("DKRetail__Claim__c")
        }
    }

    @Nested
    @DisplayName("AC2 — @SFField 매핑 키셋")
    inner class SfFieldMapping {

        private val mapping = SFSchemaUtils.getSFMapping(Claim::class.java)

        @Test
        @DisplayName("매핑 키 수 = 40 (#705 + #743 = 44 → Q1/Q8 Formula @SFField 4건 제거)")
        fun mappingKeySize() {
            assertThat(mapping).hasSize(40)
        }

        @Test
        @DisplayName("#743 ClaimType1 / ClaimType2 매핑 (2개)")
        fun spec743ClaimTypes() {
            assertThat(mapping["DKRetail__ClaimType1__c"]).isEqualTo("claim_type1")
            assertThat(mapping["DKRetail__ClaimType2__c"]).isEqualTo("claim_type2")
        }

        @Test
        @DisplayName("#705 Group A + Reference R-2 매핑 (5개)")
        fun spec705GroupA() {
            assertThat(mapping["IsDeleted"]).isEqualTo("is_deleted")
            assertThat(mapping["OwnerId"]).isEqualTo("owner_sfid")
            assertThat(mapping["CreatedById"]).isEqualTo("created_by_sfid")
            assertThat(mapping["LastModifiedById"]).isEqualTo("last_modified_by_sfid")
        }

        @Test
        @DisplayName("#705 BaseEntity 상속 (CreatedDate / LastModifiedDate)")
        fun spec705BaseEntity() {
            assertThat(mapping["CreatedDate"]).isEqualTo("created_at")
            assertThat(mapping["LastModifiedDate"]).isEqualTo("updated_at")
        }

        @Test
        @DisplayName("§6.1 — 기존 OK 10개 매핑 (FK 2개 제외, Q1 Formula `DKRetail__ProductCode__c` 제거)")
        fun section61Existing() {
            assertThat(mapping["Name"]).isEqualTo("name")
            assertThat(mapping["DKRetail__EmployeeId__c"]).isEqualTo("employee_sfid")
            assertThat(mapping["DKRetail__AccountId__c"]).isEqualTo("account_sfid")
            // Q1: SF Formula 필드 — entity 컬럼은 application 입력값 캐시로 유지, `@SFField` 만 제거
            assertThat(mapping["DKRetail__ProductCode__c"]).isNull()
            assertThat(mapping["DKRetail__ClaimDate__c"]).isEqualTo("date")
            assertThat(mapping["DKRetail__Description__c"]).isEqualTo("defect_description")
            assertThat(mapping["DKRetail__Quantity__c"]).isEqualTo("defect_quantity")
            assertThat(mapping["DKRetail__Amount__c"]).isEqualTo("purchase_amount")
            assertThat(mapping["DKRetail__PurchaseMethod__c"]).isEqualTo("purchase_method_code")
            assertThat(mapping["DKRetail__RequestType__c"]).isEqualTo("request_type_code")
            assertThat(mapping["DKRetail__Status__c"]).isEqualTo("status")
        }

        @Test
        @DisplayName("Q8 — Formula `Barcode` / `Phone` / `ProductStatus` 의 @SFField 제거")
        fun q8FormulaUnmapped() {
            assertThat(mapping["DKRetail__Barcode__c"]).isNull()
            assertThat(mapping["DKRetail__Phone__c"]).isNull()
            assertThat(mapping["DKRetail__ProductStatus__c"]).isNull()
        }

        @Test
        @DisplayName("§6.2 — SAP inbound 6개 신규 부착")
        fun section62SapInbound() {
            assertThat(mapping["DKRetail__counselNumber__c"]).isEqualTo("counsel_number")
            assertThat(mapping["DKRetail__ActionCode__c"]).isEqualTo("action_code")
            assertThat(mapping["DKRetail__ActionStatus__c"]).isEqualTo("action_status")
            assertThat(mapping["ActContent__c"]).isEqualTo("act_content")
            assertThat(mapping["DKRetail__ReasonType__c"]).isEqualTo("reason_type")
            assertThat(mapping["DKRetail__CosmosKey__c"]).isEqualTo("cosmos_key")
        }

        @Test
        @DisplayName("§6.3 — 신규 16개 필드 매핑")
        fun section63NewFields() {
            assertThat(mapping["DKRetail__ProductId__c"]).isEqualTo("product_sfid")
            assertThat(mapping["ReturnOrderNumber__c"]).isEqualTo("customer_delivery_date")
            assertThat(mapping["DKRetail__ReturnOrderNumber__c"]).isEqualTo("return_order_number")
            assertThat(mapping["DKRetail__ExpirationDate__c"]).isEqualTo("expiration_date")
            assertThat(mapping["DKRetail__InterfaceDate__c"]).isEqualTo("interface_date")
            assertThat(mapping["DKRetail__ManufacturingDate__c"]).isEqualTo("manufacturing_date")
            assertThat(mapping["DKRetail__InitialClaim__c"]).isEqualTo("initial_claim")
            assertThat(mapping["DKRetail__LogisticsCenter__c"]).isEqualTo("logistics_center")
            assertThat(mapping["ClaimSequence__c"]).isEqualTo("claim_sequence")
            assertThat(mapping["DKRetail__DetailSNSName__c"]).isEqualTo("detail_sns_name")
            assertThat(mapping["CostCenterCode__c"]).isEqualTo("cost_center_code")
            assertThat(mapping["division__c"]).isEqualTo("division")
            assertThat(mapping["DKRetail__Channel__c"]).isEqualTo("channel")
            assertThat(mapping["DKRetail__SampleCollectionFlag__c"]).isEqualTo("sample_collection_flag")
            assertThat(mapping["ImageCount__c"]).isEqualTo("image_count")
            assertThat(mapping["DKRetail__ActionDate__c"]).isEqualTo("action_date")
        }
    }

    @Nested
    @DisplayName("AC3 — PK / FK 미부착")
    inner class PkFkExclusion {

        @Test
        @DisplayName("PK(id) 필드에 @SFField 미부착")
        fun idHasNoSfField() {
            val field = Claim::class.java.getDeclaredField("id")
            assertThat(field.isAnnotationPresent(SFField::class.java)).isFalse()
        }

        @Test
        @DisplayName("FK 필드(employee, account, ownerUser, ownerGroup, createdBy, lastModifiedBy, product)에 @SFField 미부착")
        fun fkHasNoSfField() {
            listOf("employee", "account", "ownerUser", "ownerGroup", "createdBy", "lastModifiedBy", "product").forEach { name ->
                val field = Claim::class.java.getDeclaredField(name)
                assertThat(field.isAnnotationPresent(SFField::class.java))
                    .withFailMessage("FK 필드 $name 에 @SFField 부착되어 있음 — §2.4/§6.6 정책 위반")
                    .isFalse()
            }
        }

        @Test
        @DisplayName("매핑 values 에 PK/FK 컬럼명 미등장")
        fun mappingValuesExcludePkFk() {
            val mapping = SFSchemaUtils.getSFMapping(Claim::class.java)
            assertThat(mapping.values).doesNotContain(
                "claim_id",
                "employee_id",
                "account_id",
                "owner_user_id",
                "owner_group_id",
                "created_by_id",
                "last_modified_by_id",
                "product_id",
            )
        }
    }

    @Nested
    @DisplayName("AC5 — ClaimStatus enum SF 정합 (#705 §6 Q4)")
    inner class ClaimStatusEnumConsistency {

        @Test
        @DisplayName("ClaimStatus.values() = 4개 멤버 (#829 dual-write 추가: SF_PENDING)")
        fun claimStatusMembers() {
            // SF `DKRetail__Status__c` picklist (3개 — 임시저장/전송완료/전송실패) + #829 SF_PENDING 신설.
            // SF picklist 측은 운영자가 별도로 '전송대기' 값을 추가하거나, backend → SF push 직전 임시 상태로만 사용.
            assertThat(ClaimStatus.entries).hasSize(4)
            assertThat(ClaimStatus.entries.map { it.name })
                .containsExactlyInAnyOrder("DRAFT", "SF_PENDING", "SENT", "SEND_FAILED")
            assertThat(ClaimStatus.DRAFT.displayName).isEqualTo("임시저장")
            assertThat(ClaimStatus.SF_PENDING.displayName).isEqualTo("전송대기")
            assertThat(ClaimStatus.SENT.displayName).isEqualTo("전송완료")
            assertThat(ClaimStatus.SEND_FAILED.displayName).isEqualTo("전송실패")
        }

        @Test
        @DisplayName("ClaimStatusConverter — enum ↔ DB 한국어 displayName 양방향")
        fun converter() {
            val converter = ClaimStatusConverter()
            assertThat(converter.convertToDatabaseColumn(ClaimStatus.DRAFT)).isEqualTo("임시저장")
            assertThat(converter.convertToDatabaseColumn(ClaimStatus.SENT)).isEqualTo("전송완료")
            assertThat(converter.convertToDatabaseColumn(ClaimStatus.SEND_FAILED)).isEqualTo("전송실패")
            assertThat(converter.convertToDatabaseColumn(null)).isNull()
            assertThat(converter.convertToEntityAttribute("임시저장")).isEqualTo(ClaimStatus.DRAFT)
            assertThat(converter.convertToEntityAttribute("전송완료")).isEqualTo(ClaimStatus.SENT)
            assertThat(converter.convertToEntityAttribute("전송실패")).isEqualTo(ClaimStatus.SEND_FAILED)
            assertThat(converter.convertToEntityAttribute(null)).isNull()
            assertThat(converter.convertToEntityAttribute("UNKNOWN")).isNull()
        }

        @Test
        @DisplayName("Claim.status 필드 = @Convert(ClaimStatusConverter)")
        fun statusFieldHasConvert() {
            val field = Claim::class.java.getDeclaredField("status")
            assertThat(field.type).isEqualTo(ClaimStatus::class.java)
            val convert = field.getAnnotation(Convert::class.java)
            assertThat(convert).isNotNull
            assertThat(convert.converter.java).isEqualTo(ClaimStatusConverter::class.java)
        }
    }

    @Nested
    @DisplayName("AC7 — Reference R-2 FK 검증 (#705 §4 + sf-meta-diff Q2/Q3/Q4)")
    inner class ReferenceR2 {

        @Test
        @DisplayName("ownerUser / ownerGroup / createdBy / lastModifiedBy / product 5개 FK 필드 + @ManyToOne + @JoinColumn")
        fun referenceFks() {
            mapOf(
                "ownerUser" to ("owner_user_id" to com.otoki.powersales.user.entity.User::class.java),
                "ownerGroup" to ("owner_group_id" to com.otoki.powersales.employee.entity.Group::class.java),
                "createdBy" to ("created_by_id" to com.otoki.powersales.user.entity.User::class.java),
                "lastModifiedBy" to ("last_modified_by_id" to com.otoki.powersales.user.entity.User::class.java),
                "product" to ("product_id" to Product::class.java)
            ).forEach { (fieldName, expected) ->
                val (expectedColumn, expectedType) = expected
                val field = Claim::class.java.getDeclaredField(fieldName)
                assertThat(field.isAnnotationPresent(ManyToOne::class.java))
                    .withFailMessage("$fieldName 에 @ManyToOne 미부착").isTrue()
                val joinColumn = field.getAnnotation(JoinColumn::class.java)
                assertThat(joinColumn).withFailMessage("$fieldName 에 @JoinColumn 미부착").isNotNull
                assertThat(joinColumn.name).isEqualTo(expectedColumn)
                assertThat(field.type)
                    .withFailMessage("$fieldName 의 FK 타입 불일치 (기대: ${expectedType.simpleName}, 실제: ${field.type.simpleName})")
                    .isEqualTo(expectedType)
            }
        }

        @Test
        @DisplayName("Q5/Q6/Q7 — SF 절단 위험 정합 (name=80, action_code=40, counsel_number=40)")
        fun sfLengthAlignment() {
            mapOf(
                "name" to 80,
                "actionCode" to 40,
                "counselNumber" to 40
            ).forEach { (fieldName, expectedLength) ->
                val field = Claim::class.java.getDeclaredField(fieldName)
                val column = field.getAnnotation(Column::class.java)
                assertThat(column).withFailMessage("$fieldName 에 @Column 미부착").isNotNull
                assertThat(column.length)
                    .withFailMessage("$fieldName length=${column.length} (기대=$expectedLength) — SF 정합 깨짐")
                    .isEqualTo(expectedLength)
            }
        }

        @Test
        @DisplayName("ownerSfid / createdBySfid / lastModifiedBySfid — @SFField 부착")
        fun sfidBuffers() {
            mapOf(
                "ownerSfid" to "OwnerId",
                "createdBySfid" to "CreatedById",
                "lastModifiedBySfid" to "LastModifiedById"
            ).forEach { (fieldName, sfName) ->
                val field = Claim::class.java.getDeclaredField(fieldName)
                val sfField = field.getAnnotation(SFField::class.java)
                assertThat(sfField).withFailMessage("$fieldName 에 @SFField 미부착").isNotNull
                assertThat(sfField.value).isEqualTo(sfName)
            }
        }
    }

    @Nested
    @DisplayName("AC8 — ClaimType1 / ClaimType2 enum + Converter (#743)")
    inner class ClaimTypeEnums {

        @Test
        @DisplayName("ClaimType1.entries = 3개 (A/B/C) — SF picklist 옵션값 정합")
        fun claimType1Members() {
            assertThat(ClaimType1.entries).hasSize(3)
            assertThat(ClaimType1.entries.map { it.value }).containsExactly("A", "B", "C")
            assertThat(ClaimType1.A.label).isEqualTo("포장불량")
            assertThat(ClaimType1.B.label).isEqualTo("이물혼입")
            assertThat(ClaimType1.C.label).isEqualTo("내용물이상")
        }

        @Test
        @DisplayName("ClaimType2.entries = 25개 (A/B/C 별 12/7/6) — SF dependent picklist 정합")
        fun claimType2Members() {
            assertThat(ClaimType2.entries).hasSize(25)
            assertThat(ClaimType2.entries.count { it.parent == ClaimType1.A }).isEqualTo(12)
            assertThat(ClaimType2.entries.count { it.parent == ClaimType1.B }).isEqualTo(7)
            assertThat(ClaimType2.entries.count { it.parent == ClaimType1.C }).isEqualTo(6)
        }

        @Test
        @DisplayName("ClaimType2 옵션값 = enum constant name 동일 (AA/AB/.../CF/AL)")
        fun claimType2ValuesMatchNames() {
            ClaimType2.entries.forEach { entry ->
                assertThat(entry.value).isEqualTo(entry.name)
            }
        }

        @Test
        @DisplayName("ClaimType1Converter — enum ↔ DB String 양방향")
        fun claimType1Converter() {
            val converter = ClaimType1Converter()
            assertThat(converter.convertToDatabaseColumn(ClaimType1.A)).isEqualTo("A")
            assertThat(converter.convertToDatabaseColumn(null)).isNull()
            assertThat(converter.convertToEntityAttribute("A")).isEqualTo(ClaimType1.A)
            assertThat(converter.convertToEntityAttribute("B")).isEqualTo(ClaimType1.B)
            assertThat(converter.convertToEntityAttribute(null)).isNull()
            assertThat(converter.convertToEntityAttribute("UNKNOWN")).isNull()
        }

        @Test
        @DisplayName("ClaimType2Converter — enum ↔ DB String 양방향")
        fun claimType2Converter() {
            val converter = ClaimType2Converter()
            assertThat(converter.convertToDatabaseColumn(ClaimType2.AA)).isEqualTo("AA")
            assertThat(converter.convertToDatabaseColumn(ClaimType2.AL)).isEqualTo("AL")
            assertThat(converter.convertToDatabaseColumn(null)).isNull()
            assertThat(converter.convertToEntityAttribute("AA")).isEqualTo(ClaimType2.AA)
            assertThat(converter.convertToEntityAttribute("CF")).isEqualTo(ClaimType2.CF)
            assertThat(converter.convertToEntityAttribute(null)).isNull()
            assertThat(converter.convertToEntityAttribute("UNKNOWN")).isNull()
        }

        @Test
        @DisplayName("Claim.claimType1 / claimType2 필드 = @Convert + 적절한 타입")
        fun claimTypeFieldsHaveConvert() {
            val field1 = Claim::class.java.getDeclaredField("claimType1")
            assertThat(field1.type).isEqualTo(ClaimType1::class.java)
            val convert1 = field1.getAnnotation(Convert::class.java)
            assertThat(convert1).isNotNull
            assertThat(convert1.converter.java).isEqualTo(ClaimType1Converter::class.java)

            val field2 = Claim::class.java.getDeclaredField("claimType2")
            assertThat(field2.type).isEqualTo(ClaimType2::class.java)
            val convert2 = field2.getAnnotation(Convert::class.java)
            assertThat(convert2).isNotNull
            assertThat(convert2.converter.java).isEqualTo(ClaimType2Converter::class.java)
        }
    }

    @Nested
    @DisplayName("AC6 — ClaimChannel enum 신규")
    inner class ClaimChannelEnum {

        @Test
        @DisplayName("ClaimChannel.values() = {CRM, CAP, WEB} (#829 web admin 신설)")
        fun claimChannelMembers() {
            assertThat(ClaimChannel.entries).hasSize(3)
            assertThat(ClaimChannel.entries.map { it.name })
                .containsExactlyInAnyOrder("CRM", "CAP", "WEB")
        }

        @Test
        @DisplayName("ClaimChannelConverter — enum ↔ DB String 양방향")
        fun converter() {
            val converter = ClaimChannelConverter()
            assertThat(converter.convertToDatabaseColumn(ClaimChannel.CRM)).isEqualTo("CRM")
            assertThat(converter.convertToDatabaseColumn(ClaimChannel.CAP)).isEqualTo("CAP")
            assertThat(converter.convertToDatabaseColumn(null)).isNull()
            assertThat(converter.convertToEntityAttribute("CRM")).isEqualTo(ClaimChannel.CRM)
            assertThat(converter.convertToEntityAttribute("CAP")).isEqualTo(ClaimChannel.CAP)
            assertThat(converter.convertToEntityAttribute(null)).isNull()
            assertThat(converter.convertToEntityAttribute("UNKNOWN")).isNull()
        }

        @Test
        @DisplayName("Claim.channel 필드 타입 = ClaimChannel? + @Convert(ClaimChannelConverter)")
        fun channelFieldHasConvert() {
            val field = Claim::class.java.getDeclaredField("channel")
            assertThat(field.type).isEqualTo(ClaimChannel::class.java)
            val convert = field.getAnnotation(Convert::class.java)
            assertThat(convert).isNotNull
            assertThat(convert.converter.java).isEqualTo(ClaimChannelConverter::class.java)
        }
    }
}
