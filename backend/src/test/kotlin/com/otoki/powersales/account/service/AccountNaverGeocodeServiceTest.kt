package com.otoki.powersales.account.service

import com.otoki.powersales.account.entity.Account
import com.otoki.powersales.account.repository.AccountRepository
import com.otoki.powersales.common.naver.NaverGeocodeClient
import com.otoki.powersales.common.naver.NaverGeocodeResponse
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.util.Optional

@DisplayName("AccountNaverGeocodeService 테스트 (#637)")
class AccountNaverGeocodeServiceTest {

    private val accountRepository: AccountRepository = mockk()
    private val naverGeocodeClient: NaverGeocodeClient = mockk()

    private val service = AccountNaverGeocodeService(
        accountRepository,
        naverGeocodeClient,
    )

    @Nested
    @DisplayName("enrichSingleAccount — 거래처 1건 좌표 보강")
    inner class EnrichSingleAccountTests {

        @Test
        @DisplayName("정상 — 응답 x/y → longitude/latitude set + true 반환")
        fun enrichSingleAccount_success() {
            val account = createAccount(id = 10, address1 = "부산시 해운대구")
            every { accountRepository.findById(10) } returns Optional.of(account)
            every { naverGeocodeClient.geocode("부산시 해운대구") } returns
                NaverGeocodeResponse(addresses = listOf(NaverGeocodeResponse.Address(x = "129.158", y = "35.163")))

            val ok = service.enrichSingleAccount(10)

            assertThat(ok).isTrue
            assertThat(account.longitude).isEqualTo("129.158")
            assertThat(account.latitude).isEqualTo("35.163")
        }

        @Test
        @DisplayName("Account 미존재 — false 반환 + Naver 호출 없음")
        fun enrichSingleAccount_accountNotFound() {
            every { accountRepository.findById(99) } returns Optional.empty()

            val ok = service.enrichSingleAccount(99)

            assertThat(ok).isFalse
            verify(exactly = 0) { naverGeocodeClient.geocode(any()) }
        }

        @Test
        @DisplayName("address1 null — false 반환 + Naver 호출 없음")
        fun enrichSingleAccount_address1Null() {
            val account = createAccount(id = 11, address1 = null)
            every { accountRepository.findById(11) } returns Optional.of(account)

            val ok = service.enrichSingleAccount(11)

            assertThat(ok).isFalse
            verify(exactly = 0) { naverGeocodeClient.geocode(any()) }
        }

        @Test
        @DisplayName("Naver 응답 null (호출 실패) — false 반환 + 좌표 미변경")
        fun enrichSingleAccount_naverReturnsNull() {
            val account = createAccount(id = 12, address1 = "부산시 사상구", latitude = null, longitude = null)
            every { accountRepository.findById(12) } returns Optional.of(account)
            every { naverGeocodeClient.geocode("부산시 사상구") } returns null

            val ok = service.enrichSingleAccount(12)

            assertThat(ok).isFalse
            assertThat(account.latitude).isNull()
            assertThat(account.longitude).isNull()
        }

        @Test
        @DisplayName("응답 addresses 빈 배열 — false 반환 + 좌표 미변경")
        fun enrichSingleAccount_addressesEmpty() {
            val account = createAccount(id = 13, address1 = "유효하지 않은 주소", latitude = null, longitude = null)
            every { accountRepository.findById(13) } returns Optional.of(account)
            every { naverGeocodeClient.geocode("유효하지 않은 주소") } returns NaverGeocodeResponse(addresses = emptyList())

            val ok = service.enrichSingleAccount(13)

            assertThat(ok).isFalse
            assertThat(account.latitude).isNull()
            assertThat(account.longitude).isNull()
        }

        @Test
        @DisplayName("응답 x/y null — false 반환 + 좌표 미변경")
        fun enrichSingleAccount_xyNull() {
            val account = createAccount(id = 14, address1 = "주소", latitude = null, longitude = null)
            every { accountRepository.findById(14) } returns Optional.of(account)
            every { naverGeocodeClient.geocode("주소") } returns
                NaverGeocodeResponse(addresses = listOf(NaverGeocodeResponse.Address(x = null, y = null)))

            val ok = service.enrichSingleAccount(14)

            assertThat(ok).isFalse
            assertThat(account.latitude).isNull()
            assertThat(account.longitude).isNull()
        }
    }

    @Nested
    @DisplayName("enrichCoordinatesMissingAccounts — batch 묶음 처리")
    inner class EnrichBatchTests {

        @Test
        @DisplayName("3건 — 2 succeeded / 1 failed (응답 없음)")
        fun enrichBatch_mixedResults() {
            val a1 = createAccount(id = 1, address1 = "주소1")
            val a2 = createAccount(id = 2, address1 = "주소2")
            val a3 = createAccount(id = 3, address1 = "주소3")
            every { accountRepository.findCoordinatesMissingAccounts(1000) } returns listOf(a1, a2, a3)
            every { accountRepository.findById(1) } returns Optional.of(a1)
            every { accountRepository.findById(2) } returns Optional.of(a2)
            every { accountRepository.findById(3) } returns Optional.of(a3)
            every { naverGeocodeClient.geocode("주소1") } returns
                NaverGeocodeResponse(listOf(NaverGeocodeResponse.Address("127.1", "37.5")))
            every { naverGeocodeClient.geocode("주소2") } returns null
            every { naverGeocodeClient.geocode("주소3") } returns
                NaverGeocodeResponse(listOf(NaverGeocodeResponse.Address("127.2", "37.6")))

            val result = service.enrichCoordinatesMissingAccounts(1000)

            assertThat(result.scanned).isEqualTo(3)
            assertThat(result.succeeded).isEqualTo(2)
            assertThat(result.failed).isEqualTo(1)
            verify(exactly = 3) { naverGeocodeClient.geocode(any()) }
        }

        @Test
        @DisplayName("매칭 거래처 0건 — Naver 호출 없음 + 모든 카운트 0")
        fun enrichBatch_emptyCandidates() {
            every { accountRepository.findCoordinatesMissingAccounts(1000) } returns emptyList()

            val result = service.enrichCoordinatesMissingAccounts(1000)

            assertThat(result.scanned).isEqualTo(0)
            assertThat(result.succeeded).isEqualTo(0)
            assertThat(result.failed).isEqualTo(0)
            verify(exactly = 0) { naverGeocodeClient.geocode(any()) }
        }

        @Test
        @DisplayName("거래처 보강 중 예외 발생 — 다음 거래처 진행 + failed 카운트 증가")
        fun enrichBatch_exceptionHandled() {
            val a1 = createAccount(id = 1, address1 = "주소1")
            val a2 = createAccount(id = 2, address1 = "주소2")
            every { accountRepository.findCoordinatesMissingAccounts(1000) } returns listOf(a1, a2)
            every { accountRepository.findById(1) } throws RuntimeException("DB 일시 오류")
            every { accountRepository.findById(2) } returns Optional.of(a2)
            every { naverGeocodeClient.geocode("주소2") } returns
                NaverGeocodeResponse(listOf(NaverGeocodeResponse.Address("127.2", "37.6")))

            val result = service.enrichCoordinatesMissingAccounts(1000)

            assertThat(result.scanned).isEqualTo(2)
            assertThat(result.succeeded).isEqualTo(1)
            assertThat(result.failed).isEqualTo(1)
        }
    }

    private fun createAccount(
        id: Int,
        address1: String? = "주소",
        latitude: String? = null,
        longitude: String? = null
    ): Account = Account(
        id = id,
        externalKey = "EXT-$id",
        address1 = address1,
        latitude = latitude,
        longitude = longitude,
        accountStatusName = "거래"
    )
}
