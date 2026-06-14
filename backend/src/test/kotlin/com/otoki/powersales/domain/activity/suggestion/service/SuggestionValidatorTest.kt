package com.otoki.powersales.domain.activity.suggestion.service

import com.otoki.powersales.domain.activity.suggestion.service.SuggestionValidator
import com.otoki.powersales.domain.activity.suggestion.entity.SuggestionActionStatus
import com.otoki.powersales.domain.activity.suggestion.entity.SuggestionCategory
import com.otoki.powersales.domain.activity.suggestion.exception.SuggestionValidationException
import org.assertj.core.api.Assertions.assertThatCode
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDate

/**
 * Spec #664 P2-B §2.4 — BR1~BR7 Category 분기 검증 룰 unit test.
 *
 * 각 BR 별 별 테스트 메서드 + 메시지 문구까지 검증 (`@DisplayName` 한글 표기).
 */
@DisplayName("SuggestionValidator — BR1~BR7 Category 분기 검증")
class SuggestionValidatorTest {

    private val validator = SuggestionValidator()

    @Nested
    @DisplayName("LOGISTICS_CLAIM 카테고리")
    inner class LogisticsClaim {

        @Test
        @DisplayName("BR1 — claim_type 누락 시 예외")
        fun br1_claimTypeRequired() {
            assertThatThrownBy {
                validator.validate(
                    category = SuggestionCategory.LOGISTICS_CLAIM,
                    claimType = null,
                    claimDate = LocalDate.now(),
                    carNumber = null,
                    duplicateProposalNum = null,
                    actionStatus = null
                )
            }
                .isInstanceOf(SuggestionValidationException::class.java)
                .hasMessage("제안구분이 물류 클레임일 경우 클레임 항목을 기입하셔야 합니다.")
        }

        @Test
        @DisplayName("BR2 — claim_date 누락 시 예외")
        fun br2_claimDateRequired() {
            assertThatThrownBy {
                validator.validate(
                    category = SuggestionCategory.LOGISTICS_CLAIM,
                    claimType = "포장불량",
                    claimDate = null,
                    carNumber = null,
                    duplicateProposalNum = null,
                    actionStatus = null
                )
            }
                .isInstanceOf(SuggestionValidationException::class.java)
                .hasMessage("제안구분이 물류 클레임일 경우 물류 클레임 발생일자를 기입하셔야 합니다.")
        }

        @Test
        @DisplayName("BR3 — actionStatus=중복접수 시 duplicate_proposal_num 누락 시 예외")
        fun br3_duplicateProposalNumRequired() {
            assertThatThrownBy {
                validator.validate(
                    category = SuggestionCategory.LOGISTICS_CLAIM,
                    claimType = "포장불량",
                    claimDate = LocalDate.now(),
                    carNumber = null,
                    duplicateProposalNum = null,
                    actionStatus = SuggestionActionStatus.DUPLICATE_RECEPTION
                )
            }
                .isInstanceOf(SuggestionValidationException::class.java)
                .hasMessage("제안구분이 물류 클레임이면서 중복접수를 선택하셨을경우 중복 제안번호를 기입하셔야 합니다.")
        }

        @Test
        @DisplayName("golden positive — 모든 필수 충족 시 통과")
        fun goldenPositive() {
            assertThatCode {
                validator.validate(
                    category = SuggestionCategory.LOGISTICS_CLAIM,
                    claimType = "포장불량",
                    claimDate = LocalDate.now(),
                    carNumber = "12가1234",
                    duplicateProposalNum = "DUP-001",
                    actionStatus = SuggestionActionStatus.DUPLICATE_RECEPTION
                )
            }.doesNotThrowAnyException()
        }
    }

    @Nested
    @DisplayName("NEW_PRODUCT / EXISTING_PRODUCT 카테고리")
    inner class NonLogisticsClaim {

        @Test
        @DisplayName("BR4 — claim_type 입력 시 예외")
        fun br4_claimTypeForbidden() {
            assertThatThrownBy {
                validator.validate(
                    category = SuggestionCategory.NEW_PRODUCT,
                    claimType = "포장불량",
                    claimDate = null,
                    carNumber = null,
                    duplicateProposalNum = null,
                    actionStatus = null
                )
            }
                .isInstanceOf(SuggestionValidationException::class.java)
                .hasMessage("제안구분이 물류 클레임이 아닐 경우 클레임 항목을 기입할 수 없습니다.")
        }

        @Test
        @DisplayName("BR5 — claim_date 입력 시 예외")
        fun br5_claimDateForbidden() {
            assertThatThrownBy {
                validator.validate(
                    category = SuggestionCategory.EXISTING_PRODUCT,
                    claimType = null,
                    claimDate = LocalDate.now(),
                    carNumber = null,
                    duplicateProposalNum = null,
                    actionStatus = null
                )
            }
                .isInstanceOf(SuggestionValidationException::class.java)
                .hasMessage("제안구분이 물류 클레임이 아닐 경우 물류 클레임 발생일자를 기입할 수 없습니다.")
        }

        @Test
        @DisplayName("BR6 — car_number 입력 시 예외")
        fun br6_carNumberForbidden() {
            assertThatThrownBy {
                validator.validate(
                    category = SuggestionCategory.NEW_PRODUCT,
                    claimType = null,
                    claimDate = null,
                    carNumber = "12가1234",
                    duplicateProposalNum = null,
                    actionStatus = null
                )
            }
                .isInstanceOf(SuggestionValidationException::class.java)
                .hasMessage("제안구분이 물류 클레임이 아닐 경우 물류 차량번호를 기입할 수 없습니다.")
        }

        @Test
        @DisplayName("BR7 — duplicate_proposal_num 입력 시 예외")
        fun br7_duplicateProposalNumForbidden() {
            assertThatThrownBy {
                validator.validate(
                    category = SuggestionCategory.EXISTING_PRODUCT,
                    claimType = null,
                    claimDate = null,
                    carNumber = null,
                    duplicateProposalNum = "DUP-001",
                    actionStatus = null
                )
            }
                .isInstanceOf(SuggestionValidationException::class.java)
                .hasMessage("제안구분이 물류 클레임이 아닐 경우 중복 제안번호를 기입할 수 없습니다.")
        }

        @Test
        @DisplayName("golden positive — 클레임 관련 필드 모두 null 이면 통과")
        fun goldenPositive() {
            assertThatCode {
                validator.validate(
                    category = SuggestionCategory.NEW_PRODUCT,
                    claimType = null,
                    claimDate = null,
                    carNumber = null,
                    duplicateProposalNum = null,
                    actionStatus = null
                )
            }.doesNotThrowAnyException()
        }
    }
}
