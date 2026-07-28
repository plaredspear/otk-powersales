package com.otoki.powersales.domain.foundation.account.service

import com.otoki.powersales.domain.foundation.account.dto.response.DistributionChannelOption
import com.otoki.powersales.domain.foundation.account.entity.AccountCategoryMaster
import com.otoki.powersales.domain.foundation.account.repository.AccountCategoryMasterRepository
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

/**
 * 유통형태 정본 — 거래처유형마스터 `"{거래처유형코드} {이름}"`.
 *
 * 운영 마스터 실값 일부를 픽스처로 쓴다 (01 대형마트(3대) / 02 체인 / 06 슈퍼 / 13 외식).
 * 과거 구현이 쓰던 `거래처상태코드 + 거래처유형명` 조합과 어긋난다는 점이 회귀 방지의 핵심 —
 * 상태코드는 01/02/03 세 값뿐이라 "02 슈퍼" 같은 잘못된 라벨을 만들었다.
 */
@DisplayName("AccountCategoryLookup — 유통형태 라벨/코드 해소")
class AccountCategoryLookupTest {

    private val repository = mockk<AccountCategoryMasterRepository>()
    private val lookup = AccountCategoryLookup(AccountCategoryCatalog(repository))

    private fun master(code: String?, name: String?, deleted: Boolean? = false) =
        AccountCategoryMaster(accountCode = code, name = name, isDeleted = deleted)

    private fun givenMasters(vararg rows: AccountCategoryMaster) {
        every { repository.findAll() } returns rows.toList()
    }

    @Test
    @DisplayName("거래처유형명(Account.accountType)으로 \"{코드} {이름}\" 라벨을 해소한다")
    fun resolvesLabelByAccountTypeName() {
        givenMasters(master("06", "슈퍼"), master("02", "체인"))

        val directory = lookup.directory()

        assertThat(directory.label("슈퍼")).isEqualTo("06 슈퍼")
        assertThat(directory.label("체인")).isEqualTo("02 체인")
    }

    @Test
    @DisplayName("유형 미지정/마스터 미등록 거래처는 라벨이 null (표시 폴백은 호출 측 책임)")
    fun unknownTypeReturnsNull() {
        givenMasters(master("06", "슈퍼"))

        val directory = lookup.directory()

        assertThat(directory.label(null)).isNull()
        assertThat(directory.label("  ")).isNull()
        assertThat(directory.label("존재하지않는유형")).isNull()
    }

    @Test
    @DisplayName("옵션은 코드 오름차순이며 use_search 로 걸러내지 않는다 (외식 등도 조회 가능해야 함)")
    fun optionsSortedByCodeWithoutUseSearchGate() {
        givenMasters(master("13", "외식"), master("01", "대형마트(3대)"), master("06", "슈퍼"))

        assertThat(lookup.options()).containsExactly(
            DistributionChannelOption("01", "01 대형마트(3대)"),
            DistributionChannelOption("06", "06 슈퍼"),
            DistributionChannelOption("13", "13 외식"),
        )
    }

    @Test
    @DisplayName("선택 코드는 Account.accountType 매칭용 이름으로 되돌린다 (미등록 코드는 무시)")
    fun namesOfResolvesSelectedCodes() {
        givenMasters(master("06", "슈퍼"), master("02", "체인"))

        assertThat(lookup.namesOf(listOf("06", "02", "99"))).containsExactly("슈퍼", "체인")
    }

    @Test
    @DisplayName("삭제되었거나 코드·이름이 비어 있는 마스터 행은 옵션/라벨에서 제외")
    fun skipsDeletedOrIncompleteRows() {
        givenMasters(
            master("06", "슈퍼"),
            master("07", "대리점", deleted = true),
            master(null, "코드없음"),
            master("08", null),
        )

        val directory = lookup.directory()

        assertThat(lookup.options().map { it.code }).containsExactly("06")
        assertThat(directory.label("대리점")).isNull()
        assertThat(directory.label("코드없음")).isNull()
    }
}
