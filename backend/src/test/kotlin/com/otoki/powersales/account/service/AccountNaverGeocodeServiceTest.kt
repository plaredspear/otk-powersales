package com.otoki.powersales.account.service

import com.otoki.powersales.account.entity.Account
import com.otoki.powersales.account.repository.AccountRepository
import com.otoki.powersales.common.naver.NaverGeocodeClient
import com.otoki.powersales.common.naver.NaverGeocodeResponse
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.util.Optional

@ExtendWith(MockitoExtension::class)
@DisplayName("AccountNaverGeocodeService 테스트 (#637)")
class AccountNaverGeocodeServiceTest {

    @Mock
    private lateinit var accountRepository: AccountRepository

    @Mock
    private lateinit var naverGeocodeClient: NaverGeocodeClient

    @InjectMocks
    private lateinit var service: AccountNaverGeocodeService

    @Nested
    @DisplayName("enrichSingleAccount — 거래처 1건 좌표 보강")
    inner class EnrichSingleAccountTests {

        @Test
        @DisplayName("정상 — 응답 x/y → longitude/latitude set + true 반환")
        fun enrichSingleAccount_success() {
            // Given
            val account = createAccount(id = 10, address1 = "부산시 해운대구")
            whenever(accountRepository.findById(10)).thenReturn(Optional.of(account))
            whenever(naverGeocodeClient.geocode("부산시 해운대구")).thenReturn(
                NaverGeocodeResponse(addresses = listOf(NaverGeocodeResponse.Address(x = "129.158", y = "35.163")))
            )

            // When
            val ok = service.enrichSingleAccount(10)

            // Then
            assertThat(ok).isTrue
            assertThat(account.longitude).isEqualTo("129.158")
            assertThat(account.latitude).isEqualTo("35.163")
        }

        @Test
        @DisplayName("Account 미존재 — false 반환 + Naver 호출 없음")
        fun enrichSingleAccount_accountNotFound() {
            whenever(accountRepository.findById(99)).thenReturn(Optional.empty())

            val ok = service.enrichSingleAccount(99)

            assertThat(ok).isFalse
            verify(naverGeocodeClient, never()).geocode(any())
        }

        @Test
        @DisplayName("address1 null — false 반환 + Naver 호출 없음")
        fun enrichSingleAccount_address1Null() {
            val account = createAccount(id = 11, address1 = null)
            whenever(accountRepository.findById(11)).thenReturn(Optional.of(account))

            val ok = service.enrichSingleAccount(11)

            assertThat(ok).isFalse
            verify(naverGeocodeClient, never()).geocode(any())
        }

        @Test
        @DisplayName("Naver 응답 null (호출 실패) — false 반환 + 좌표 미변경")
        fun enrichSingleAccount_naverReturnsNull() {
            val account = createAccount(id = 12, address1 = "부산시 사상구", latitude = null, longitude = null)
            whenever(accountRepository.findById(12)).thenReturn(Optional.of(account))
            whenever(naverGeocodeClient.geocode("부산시 사상구")).thenReturn(null)

            val ok = service.enrichSingleAccount(12)

            assertThat(ok).isFalse
            assertThat(account.latitude).isNull()
            assertThat(account.longitude).isNull()
        }

        @Test
        @DisplayName("응답 addresses 빈 배열 — false 반환 + 좌표 미변경")
        fun enrichSingleAccount_addressesEmpty() {
            val account = createAccount(id = 13, address1 = "유효하지 않은 주소", latitude = null, longitude = null)
            whenever(accountRepository.findById(13)).thenReturn(Optional.of(account))
            whenever(naverGeocodeClient.geocode("유효하지 않은 주소")).thenReturn(
                NaverGeocodeResponse(addresses = emptyList())
            )

            val ok = service.enrichSingleAccount(13)

            assertThat(ok).isFalse
            assertThat(account.latitude).isNull()
            assertThat(account.longitude).isNull()
        }

        @Test
        @DisplayName("응답 x/y null — false 반환 + 좌표 미변경")
        fun enrichSingleAccount_xyNull() {
            val account = createAccount(id = 14, address1 = "주소", latitude = null, longitude = null)
            whenever(accountRepository.findById(14)).thenReturn(Optional.of(account))
            whenever(naverGeocodeClient.geocode("주소")).thenReturn(
                NaverGeocodeResponse(addresses = listOf(NaverGeocodeResponse.Address(x = null, y = null)))
            )

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
            // Given
            val a1 = createAccount(id = 1, address1 = "주소1")
            val a2 = createAccount(id = 2, address1 = "주소2")
            val a3 = createAccount(id = 3, address1 = "주소3")
            whenever(accountRepository.findCoordinatesMissingAccounts(eq(1000))).thenReturn(listOf(a1, a2, a3))
            whenever(accountRepository.findById(1)).thenReturn(Optional.of(a1))
            whenever(accountRepository.findById(2)).thenReturn(Optional.of(a2))
            whenever(accountRepository.findById(3)).thenReturn(Optional.of(a3))
            whenever(naverGeocodeClient.geocode("주소1")).thenReturn(
                NaverGeocodeResponse(listOf(NaverGeocodeResponse.Address("127.1", "37.5")))
            )
            whenever(naverGeocodeClient.geocode("주소2")).thenReturn(null)
            whenever(naverGeocodeClient.geocode("주소3")).thenReturn(
                NaverGeocodeResponse(listOf(NaverGeocodeResponse.Address("127.2", "37.6")))
            )

            // When
            val result = service.enrichCoordinatesMissingAccounts(1000)

            // Then
            assertThat(result.scanned).isEqualTo(3)
            assertThat(result.succeeded).isEqualTo(2)
            assertThat(result.failed).isEqualTo(1)
            verify(naverGeocodeClient, times(3)).geocode(any())
        }

        @Test
        @DisplayName("매칭 거래처 0건 — Naver 호출 없음 + 모든 카운트 0")
        fun enrichBatch_emptyCandidates() {
            whenever(accountRepository.findCoordinatesMissingAccounts(eq(1000))).thenReturn(emptyList())

            val result = service.enrichCoordinatesMissingAccounts(1000)

            assertThat(result.scanned).isEqualTo(0)
            assertThat(result.succeeded).isEqualTo(0)
            assertThat(result.failed).isEqualTo(0)
            verify(naverGeocodeClient, never()).geocode(any())
        }

        @Test
        @DisplayName("거래처 보강 중 예외 발생 — 다음 거래처 진행 + failed 카운트 증가")
        fun enrichBatch_exceptionHandled() {
            val a1 = createAccount(id = 1, address1 = "주소1")
            val a2 = createAccount(id = 2, address1 = "주소2")
            whenever(accountRepository.findCoordinatesMissingAccounts(eq(1000))).thenReturn(listOf(a1, a2))
            whenever(accountRepository.findById(1)).thenThrow(RuntimeException("DB 일시 오류"))
            whenever(accountRepository.findById(2)).thenReturn(Optional.of(a2))
            whenever(naverGeocodeClient.geocode("주소2")).thenReturn(
                NaverGeocodeResponse(listOf(NaverGeocodeResponse.Address("127.2", "37.6")))
            )

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
