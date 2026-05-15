package com.otoki.powersales.account.service

import com.otoki.powersales.account.dto.request.AdminAccountUpdateRequest
import com.otoki.powersales.account.dto.response.AdminAccountUpdateResponse
import com.otoki.powersales.account.entity.Account
import com.otoki.powersales.account.exception.AccountNameBlankException
import com.otoki.powersales.account.exception.AccountNameDuplicateException
import com.otoki.powersales.account.exception.AccountNamePrefixRequiredForUpdateException
import com.otoki.powersales.account.exception.AccountNotFoundException
import com.otoki.powersales.account.exception.EmployeeNotFoundException
import com.otoki.powersales.account.policy.AccountNamePrefix
import com.otoki.powersales.account.repository.AccountRepository
import com.otoki.powersales.auth.entity.UserRole
import com.otoki.powersales.auth.web.WebUserPrincipal
import com.otoki.powersales.employee.repository.EmployeeRepository
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
 * - **검증 우회 권한**: SF "시스템 관리자 Profile" → [UserRole.SYSTEM_ADMIN] (한글 "시스템관리자"). prefix + 동일명 중복 검증 모두 skip. blank/Account 존재/employee 존재 검증은 SYSTEM_ADMIN 도 적용. 결정 근거: 스펙 §3 Q-A.
 * - **prefix 검증 발동**: 레거시 코드 동등 — Name 변경 여부와 무관하게 항상 발동 (페이로드에 name 포함 시만, PUT 부분 갱신 시맨틱 적용). 결정 근거: 스펙 §3 Q-B.
 * - **prefix 출처**: SF Custom Label `AccountPrefix` → application Constants [AccountNamePrefix] (#640 재사용). 변경 시 1곳만 갱신.
 * - **prefix 메시지 분기**: 신규 [AccountNamePrefixRequiredForUpdateException] — errorCode 는 `ACCOUNT_NAME_PREFIX_REQUIRED` 동일 (#640 와 호환), 메시지만 "거래처 수정은 ..." 으로 분기.
 * - **자기 자신 제외 중복 검증**: 레거시 `Trigger.oldMap` 비교 동등 효과 — `existsActiveByNameAndIdNot(name, id)` 로 자기 자신 제외.
 * - **employeeCode 변경 허용 + branch 자동 재계산 안 함**: 레거시 동등 — `BranchCode__c` 자동 set 은 `newAccount()` (CREATE) 경로 한정. 운영자 필요 시 branch 직접 수정. 결정 근거: 스펙 §3 Q-D.
 * - **Address 변경 시 좌표 자동 null 미적용**: #637 (account-naver-geocode-batch) §2.6 정책 — Naver Geocode batch 가 lat/long null 거래처 자동 픽업 정책으로 충분.
 * - **PUT 부분 갱신 시맨틱**: nullable 필드 + null = 미포함 (보존) 패턴 — Q-E 의 "null 명시 = null 로 덮어쓰기" 부분만 의도적 단순화 (본 P1-B 구현 시점, 운영 시나리오 빈도 낮음).
 * - **SAP 동기 키 silent ignore**: DTO 정의 자체에서 제외 — Jackson deserialization 단에서 알 수 없는 필드 무시.
 *
 * @see AccountCreateService — 거래처 등록 (#640) 자매 service
 */
@Service
class AccountUpdateService(
    private val accountRepository: AccountRepository,
    private val employeeRepository: EmployeeRepository
) {

    /**
     * 거래처 수정 단일 진입.
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
    @Transactional
    fun update(
        id: Int,
        principal: WebUserPrincipal,
        request: AdminAccountUpdateRequest
    ): AdminAccountUpdateResponse {
        val account = accountRepository.findActiveById(id)
            ?: throw AccountNotFoundException(id)

        validateAndApplyName(account, request.name, principal)
        validateAndApplyEmployeeCode(account, request.employeeCode)
        applyOtherFields(account, request)

        return AdminAccountUpdateResponse.from(account)
    }

    private fun validateAndApplyName(account: Account, requestName: String?, principal: WebUserPrincipal) {
        if (requestName == null) return // PUT 부분 갱신 — 미포함 시 검증/갱신 skip

        val name = requestName.trim()
        if (name.isBlank()) throw AccountNameBlankException()

        val isSystemAdmin = principal.role == UserRole.SYSTEM_ADMIN
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
        request.parentSfid?.let { account.parentSfid = it }
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
