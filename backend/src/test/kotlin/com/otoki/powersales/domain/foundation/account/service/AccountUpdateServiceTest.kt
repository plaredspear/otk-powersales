package com.otoki.powersales.domain.foundation.account.service

import com.otoki.powersales.domain.foundation.account.dto.request.AdminAccountUpdateRequest
import com.otoki.powersales.domain.foundation.account.dto.response.AdminAccountUpdateResponse
import com.otoki.powersales.platform.auth.entity.AppAuthority
import com.otoki.powersales.platform.auth.web.WebUserPrincipal
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

/**
 * [AccountUpdateService] 조율 테스트 — 주소 변경 시 좌표 동기 재조회 호출 흐름 검증.
 *
 * 필드 검증/갱신 로직 자체는 [AccountUpdateTxServiceTest] 가 담당하며, 본 테스트는
 * `applyUpdate` 의 주소 변경 여부 결과에 따라 [AccountNaverGeocodeService.refreshSingleAccount]
 * 가 정확히 호출/미호출되는지에 집중한다 (협력 객체 mock).
 */
@DisplayName("AccountUpdateService 조율 테스트 (좌표 재조회 호출 흐름, Spec #643 P1-B)")
class AccountUpdateServiceTest {

    private val accountTxService: AccountUpdateTxService = mockk()
    private val accountNaverGeocodeService: AccountNaverGeocodeService = mockk(relaxed = true)

    private val service = AccountUpdateService(
        accountTxService,
        accountNaverGeocodeService,
    )

    private val accountId = 1234L
    private val principal = WebUserPrincipal(
        userId = 1L,
        usernameValue = "u1@otokims.co.kr",
        employeeCode = "S1",
        employeeId = 1L,
        role = AppAuthority.BRANCH_MANAGER,
        costCenterCode = null,
        profileName = "9. Staff",
        isSalesSupport = false,
        passwordChangeRequired = false,
        permissions = emptySet(),
        encodedPassword = "",
        grantedAuthorities = emptyList(),
        active = true
    )

    private val response: AdminAccountUpdateResponse = mockk()

    @Test
    @DisplayName("주소 변경됨 - applyUpdate 후 refreshSingleAccount 동기 호출")
    fun addressChanged_triggersRefresh() {
        val request = AdminAccountUpdateRequest(address1 = "서울특별시 강남구 테헤란로 100")
        every { accountTxService.applyUpdate(accountId, principal, request) } returns true
        every { accountTxService.findResponse(accountId) } returns response

        service.update(accountId, principal, request)

        verify(exactly = 1) { accountNaverGeocodeService.refreshSingleAccount(accountId) }
        verify(exactly = 1) { accountTxService.findResponse(accountId) }
    }

    @Test
    @DisplayName("주소 미변경 - refreshSingleAccount 호출 안 함")
    fun addressUnchanged_noRefresh() {
        val request = AdminAccountUpdateRequest(phone = "02-9999-9999")
        every { accountTxService.applyUpdate(accountId, principal, request) } returns false
        every { accountTxService.findResponse(accountId) } returns response

        service.update(accountId, principal, request)

        verify(exactly = 0) { accountNaverGeocodeService.refreshSingleAccount(any()) }
        verify(exactly = 1) { accountTxService.findResponse(accountId) }
    }
}
