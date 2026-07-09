package com.otoki.powersales.domain.foundation.account.service

import com.otoki.powersales.domain.foundation.account.entity.Account
import com.otoki.powersales.domain.foundation.account.repository.AccountRepository
import com.otoki.powersales.platform.common.naver.NaverGeocodeClient
import com.otoki.powersales.platform.common.naver.NaverGeocodeResponse
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
        @DisplayName("정상 — 응답 x/y → longitude/latitude set + SUCCESS 반환 + 마킹 해제")
        fun enrichSingleAccount_success() {
            val account = createAccount(id = 10, address1 = "부산시 해운대구", geocodeUnresolved = true)
            every { accountRepository.findById(10) } returns Optional.of(account)
            every { naverGeocodeClient.geocode("부산시 해운대구") } returns
                NaverGeocodeResponse(addresses = listOf(NaverGeocodeResponse.Address(x = "129.158", y = "35.163")))

            val result = service.enrichSingleAccount(10)

            assertThat(result).isEqualTo(AccountNaverGeocodeService.GeocodeResult.SUCCESS)
            assertThat(account.longitude).isEqualTo("129.158")
            assertThat(account.latitude).isEqualTo("35.163")
            // 이전 영구 실패 마킹이 성공 시 해제된다.
            assertThat(account.geocodeUnresolved).isNull()
        }

        @Test
        @DisplayName("Account 미존재 — CALL_FAILED 반환 + Naver 호출 없음")
        fun enrichSingleAccount_accountNotFound() {
            every { accountRepository.findById(99) } returns Optional.empty()

            val result = service.enrichSingleAccount(99)

            assertThat(result).isEqualTo(AccountNaverGeocodeService.GeocodeResult.CALL_FAILED)
            verify(exactly = 0) { naverGeocodeClient.geocode(any()) }
        }

        @Test
        @DisplayName("address1 null — ADDRESS_NOT_FOUND(영구 실패 마킹) + Naver 호출 없음")
        fun enrichSingleAccount_address1Null() {
            val account = createAccount(id = 11, address1 = null)
            every { accountRepository.findById(11) } returns Optional.of(account)

            val result = service.enrichSingleAccount(11)

            assertThat(result).isEqualTo(AccountNaverGeocodeService.GeocodeResult.ADDRESS_NOT_FOUND)
            assertThat(account.geocodeUnresolved).isTrue
            verify(exactly = 0) { naverGeocodeClient.geocode(any()) }
        }

        @Test
        @DisplayName("Naver 응답 null (호출 실패) — CALL_FAILED + 좌표·마킹 미변경 (재시도 대상 유지)")
        fun enrichSingleAccount_naverReturnsNull() {
            val account = createAccount(id = 12, address1 = "부산시 사상구", latitude = null, longitude = null)
            every { accountRepository.findById(12) } returns Optional.of(account)
            every { naverGeocodeClient.geocode("부산시 사상구") } returns null

            val result = service.enrichSingleAccount(12)

            assertThat(result).isEqualTo(AccountNaverGeocodeService.GeocodeResult.CALL_FAILED)
            assertThat(account.latitude).isNull()
            assertThat(account.longitude).isNull()
            // 일시 실패는 마킹하지 않는다 — 다음 배치 재시도 대상으로 남긴다.
            assertThat(account.geocodeUnresolved).isNull()
        }

        @Test
        @DisplayName("응답 addresses 빈 배열 — ADDRESS_NOT_FOUND(영구 실패 마킹) + 좌표 미변경")
        fun enrichSingleAccount_addressesEmpty() {
            val account = createAccount(id = 13, address1 = "유효하지 않은 주소", latitude = null, longitude = null)
            every { accountRepository.findById(13) } returns Optional.of(account)
            every { naverGeocodeClient.geocode("유효하지 않은 주소") } returns NaverGeocodeResponse(addresses = emptyList())

            val result = service.enrichSingleAccount(13)

            assertThat(result).isEqualTo(AccountNaverGeocodeService.GeocodeResult.ADDRESS_NOT_FOUND)
            assertThat(account.latitude).isNull()
            assertThat(account.longitude).isNull()
            // 주소로 좌표를 못 찾음 → 영구 실패 마킹 (배치 재조회 제외).
            assertThat(account.geocodeUnresolved).isTrue
        }

        @Test
        @DisplayName("응답 x/y null — ADDRESS_NOT_FOUND(영구 실패 마킹) + 좌표 미변경")
        fun enrichSingleAccount_xyNull() {
            val account = createAccount(id = 14, address1 = "주소", latitude = null, longitude = null)
            every { accountRepository.findById(14) } returns Optional.of(account)
            every { naverGeocodeClient.geocode("주소") } returns
                NaverGeocodeResponse(addresses = listOf(NaverGeocodeResponse.Address(x = null, y = null)))

            val result = service.enrichSingleAccount(14)

            assertThat(result).isEqualTo(AccountNaverGeocodeService.GeocodeResult.ADDRESS_NOT_FOUND)
            assertThat(account.latitude).isNull()
            assertThat(account.longitude).isNull()
            assertThat(account.geocodeUnresolved).isTrue
        }
    }

    @Nested
    @DisplayName("refreshSingleAccount — 주소 변경 후 좌표 즉시 재조회 (실패 시 무효화)")
    inner class RefreshSingleAccountTests {

        @Test
        @DisplayName("정상 — 응답 x/y → longitude/latitude 갱신")
        fun refreshSingleAccount_success() {
            val account = createAccount(
                id = 20, address1 = "서울특별시 강남구 테헤란로 100",
                latitude = "1.0", longitude = "2.0", geocodeUnresolved = true
            )
            every { accountRepository.findById(20) } returns Optional.of(account)
            every { naverGeocodeClient.geocode("서울특별시 강남구 테헤란로 100") } returns
                NaverGeocodeResponse(addresses = listOf(NaverGeocodeResponse.Address(x = "127.0276", y = "37.4979")))

            service.refreshSingleAccount(20)

            assertThat(account.longitude).isEqualTo("127.0276")
            assertThat(account.latitude).isEqualTo("37.4979")
            // 성공 시 이전 영구 실패 마킹 해제.
            assertThat(account.geocodeUnresolved).isNull()
        }

        @Test
        @DisplayName("Account 미존재 — Naver 호출 없음 (no-op)")
        fun refreshSingleAccount_accountNotFound() {
            every { accountRepository.findById(99) } returns Optional.empty()

            service.refreshSingleAccount(99)

            verify(exactly = 0) { naverGeocodeClient.geocode(any()) }
        }

        @Test
        @DisplayName("address1 null/blank — Naver 호출 없음 + 좌표 null 무효화")
        fun refreshSingleAccount_addressBlank_coordsNulled() {
            val account = createAccount(id = 21, address1 = "  ", latitude = "1.0", longitude = "2.0")
            every { accountRepository.findById(21) } returns Optional.of(account)

            service.refreshSingleAccount(21)

            assertThat(account.latitude).isNull()
            assertThat(account.longitude).isNull()
            verify(exactly = 0) { naverGeocodeClient.geocode(any()) }
        }

        @Test
        @DisplayName("Naver 응답 null (호출 실패) — 좌표 null 무효화 (배치 fallback)")
        fun refreshSingleAccount_naverReturnsNull_coordsNulled() {
            val account = createAccount(id = 22, address1 = "부산시 사상구", latitude = "1.0", longitude = "2.0")
            every { accountRepository.findById(22) } returns Optional.of(account)
            every { naverGeocodeClient.geocode("부산시 사상구") } returns null

            service.refreshSingleAccount(22)

            assertThat(account.latitude).isNull()
            assertThat(account.longitude).isNull()
        }

        @Test
        @DisplayName("응답 x/y blank — 좌표 null 무효화")
        fun refreshSingleAccount_xyBlank_coordsNulled() {
            val account = createAccount(id = 23, address1 = "주소", latitude = "1.0", longitude = "2.0")
            every { accountRepository.findById(23) } returns Optional.of(account)
            every { naverGeocodeClient.geocode("주소") } returns
                NaverGeocodeResponse(addresses = listOf(NaverGeocodeResponse.Address(x = "", y = "")))

            service.refreshSingleAccount(23)

            assertThat(account.latitude).isNull()
            assertThat(account.longitude).isNull()
        }
    }

    @Nested
    @DisplayName("enrichCoordinatesMissingAccounts — batch 묶음 처리")
    inner class EnrichBatchTests {

        @Test
        @DisplayName("4건 — 2 succeeded / 1 callFailed(응답 없음) / 1 unresolved(주소 못 찾음)")
        fun enrichBatch_mixedResults() {
            val a1 = createAccount(id = 1, address1 = "주소1")
            val a2 = createAccount(id = 2, address1 = "주소2")
            val a3 = createAccount(id = 3, address1 = "주소3")
            val a4 = createAccount(id = 4, address1 = "찾을 수 없는 주소")
            every { accountRepository.findCoordinatesMissingAccounts(1000) } returns listOf(a1, a2, a3, a4)
            every { accountRepository.findById(1) } returns Optional.of(a1)
            every { accountRepository.findById(2) } returns Optional.of(a2)
            every { accountRepository.findById(3) } returns Optional.of(a3)
            every { accountRepository.findById(4) } returns Optional.of(a4)
            every { naverGeocodeClient.geocode("주소1") } returns
                NaverGeocodeResponse(listOf(NaverGeocodeResponse.Address("127.1", "37.5")))
            every { naverGeocodeClient.geocode("주소2") } returns null
            every { naverGeocodeClient.geocode("주소3") } returns
                NaverGeocodeResponse(listOf(NaverGeocodeResponse.Address("127.2", "37.6")))
            every { naverGeocodeClient.geocode("찾을 수 없는 주소") } returns NaverGeocodeResponse(addresses = emptyList())

            val result = service.enrichCoordinatesMissingAccounts(1000)

            assertThat(result.scanned).isEqualTo(4)
            assertThat(result.succeeded).isEqualTo(2)
            assertThat(result.callFailed).isEqualTo(1)
            assertThat(result.unresolved).isEqualTo(1)
            // 주소 못 찾은 a4 만 영구 실패 마킹, 호출 실패한 a2 는 마킹 안 함.
            assertThat(a4.geocodeUnresolved).isTrue
            assertThat(a2.geocodeUnresolved).isNull()
            verify(exactly = 4) { naverGeocodeClient.geocode(any()) }
        }

        @Test
        @DisplayName("매칭 거래처 0건 — Naver 호출 없음 + 모든 카운트 0")
        fun enrichBatch_emptyCandidates() {
            every { accountRepository.findCoordinatesMissingAccounts(1000) } returns emptyList()

            val result = service.enrichCoordinatesMissingAccounts(1000)

            assertThat(result.scanned).isEqualTo(0)
            assertThat(result.succeeded).isEqualTo(0)
            assertThat(result.unresolved).isEqualTo(0)
            assertThat(result.callFailed).isEqualTo(0)
            verify(exactly = 0) { naverGeocodeClient.geocode(any()) }
        }

        @Test
        @DisplayName("거래처 보강 중 예외 발생 — 다음 거래처 진행 + callFailed 카운트 증가")
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
            // 예외는 일시 실패로 간주 — callFailed.
            assertThat(result.callFailed).isEqualTo(1)
            assertThat(result.unresolved).isEqualTo(0)
        }
    }

    private fun createAccount(
        id: Long,
        address1: String? = "주소",
        latitude: String? = null,
        longitude: String? = null,
        geocodeUnresolved: Boolean? = null
    ): Account = Account(
        id = id,
        externalKey = "EXT-$id",
        address1 = address1,
        latitude = latitude,
        longitude = longitude,
        accountStatusName = "거래",
        geocodeUnresolved = geocodeUnresolved
    )
}
