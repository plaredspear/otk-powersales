package com.otoki.powersales.domain.foundation.account.service

import com.otoki.powersales.domain.foundation.account.entity.Account
import com.otoki.powersales.domain.foundation.account.service.dto.AccountUpsertCommand
import com.otoki.powersales.domain.org.organization.entity.Organization
import com.otoki.powersales.user.entity.User
import org.springframework.stereotype.Component

/**
 * [AccountUpsertCommand] → [Account] entity 매핑 책임.
 *
 * [AccountUpsertService] 가 도메인 적재 흐름(캐시 / lookup / saveAll / failures 누적)을 담당하고,
 * 본 컴포넌트는 외부 입력 모델 → entity 의 mutable 필드 매핑만 담당한다.
 * 필드 그룹별 private sub-method 로 분리하여 가독성을 확보한다.
 */
@Component
class AccountUpsertMapper {

    /**
     * 신규 [Account] 인스턴스를 만들고 mutable 필드를 적용한다.
     */
    fun newAccount(
        externalKey: String,
        name: String,
        command: AccountUpsertCommand,
        matchedOrg: Organization?,
        matchedUser: User?
    ): Account {
        val account = Account(externalKey = externalKey, name = name)
        applyMutableFields(account, command, matchedOrg, matchedUser)
        return account
    }

    /**
     * 기존 영속 [Account] 의 [Account.name] 과 mutable 필드를 갱신한다.
     */
    fun update(
        account: Account,
        name: String,
        command: AccountUpsertCommand,
        matchedOrg: Organization?,
        matchedUser: User?
    ) {
        // 주소(address1/address2) 변경 감지를 위해 갱신 전 값을 캡처한다 (applyMutableFields 가 덮어쓰기 전).
        val prevAddress1 = account.address1
        val prevAddress2 = account.address2

        account.name = name
        applyMutableFields(account, command, matchedOrg, matchedUser)

        invalidateCoordinatesIfAddressChanged(account, prevAddress1, prevAddress2)
    }

    /**
     * 주소 변경 시 좌표 무효화 — 레거시 AccountTriggerHandler.setLatLongNull() 동등 복원.
     *
     * 레거시는 ClientMasterReceiver bypass 경로의 beforeUpdate 트리거에서
     * `oldAcc.Address1__c != acc.Address1__c || oldAcc.Address2__c != acc.Address2__c` 일 때
     * Latitude__c / Longitude__c 를 null 로 초기화하고, 이후 좌표 보강 배치(Naver Geocode)가
     * null 좌표 거래처를 재취득한다. 신규는 SF Trigger 가 없으므로 본 매퍼가 동일 책임을 진다.
     *
     * 신규 생성(newAccount) 경로에는 적용하지 않는다 — 신규 거래처는 좌표가 애초에 null 이라
     * 보강 후보(latitude/longitude IS NULL)로 자연 진입한다 (레거시 beforeInsert 도 호출 안 함).
     *
     * 미적용 시: 거래처 주소가 바뀌어도 기존 좌표가 잔존해 보강 배치 후보에서 영구 제외되어
     * 지도 위치가 옛 주소에 고정되는 비동등이 발생한다.
     */
    private fun invalidateCoordinatesIfAddressChanged(
        account: Account,
        prevAddress1: String?,
        prevAddress2: String?
    ) {
        if (prevAddress1 != account.address1 || prevAddress2 != account.address2) {
            account.latitude = null
            account.longitude = null
        }
    }

    private fun applyMutableFields(
        account: Account,
        command: AccountUpsertCommand,
        matchedOrg: Organization?,
        matchedUser: User?
    ) {
        applyTypeAndStatus(account, command)
        applyBusinessInfo(account, command)
        applyContactInfo(account, command)
        applyAddressAndSchedule(account, command)
        applyWerkFields(account, command)
        applyOrganizationAndCostCenter(account, command, matchedOrg)
        // Spec #758: Owner FK = User (referenceTo == User 정합).
        // owner_sfid 는 HC sync buffer 컬럼 (application 미적재).
        account.ownerUser = matchedUser
    }

    private fun applyTypeAndStatus(account: Account, command: AccountUpsertCommand) {
        account.accountType = command.accountType
        account.accountStatusName = command.accountStatusName
        account.accountStatusCode = command.accountStatusCode
        account.accountGroup = command.accountGroup
        account.abcType = command.abcType
        account.abcTypeCode = command.abcTypeCode
        account.distribution = command.distribution
        account.consignmentAcc = command.consignmentAcc
    }

    private fun applyBusinessInfo(account: Account, command: AccountUpsertCommand) {
        account.businessType = command.businessType
        account.businessCategory = command.businessCategory
        account.businessLicenseNumber = command.businessLicenseNumber
    }

    private fun applyContactInfo(account: Account, command: AccountUpsertCommand) {
        account.phone = command.phone
        account.mobilePhone = command.mobilePhone
        account.email = command.email
        account.representative = command.representative
        account.employeeCode = command.employeeCode
    }

    private fun applyAddressAndSchedule(account: Account, command: AccountUpsertCommand) {
        account.zipCode = command.zipcode
        account.address1 = command.address1
        account.address2 = command.address2
        account.closingTime1 = command.closingTime1
        account.closingTime2 = command.closingTime2
        account.closingTime3 = command.closingTime3
    }

    private fun applyWerkFields(account: Account, command: AccountUpsertCommand) {
        account.werk1 = command.werk1
        account.werk2 = command.werk2
        account.werk3 = command.werk3
        account.werk1Tx = command.werk1Tx
        account.werk2Tx = command.werk2Tx
        account.werk3Tx = command.werk3Tx
    }

    private fun applyOrganizationAndCostCenter(
        account: Account,
        command: AccountUpsertCommand,
        matchedOrg: Organization?
    ) {
        account.divisionName = command.divisionName
        account.salesDeptName = command.salesDeptName
        account.salesDeptCostCenter = command.salesDeptCode
        account.divisionCostCenter = command.divisionCode

        // 지점/사업부/영업부 코드: Organization 매칭 시 OrgCode 의 deepest non-blank 값(Level5 → 4 → 3)을 적재.
        // 매칭 실패 시 distinction:
        //  - branchCode: 페이로드 raw fallback (spec #641 §7-A.8 의도된 deviation — 레거시는 set 안 함, 신규는 trace 정밀도 향상 목적 raw 보존).
        //  - divisionCode / salesDeptCode: NULL 유지 (페이로드 raw 는 별도 컬럼 divisionCostCenter / salesDeptCostCenter 에 보존되므로 중복 적재 회피).
        // branchName: Organization 매칭 시 OrgName 우선 + 매칭 실패 시 페이로드 raw fallback (spec #641 §7-A.8 박제 — 레거시는 페이로드 raw 만 set Org 매칭 결과로 덮어쓰지 않음, 신규는 OrgName 우선 + raw fallback).
        // branchCostCenter: 페이로드 raw 보존 (Org 매칭 무관) — 레거시 IF_REST_SAP_ClientMasterReceive.cls:138 정합 (acc.BranchCostCenter__c = obj.BranchCode raw).
        account.divisionCode = matchedOrg?.let { firstNonBlank(it.orgCodeLevel5, it.orgCodeLevel4, it.orgCodeLevel3) }
        account.salesDeptCode = matchedOrg?.let { firstNonBlank(it.orgCodeLevel5, it.orgCodeLevel4, it.orgCodeLevel3) }
        account.branchCode = matchedOrg?.let { firstNonBlank(it.orgCodeLevel5, it.orgCodeLevel4, it.orgCodeLevel3) }
            ?: command.branchCode?.takeIf { it.isNotBlank() }
        account.branchName = matchedOrg?.let { firstNonBlank(it.orgNameLevel5, it.orgNameLevel4, it.orgNameLevel3) }
            ?: command.branchName?.takeIf { it.isNotBlank() }
        account.branchCostCenter = command.branchCode?.takeIf { it.isNotBlank() }
    }

    private fun firstNonBlank(vararg values: String?): String? =
        values.firstOrNull { !it.isNullOrBlank() }
}
