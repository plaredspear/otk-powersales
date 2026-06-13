package com.otoki.powersales.platform.auth.sharing.service

import com.otoki.powersales.platform.auth.sharing.service.SharingRulePolicyEvaluator
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/**
 * SharingRulePolicyEvaluator.sfApiNameToJpaProperty 의 corner case 변환 검증 (spec #786 P1-B).
 *
 * SF API name → JPA entity property 변환 매핑은 모든 sharing rule evaluation 의 진입 경로.
 * Hibernate runtime path resolution 실패 시 회귀가 발생하므로 5 종류 corner case 박제.
 *
 * 검증 도구: 순수 Kotlin String 변환 — DB / QueryDSL 무관, MockK 사용 불필요.
 *
 * (기존 SharingRulePolicyEvaluatorTest 의 SfApiNameMapping nested 5 case 와 보완 — 본 테스트는
 * SF managed namespace / 짧은 이름 / audit 컬럼 / lookup id / 일반 standard 컬럼 5 종 추가.)
 */
@DisplayName("SharingRulePolicyEvaluator — sfApiNameToJpaProperty corner case (spec #786)")
class SfApiNameToJpaPropertyTest {

    private val evaluator = SharingRulePolicyEvaluator(mockk(relaxed = true))

    @Nested
    @DisplayName("SF managed namespace 패턴 — DKRetail__*__c")
    inner class ManagedNamespace {

        @Test
        @DisplayName("DKRetail__Promotion__c → 첫 토큰 + 나머지 PascalCase")
        fun promotion() {
            // DKRetail prefix + Promotion 본문 — underscore split 후 첫 글자 lowercase + 나머지 PascalCase 합성.
            val result = evaluator.sfApiNameToJpaProperty("DKRetail__Promotion__c")
            // 일반 entity 의 JPA property 와 직접 일치하지 않을 수 있으나 변환 자체는 deterministic — 박제 대상.
            assertThat(result).isNotEmpty
            // double underscore (`__`) 가 단일 underscore 와 동일 split 동작 → empty token 발생 가능 박제.
            // 실측 동작에 의존 — 본 케이스가 변환 결과에 invariant 를 박제.
            assertThat(result).isEqualTo("dKRetailPromotion")
        }

        @Test
        @DisplayName("DKRetail__Status__c — picklist value 류 컬럼")
        fun status() {
            val result = evaluator.sfApiNameToJpaProperty("DKRetail__Status__c")
            assertThat(result).isEqualTo("dKRetailStatus")
        }
    }

    @Nested
    @DisplayName("SF audit / 표준 컬럼 (custom suffix 없음)")
    inner class StandardColumns {

        @Test
        @DisplayName("OwnerId → ownerId — 표준 lookup id (suffix 없음)")
        fun ownerId() {
            assertThat(evaluator.sfApiNameToJpaProperty("OwnerId")).isEqualTo("ownerId")
        }

        @Test
        @DisplayName("LastModifiedById → lastModifiedById — audit 컬럼")
        fun lastModifiedById() {
            assertThat(evaluator.sfApiNameToJpaProperty("LastModifiedById")).isEqualTo("lastModifiedById")
        }

        @Test
        @DisplayName("Sic → sic — 짧은 standard 컬럼 (Account.Sic)")
        fun sic() {
            assertThat(evaluator.sfApiNameToJpaProperty("Sic")).isEqualTo("sic")
        }
    }

    @Nested
    @DisplayName("값 변환 invariant — 1글자 / 빈 입력 edge")
    inner class EdgeCases {

        @Test
        @DisplayName("A → a — 단일 PascalCase 글자")
        fun singleChar() {
            assertThat(evaluator.sfApiNameToJpaProperty("A")).isEqualTo("a")
        }

        @Test
        @DisplayName("Email__c → email — 표준 도메인 컬럼명")
        fun email() {
            assertThat(evaluator.sfApiNameToJpaProperty("Email__c")).isEqualTo("email")
        }
    }
}
