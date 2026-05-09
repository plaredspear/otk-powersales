package com.otoki.powersales.claim.entity

import com.otoki.powersales.claim.entity.converter.ClaimChannelConverter
import com.otoki.powersales.common.salesforce.SFField
import com.otoki.powersales.common.salesforce.SFObject
import com.otoki.powersales.common.salesforce.SFSchemaUtils
import jakarta.persistence.Convert
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/**
 * Spec #606 — Claim ↔ Salesforce `DKRetail__Claim__c` 어노테이션 부착 검증.
 *
 * 단일 권위: docs/plan/old_source_260408/salesforce_object/클레임(DKRetail__Claim__c).md
 *
 * 검증 분류:
 *   - AC1: 클래스 `@SFObject` 무변경
 *   - AC2: `@SFField` 매핑 키셋 (33개 — 11 기존 OK 단순 컬럼 + 6 SAP inbound + 16 신규)
 *     * 기존 13개 중 FK 2개(category_id, subcategory_id) 는 §2.4/§6.6 정책으로 미부착 → 본 스펙 정합화
 *   - AC3: PK / FK 미부착
 *   - AC5: 기존 enum 정합 검증 (ClaimStatus 만 — 다른 picklist 매핑은 entity 마스터로 후속 분리)
 *   - AC6: ClaimChannel enum 신규 + Converter
 *   - AC8: Picklist md cross-check
 */
@DisplayName("Claim SF 어노테이션 검증 (Spec #606)")
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
        @DisplayName("매핑 키 수 = 33 (11 기존 + 6 SAP inbound + 16 신규)")
        fun mappingKeySize() {
            assertThat(mapping).hasSize(33)
        }

        @Test
        @DisplayName("§6.1 — 기존 OK 11개 매핑 (FK 2개 제외)")
        fun section61Existing() {
            assertThat(mapping["Name"]).isEqualTo("name")
            assertThat(mapping["DKRetail__EmployeeId__c"]).isEqualTo("employee_sfid")
            assertThat(mapping["DKRetail__AccountId__c"]).isEqualTo("account_sfid")
            assertThat(mapping["DKRetail__ProductCode__c"]).isEqualTo("product_code")
            assertThat(mapping["DKRetail__ClaimDate__c"]).isEqualTo("date")
            assertThat(mapping["DKRetail__Description__c"]).isEqualTo("defect_description")
            assertThat(mapping["DKRetail__Quantity__c"]).isEqualTo("defect_quantity")
            assertThat(mapping["DKRetail__Amount__c"]).isEqualTo("purchase_amount")
            assertThat(mapping["DKRetail__PurchaseMethod__c"]).isEqualTo("purchase_method_code")
            assertThat(mapping["DKRetail__RequestType__c"]).isEqualTo("request_type_code")
            assertThat(mapping["DKRetail__Status__c"]).isEqualTo("status")
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
        @DisplayName("FK 필드(category, subcategory, employee, account)에 @SFField 미부착")
        fun fkHasNoSfField() {
            listOf("category", "subcategory", "employee", "account").forEach { name ->
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
                "category_id",
                "subcategory_id",
                "employee_id",
                "store_id",
            )
        }
    }

    @Nested
    @DisplayName("AC5 — 기존 enum 정합 검증 (ClaimStatus)")
    inner class ExistingEnumConsistency {

        @Test
        @DisplayName("ClaimStatus.values() 는 4개 멤버 (SUBMITTED/IN_PROGRESS/RESOLVED/REJECTED)")
        fun claimStatusMembers() {
            // SF md `DKRetail__Status__c` picklist (3개 — 임시저장/전송완료/전송실패) 와는 도메인 차이.
            // 본 스펙 Q1 옵션 1: 정합 검증 단언만, 멤버 변경은 후속 별도 스펙.
            assertThat(ClaimStatus.entries).hasSize(4)
            assertThat(ClaimStatus.entries.map { it.name })
                .containsExactlyInAnyOrder("SUBMITTED", "IN_PROGRESS", "RESOLVED", "REJECTED")
        }
    }

    @Nested
    @DisplayName("AC6 — ClaimChannel enum 신규")
    inner class ClaimChannelEnum {

        @Test
        @DisplayName("ClaimChannel.values() = {CRM, CAP} (2개)")
        fun claimChannelMembers() {
            assertThat(ClaimChannel.entries).hasSize(2)
            assertThat(ClaimChannel.entries.map { it.name })
                .containsExactlyInAnyOrder("CRM", "CAP")
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
