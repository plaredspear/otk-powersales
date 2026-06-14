package com.otoki.powersales.suggestion.entity

import com.otoki.powersales.platform.common.salesforce.HCColumn
import com.otoki.powersales.platform.common.salesforce.HerokuOnly
import com.otoki.powersales.platform.common.salesforce.SFField
import com.otoki.powersales.platform.common.salesforce.SFObject
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/**
 * Spec #664 P1-B — Suggestion ↔ Salesforce `DKRetail__Proposal__c` 어노테이션 부착 검증.
 *
 * 단일 권위: SF Object 메타 (`DKRetail__Proposal__c` — sandbox retrieve 41 field xml).
 *
 * 검증 분류:
 *   - AC1: 클래스 `@SFObject("DKRetail__Proposal__c")` 부착
 *   - AC2: 활성 컬럼 `@SFField` 부착 (P1-B §2.2 매핑 표 ✅ 컬럼)
 *   - AC3: **SF 매핑 entity 의 Heroku 마커 부재 가드** — `@HerokuOnly` / `@HCColumn` 부재 verify
 *     (`backend-conventions.md` §"Heroku Connect 어노테이션 정책")
 */
@DisplayName("Suggestion SF 어노테이션 검증 (Spec #664)")
class SuggestionSFAnnotationTest {

    @Nested
    @DisplayName("AC1 — 클래스 @SFObject 부착")
    inner class ClassAnnotation {

        @Test
        @DisplayName("@SFObject 값은 'DKRetail__Proposal__c'")
        fun sfObjectValue() {
            val annotation = Suggestion::class.java.getAnnotation(SFObject::class.java)
            assertThat(annotation).isNotNull
            assertThat(annotation.value).isEqualTo("DKRetail__Proposal__c")
        }
    }

    @Nested
    @DisplayName("AC3 — SF 매핑 entity 의 Heroku 마커 부재 가드")
    inner class NoHerokuAnnotations {

        @Test
        @DisplayName("클래스에 @HerokuOnly 부착 금지")
        fun classNoHerokuOnly() {
            val annotation = Suggestion::class.java.getAnnotation(HerokuOnly::class.java)
            assertThat(annotation)
                .withFailMessage("SF 매핑 entity Suggestion 에 @HerokuOnly 부착은 정책 위반 (backend-conventions.md §\"Heroku Connect 어노테이션 정책\")")
                .isNull()
        }

        @Test
        @DisplayName("모든 필드에 @HCColumn 부착 금지")
        fun fieldsNoHCColumn() {
            val violations = Suggestion::class.java.declaredFields
                .filter { it.isAnnotationPresent(HCColumn::class.java) }
                .map { it.name }

            assertThat(violations)
                .withFailMessage(
                    "SF 매핑 entity Suggestion 의 다음 필드가 @HCColumn 부착 — 정책 위반: %s",
                    violations.joinToString(", ")
                )
                .isEmpty()
        }
    }

    @Nested
    @DisplayName("AC2 — 활성 컬럼 @SFField 부착 (P1-B §2.2 매핑 표)")
    inner class ActiveFieldsSFField {

        private fun sfFieldOf(fieldName: String): String? {
            val f = Suggestion::class.java.declaredFields.firstOrNull { it.name == fieldName }
            return f?.getAnnotation(SFField::class.java)?.value
        }

        @Test
        @DisplayName("proposal_number ↔ Name")
        fun proposalNumber() {
            assertThat(sfFieldOf("proposalNumber")).isEqualTo("Name")
        }

        @Test
        @DisplayName("title ↔ DKRetail__Title__c")
        fun title() {
            assertThat(sfFieldOf("title")).isEqualTo("DKRetail__Title__c")
        }

        @Test
        @DisplayName("content ↔ DKRetail__Description__c")
        fun content() {
            assertThat(sfFieldOf("content")).isEqualTo("DKRetail__Description__c")
        }

        @Test
        @DisplayName("category ↔ Category__c (3값 picklist — Q6 옵션 1)")
        fun category() {
            assertThat(sfFieldOf("category")).isEqualTo("Category__c")
        }

        @Test
        @DisplayName("Spec #849 — dk_category ↔ DKRetail__Category__c (deprecated Text 40 부활)")
        fun dkCategoryRevived() {
            assertThat(sfFieldOf("dkCategory")).isEqualTo("DKRetail__Category__c")
        }

        @Test
        @DisplayName("type ↔ DKRetail__Type__c (제안유형 Text 40 — SF 정합 누락 필드 추가)")
        fun type() {
            assertThat(sfFieldOf("type")).isEqualTo("DKRetail__Type__c")
        }

        @Test
        @DisplayName("claim_type / claim_type_measures 양쪽 보존 (Q9 옵션 1)")
        fun claimTypeAndMeasures() {
            assertThat(sfFieldOf("claimType")).isEqualTo("ClaimType__c")
            assertThat(sfFieldOf("claimTypeMeasures")).isEqualTo("ClaimTypeMeasures__c")
        }

        @Test
        @DisplayName("WERK1_TEXT2 / WERK3_TEXT2 (Q3 옵션 B — 레거시 동등 재현)")
        fun werkTexts() {
            assertThat(sfFieldOf("receptionLogisticsCenter")).isEqualTo("WERK1_TEXT2__c")
            assertThat(sfFieldOf("responsibleLogisticsCenter")).isEqualTo("WERK3_TEXT2__c")
        }

        @Test
        @DisplayName("action_status / duplicate_proposal_num — Q10 옵션 1 (BR3 의존)")
        fun actionStatusAndDuplicate() {
            assertThat(sfFieldOf("actionStatus")).isEqualTo("ActionStatus__c")
            assertThat(sfFieldOf("duplicateProposalNum")).isEqualTo("DuplicateProposalNum__c")
        }
    }

    @Nested
    @DisplayName("SuggestionCategory enum SF picklist 3값 정합")
    inner class CategoryEnum {

        @Test
        @DisplayName("3값 enum + 한글 displayName 매핑")
        fun threeValuesEnum() {
            assertThat(SuggestionCategory.entries).hasSize(3)
            assertThat(SuggestionCategory.NEW_PRODUCT.displayName).isEqualTo("신제품 제안")
            assertThat(SuggestionCategory.EXISTING_PRODUCT.displayName).isEqualTo("기존제품 상품가치 향상")
            assertThat(SuggestionCategory.LOGISTICS_CLAIM.displayName).isEqualTo("물류 클레임")
        }

        @Test
        @DisplayName("fromDisplayNameOrNull — 한글값 ↔ enum 양방향")
        fun fromDisplayName() {
            assertThat(SuggestionCategory.fromDisplayNameOrNull("물류 클레임"))
                .isEqualTo(SuggestionCategory.LOGISTICS_CLAIM)
            assertThat(SuggestionCategory.fromDisplayNameOrNull(null)).isNull()
            assertThat(SuggestionCategory.fromDisplayNameOrNull("미정의값")).isNull()
        }
    }

    @Nested
    @DisplayName("SuggestionActionStatus enum SF picklist 4값 정합 (BR3 의존)")
    inner class ActionStatusEnum {

        @Test
        @DisplayName("4값 enum + 한글 displayName 매핑")
        fun fourValuesEnum() {
            assertThat(SuggestionActionStatus.entries).hasSize(4)
            assertThat(SuggestionActionStatus.UNCONFIRMED.displayName).isEqualTo("미확인")
            assertThat(SuggestionActionStatus.IN_PROGRESS.displayName).isEqualTo("조치중")
            assertThat(SuggestionActionStatus.COMPLETED.displayName).isEqualTo("조치 완료")
            assertThat(SuggestionActionStatus.DUPLICATE_RECEPTION.displayName).isEqualTo("중복접수")
        }
    }
}
