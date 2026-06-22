package com.otoki.powersales.domain.foundation.account.service

import com.otoki.powersales.domain.foundation.account.dto.request.AdminAccountUpdateRequest
import com.otoki.powersales.domain.foundation.account.dto.response.AdminAccountUpdateResponse
import com.otoki.powersales.domain.foundation.account.entity.Account
import com.otoki.powersales.domain.foundation.account.exception.AccountNameBlankException
import com.otoki.powersales.domain.foundation.account.exception.AccountNameDuplicateException
import com.otoki.powersales.domain.foundation.account.exception.AccountNamePrefixRequiredForUpdateException
import com.otoki.powersales.domain.foundation.account.exception.AccountNotFoundException
import com.otoki.powersales.domain.foundation.account.exception.EmployeeNotFoundException
import com.otoki.powersales.domain.foundation.account.policy.AccountNamePrefix
import com.otoki.powersales.domain.foundation.account.repository.AccountRepository
import com.otoki.powersales.domain.org.employee.repository.EmployeeRepository
import com.otoki.powersales.platform.auth.web.WebUserPrincipal
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 관리자 웹 거래처 수정 도메인 서비스. (Spec #643 P1-B)
 *
 * ## 레거시 매핑
 * - SF Apex: `AccountTrigger.trigger:1-3` (beforeUpdate) → `AccountTriggerHandler.cls:22-33` (분기) → `AccountTriggerHandler.cls:129-155` (`editAccountName`)
 * - flow-legacy: flow-legacy yaml (Spec #643) (5 step)
 * - origin spec: #643
 *
 * ## 레거시 동작 요약
 * 1. 입력: SF Lightning UI 의 Account 표준 edit 화면(또는 inline edit / API) 에서 거래처 수정 후 SF 표준 update API 호출.
 * 2. AccountTrigger before update → `TriggerHandler.isBypassed('ClientMasterReceiver')` 체크 + 비우회 시 `Profile.Name != '시스템 관리자'` 체크 → `editAccountName()` 호출 분기.
 * 3. bypass 우회 시 `setLatLongNull` 동작 — Address1/Address2 변경 감지 시 `Latitude__c=null`, `Longitude__c=null` 강제.
 * 4. bypass 미우회 + Profile != 시스템 관리자 시 `editAccountName` —
 *    - Name 변경 시 동일명 중복 SOQL → cnt > 0 시 `addError('동일한 이름의 거래처가 이미 존재합니다.')`
 *    - Name 변경 무관 항상 prefix 검증 (`Custom Label AccountPrefix` '/' split, `'(' + p + ')'` contains 위반 시 `addError`)
 * 5. 검증 통과 후 SF 표준 update → DML UPDATE.
 *
 * ## 신규 차이
 * - **진입점**: SF Lightning UI native → 관리자 웹 (`PUT /api/v1/admin/accounts/{id}`). 모바일은 거래처 조회 단독 유지. 결정 근거: 스펙 §3 Q-C.
 * - **bypass 패턴 자연 소멸**: SAP 인바운드 / Naver Geocode batch 가 별도 endpoint 로 분리되어 신규에는 bypass 분기 부재. 본 service 는 native 거래처 관리자 수정 단일 흐름.
 * - **검증 우회 권한**: SF "시스템 관리자 Profile" — Profile.name == "시스템 관리자". prefix + 동일명 중복 검증 모두 skip. blank/Account 존재/employee 존재 검증은 시스템 관리자도 적용. 결정 근거: 스펙 §3 Q-A.
 * - **prefix 검증 발동**: 레거시 코드 동등 — Name 변경 여부와 무관하게 항상 발동 (페이로드에 name 포함 시만, PUT 부분 갱신 시맨틱 적용). 결정 근거: 스펙 §3 Q-B.
 * - **prefix 출처**: SF Custom Label `AccountPrefix` → application Constants [AccountNamePrefix] (#640 재사용). 변경 시 1곳만 갱신.
 * - **prefix 메시지 분기**: 신규 [AccountNamePrefixRequiredForUpdateException] — errorCode 는 `ACCOUNT_NAME_PREFIX_REQUIRED` 동일 (#640 와 호환), 메시지만 "거래처 수정은 ..." 으로 분기.
 * - **자기 자신 제외 중복 검증**: 레거시 `Trigger.oldMap` 비교 동등 효과 — `existsActiveByNameAndIdNot(name, id)` 로 자기 자신 제외.
 * - **employeeCode 변경 허용 + branch 자동 재계산 안 함**: 레거시 동등 — `BranchCode__c` 자동 set 은 `newAccount()` (CREATE) 경로 한정. 운영자 필요 시 branch 직접 수정. 결정 근거: 스펙 §3 Q-D.
 * - **Address 변경 시 좌표 즉시 재조회**: Address1/Address2 변경 감지 시 메인 쓰기 트랜잭션 커밋 후 [AccountNaverGeocodeService.refreshSingleAccount] 로 `address1` 을 동기 재조회해 `latitude/longitude` 를 즉시 갱신. 외부 HTTP 호출은 메인 쓰기 트랜잭션 밖에서 별도 트랜잭션으로 수행 (커넥션 점유 최소화). 조회 실패 / 주소 부재 시 좌표 null 무효화 → Naver Geocode batch 가 재변환 픽업(fallback). (SAP 인바운드 경로 [AccountUpsertMapper] 는 좌표 null 무효화 후 batch 보강 방식 유지.)
 * - **PUT 부분 갱신 시맨틱**: nullable 필드 + null = 미포함 (보존) 패턴 — Q-E 의 "null 명시 = null 로 덮어쓰기" 부분만 의도적 단순화 (본 P1-B 구현 시점, 운영 시나리오 빈도 낮음).
 * - **SAP 동기 키 silent ignore**: DTO 정의 자체에서 제외 — Jackson deserialization 단에서 알 수 없는 필드 무시.
 *
 * @see AccountCreateService — 거래처 등록 (#640) 자매 service
 */
@Service
class AccountUpdateService(
    private val accountTxService: AccountUpdateTxService,
    private val accountNaverGeocodeService: AccountNaverGeocodeService
) {

    /**
     * 거래처 수정 단일 진입.
     *
     * 주소(address1/address2) 변경이 감지되면, 메인 쓰기 트랜잭션 커밋 **후** [AccountNaverGeocodeService.refreshSingleAccount]
     * 로 좌표를 동기 재조회한다 (외부 HTTP 응답 동안 메인 쓰기 트랜잭션이 DB 커넥션을 점유하지 않도록 트랜잭션 분리).
     * 트랜잭션 메서드([AccountUpdateTxService])는 별도 빈으로 분리해 self-invocation 으로 인한 프록시 우회를 방지한다.
     *
     * @param id path variable Account.id
     * @param principal JWT 인증 사용자 (SYSTEM_ADMIN 우회 분기 판정용)
     * @param request PUT 페이로드 — 모든 필드 nullable, null 인 필드는 갱신 skip
     * @return 갱신 후 entity 매핑 응답
     * @throws AccountNotFoundException Account 부재 또는 soft-delete 처리됨
     * @throws AccountNameBlankException name 페이로드 포함했으나 blank
     * @throws AccountNamePrefixRequiredForUpdateException name 페이로드 포함 + prefix 위반 + non-SYSTEM_ADMIN
     * @throws AccountNameDuplicateException name 변경 + 자기 자신 제외 동일명 활성 row 존재 + non-SYSTEM_ADMIN
     * @throws EmployeeNotFoundException employeeCode 페이로드 포함 + Employee 부재 또는 soft-delete
     */
    fun update(
        id: Long,
        principal: WebUserPrincipal,
        request: AdminAccountUpdateRequest
    ): AdminAccountUpdateResponse {
        val addressChanged = accountTxService.applyUpdate(id, principal, request)
        if (addressChanged) {
            // 주소 변경 → 좌표 동기 재조회 (별도 트랜잭션). 외부 API 실패 시 좌표 무효화 + 배치 fallback.
            accountNaverGeocodeService.refreshSingleAccount(id)
        }
        return accountTxService.findResponse(id)
    }
}

/**
 * [AccountUpdateService] 의 트랜잭션 경계 빈. 필드 검증/갱신과 응답 조회를 각각 짧은 트랜잭션으로 수행한다.
 *
 * 외부 HTTP 호출(좌표 재조회)을 메인 쓰기 트랜잭션 밖으로 분리하기 위해 트랜잭션 메서드를 별도 빈으로 둔다
 * (같은 빈 내 self-invocation 은 Spring AOP 프록시를 우회해 `@Transactional` 이 적용되지 않으므로).
 */
@Service
class AccountUpdateTxService(
    private val accountRepository: AccountRepository,
    private val employeeRepository: EmployeeRepository
) {

    /**
     * 필드 검증/갱신 메인 쓰기 트랜잭션. 외부 HTTP 호출을 포함하지 않는다.
     *
     * @return 주소(address1/address2) 변경 여부
     */
    @Transactional
    fun applyUpdate(
        id: Long,
        principal: WebUserPrincipal,
        request: AdminAccountUpdateRequest
    ): Boolean {
        val account = accountRepository.findActiveById(id)
            ?: throw AccountNotFoundException(id)

        val prevAddress1 = account.address1
        val prevAddress2 = account.address2

        validateAndApplyName(account, request.name, principal)
        validateAndApplyEmployeeCode(account, request.employeeCode)
        applyOtherFields(account, request)

        return prevAddress1 != account.address1 || prevAddress2 != account.address2
    }

    @Transactional(readOnly = true)
    fun findResponse(id: Long): AdminAccountUpdateResponse {
        val account = accountRepository.findActiveById(id)
            ?: throw AccountNotFoundException(id)
        return AdminAccountUpdateResponse.Companion.from(account)
    }

    private fun validateAndApplyName(account: Account, requestName: String?, principal: WebUserPrincipal) {
        if (requestName == null) return // PUT 부분 갱신 — 미포함 시 검증/갱신 skip

        val name = requestName.trim()
        if (name.isBlank()) throw AccountNameBlankException()

        val isSystemAdmin = principal.profileName == "시스템 관리자"
        if (!isSystemAdmin) {
            // Q-B 항상 발동 — Name 변경 여부와 무관 (레거시 코드 동등)
            if (!AccountNamePrefix.isValidName(name)) {
                throw AccountNamePrefixRequiredForUpdateException(AccountNamePrefix.joinForMessage())
            }
            // 자기 자신 제외 동일명 — name 변경 시만 검증
            if (account.name != name && accountRepository.existsActiveByNameAndIdNot(name, account.id)) {
                throw AccountNameDuplicateException()
            }
        }

        account.name = name
    }

    private fun validateAndApplyEmployeeCode(account: Account, requestEmployeeCode: String?) {
        if (requestEmployeeCode == null) return // PUT 미포함 — skip

        val employeeCode = requestEmployeeCode.trim()
        if (employeeCode.isBlank()) return // Q-E `""` = null 동등 — 변경 안 함

        if (employeeCode == account.employeeCode) return // 동일 값 — Employee 검증 불필요

        val employee = employeeRepository.findByEmployeeCode(employeeCode)
            .filter { it.isDeleted != true }
            .orElseThrow { EmployeeNotFoundException(employeeCode) }

        account.employeeCode = employee.employeeCode
        // Q-D: branch_code/branch_name 자동 재계산 안 함 (레거시 동등). 운영자가 별도 필드로 직접 수정.
    }

    @Suppress("ComplexMethod")
    private fun applyOtherFields(account: Account, request: AdminAccountUpdateRequest) {
        // 모든 필드는 null = 미포함 (보존) 시맨틱 — 명시적 if-not-null 분기로 갱신
        request.address1?.let { account.address1 = it }
        request.address2?.let { account.address2 = it }
        request.phone?.let { account.phone = it }
        request.mobilePhone?.let { account.mobilePhone = it }
        request.representative?.let { account.representative = it }
        request.email?.let { account.email = it }
        request.zipCode?.let { account.zipCode = it }
        request.industry?.let { account.industry = it }
        request.description?.let { account.description = it }
        request.website?.let { account.website = it }
        request.fax?.let { account.fax = it }
        request.closingTime1?.let { account.closingTime1 = it }
        request.closingTime2?.let { account.closingTime2 = it }
        request.closingTime3?.let { account.closingTime3 = it }
        request.accountNumber?.let { account.accountNumber = it }
        request.site?.let { account.site = it }
        request.accountSource?.let { account.accountSource = it }
        request.mapCoordinate?.let { account.mapCoordinate = it }
        request.rating?.let { account.rating = it }
        request.ownership?.let { account.ownership = it }
        request.accountStatusName?.let { account.accountStatusName = it }
        request.accountStatusCode?.let { account.accountStatusCode = it }
        request.businessType?.let { account.businessType = it }
        request.businessCategory?.let { account.businessCategory = it }
        request.businessLicenseNumber?.let { account.businessLicenseNumber = it }
        request.consignmentAcc?.let { account.consignmentAcc = it }
        request.distribution?.let { account.distribution = it }
        request.accountType?.let { account.accountType = it }
        request.freezerType?.let { account.freezerType = it }
        request.freezerInstalled?.let { account.freezerInstalled = it }
        request.firstInstalled?.let { account.firstInstalled = it }
        request.orderEndTime?.let { account.orderEndTime = it }
        request.remainingCredit?.let { account.remainingCredit = it }
        request.totalCredit?.let { account.totalCredit = it }
        request.annualRevenue?.let { account.annualRevenue = it }
        request.numberOfEmployees?.let { account.numberOfEmployees = it }
        request.branchCode?.let { account.branchCode = it }
        request.branchName?.let { account.branchName = it }
        request.abcType?.let { account.abcType = it }
        request.abcTypeCode?.let { account.abcTypeCode = it }
    }
}
