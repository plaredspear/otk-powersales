package com.otoki.powersales.account.service

import com.otoki.powersales.account.dto.request.AdminAccountCreateRequest
import com.otoki.powersales.account.dto.response.AdminAccountCreateResponse
import com.otoki.powersales.account.entity.Account
import com.otoki.powersales.account.exception.AccountNameBlankException
import com.otoki.powersales.account.exception.AccountNameDuplicateException
import com.otoki.powersales.account.exception.AccountNamePrefixRequiredException
import com.otoki.powersales.account.exception.EmployeeCodeBlankException
import com.otoki.powersales.account.exception.EmployeeNotFoundException
import com.otoki.powersales.account.policy.AccountNamePrefix
import com.otoki.powersales.account.repository.AccountRepository
import com.otoki.powersales.employee.entity.Employee
import com.otoki.powersales.employee.repository.EmployeeRepository
import com.otoki.powersales.organization.entity.Organization
import com.otoki.powersales.organization.repository.OrganizationRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 관리자 웹 신규 거래처 등록 도메인 서비스. (Spec #640 P1-B)
 *
 * ## 레거시 매핑
 * - SF Apex: `AccountTrigger.trigger:1-3` (beforeInsert) → `AccountTriggerHandler.cls:43-127` (`newAccount`)
 * - flow-legacy: flow-legacy yaml (Spec #640) (8 step)
 * - origin spec: #640
 *
 * ## 레거시 동작 요약
 * 1. 입력: 영업사원이 SF Lightning UI 에서 거래처 Name 입력 후 저장 (단건). OwnerId = 영업사원 본인.
 * 2. AccountTrigger before insert → `newAccount()` 호출 (`TriggerHandler.bypass('ClientMasterReceiver')` 우회 케이스 제외).
 * 3. 동일명 중복 검증 — `SELECT COUNT(Name) FROM Account WHERE Name = :Name` → cnt > 0 시 `addError('동일한 이름의 거래처가 이미 존재합니다.')`
 * 4. prefix 검증 — `Custom Label AccountPrefix` 의 `/` split 결과 (예: `['신규','기타']`) 중 하나가 `'(' + p + ')'` 형태로 Name 에 포함되어야 함. 위반 시 `addError('신규 거래처 등록은 (...) 중 1개를 필수로 입력하셔야 합니다.')`.
 * 5. 검증 통과 시 `AccountGroup__c='9999'` 강제, 현재 사용자 → `User.DKRetail__EmployeeNumber__c` → `DKRetail__Employee__c.CostCenterCode__c` SOQL → `BranchCode__c` 자동 set.
 * 6. SF 표준 insert → DML INSERT (OwnerId = 영업사원 본인).
 *
 * ## 신규 차이
 * - **진입점**: 영업사원 native (SF Lightning) → 관리자 웹 (`POST /api/v1/admin/accounts`). 모바일 미운영. 결정 근거: 스펙 §3 Q1.
 * - **prefix 출처**: SF Custom Label `AccountPrefix` → application Constants [AccountNamePrefix]. 변경 시 1곳만 갱신. 결정 근거: 스펙 §3 Q2.
 * - **Owner 모델**: SF OwnerId (영업사원 본인) → `account.employee_code` (관리자가 dropdown 으로 선택한 영업사원 사번) + `BaseEntity.created_by` (등록 관리자 ID, JWT principal). 이중 추적. 결정 근거: 스펙 §3 Q5.
 * - **BranchCode 매핑 주체**: 현재 로그인 user → 관리자가 선택한 영업사원. 매핑 규칙은 동등 (`employee.cost_center_code` 직접 사용). 결정 근거: 스펙 §3 Q4.
 * - **BranchName 신규 보강**: 레거시는 `BranchCode__c` 만 set, `BranchName__c` 미설정. 신규는 Organization.findAll 캐시에서 cost_center_level5/4/3 매칭 → `firstNonBlank(org_name_level5/4/3)` 으로 `branch_name` 보강. 매칭 실패 시 NULL. 결정 근거: 스펙 §2.2.
 * - **dead validation 미인계**: `AccountTriggerHandler.cls:55-66` (`accList.size != 1`, `isVali=FALSE` 통과) — 레거시 dead code 로 신규 미인계. 결정 근거: 스펙 §4 비범위.
 */
@Service
class AccountCreateService(
    private val accountRepository: AccountRepository,
    private val employeeRepository: EmployeeRepository,
    private val organizationRepository: OrganizationRepository
) {

    @Transactional
    fun create(request: AdminAccountCreateRequest): AdminAccountCreateResponse {
        val name = request.name.trim()
        if (name.isBlank()) throw AccountNameBlankException()

        val employeeCode = request.employeeCode.trim()
        if (employeeCode.isBlank()) throw EmployeeCodeBlankException()

        if (!AccountNamePrefix.isValidName(name)) {
            throw AccountNamePrefixRequiredException(AccountNamePrefix.joinForMessage())
        }

        if (accountRepository.existsActiveByName(name)) {
            throw AccountNameDuplicateException()
        }

        val employee = employeeRepository.findByEmployeeCode(employeeCode)
            .filter { it.isDeleted != true }
            .orElseThrow { EmployeeNotFoundException(employeeCode) }

        val matchedOrg = lookupOrganization(employee)

        val account = Account(
            name = name,
            accountGroup = ACCOUNT_GROUP_NATIVE,
            externalKey = null,
            employeeCode = employeeCode,
            branchCode = employee.costCenterCode,
            branchName = matchedOrg?.let { firstNonBlank(it.orgNameLevel5, it.orgNameLevel4, it.orgNameLevel3) }
        )

        val saved = accountRepository.save(account)
        return AdminAccountCreateResponse.from(saved)
    }

    private fun lookupOrganization(employee: Employee): Organization? {
        val key = employee.costCenterCode?.takeIf { it.isNotBlank() } ?: return null
        return organizationRepository.findAll().firstOrNull { org ->
            firstNonBlank(org.costCenterLevel5, org.costCenterLevel4, org.costCenterLevel3) == key
        }
    }

    private fun firstNonBlank(vararg values: String?): String? =
        values.firstOrNull { !it.isNullOrBlank() }

    companion object {
        /** 영업사원 직접 등록 표시 (SAP 동기 거래처 1010/1000 과 영구 분리) */
        private const val ACCOUNT_GROUP_NATIVE: String = "9999"
    }
}
