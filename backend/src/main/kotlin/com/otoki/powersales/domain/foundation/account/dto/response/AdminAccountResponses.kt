package com.otoki.powersales.domain.foundation.account.dto.response

import com.otoki.powersales.domain.foundation.account.entity.Account
import com.otoki.powersales.domain.foundation.account.entity.AccountSource
import com.otoki.powersales.domain.foundation.account.entity.FreezerType
import com.otoki.powersales.domain.foundation.account.entity.Industry
import com.otoki.powersales.domain.foundation.account.entity.Ownership
import com.otoki.powersales.domain.foundation.account.entity.Rating
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalTime

data class AccountListResponse(
    val content: List<AccountListItem>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int
)

data class AccountListItem(
    val id: Long,
    val externalKey: String?,
    val name: String?,
    val abcType: String?,
    val branchCode: String?,
    val branchName: String?,
    val employeeCode: String?,
    val address1: String?,
    val phone: String?,
    val accountStatusName: String?,
    // SF 행사마스터 거래처 고급 검색(Enhanced Lookup) 결과 그리드 컬럼 동등 — 거래처유형/우편번호/대표자명.
    val accountType: String?,
    val zipCode: String?,
    val representative: String?
) {
    companion object {
        fun from(account: Account): AccountListItem = AccountListItem(
            id = account.id,
            externalKey = account.externalKey,
            name = account.name,
            abcType = account.abcType,
            branchCode = account.branchCode,
            branchName = account.branchName,
            employeeCode = account.employeeCode,
            address1 = account.address1,
            phone = account.phone,
            accountStatusName = account.accountStatusName,
            accountType = account.accountType,
            zipCode = account.zipCode,
            representative = account.representative
        )
    }
}

/**
 * 관리자 웹 거래처 상세 조회 응답 DTO.
 *
 * 거래처 상세 페이지(`GET /api/v1/admin/accounts/{id}`) 의 "기본 정보" 영역 — 레거시 SF Account
 * 레코드 페이지(`Account_Record_Page`)의 표준 detailPanel + FieldSet 노출 필드 동등.
 *
 * [AdminAccountUpdateResponse] 와 노출 필드가 대부분 겹치나, 상세 페이지는 **읽기 전용 식별 정보**
 * (`externalKey` / `sfid 제외` / `latitude` / `longitude`) 를 추가로 포함한다. SAP 동기 키 중
 * 거래처코드(`externalKey`)는 화면 식별자로 노출하되, `werk*`/`sales_dept_*` 등 내부 SAP 코드는 제외.
 */
data class AccountDetailResponse(
    val id: Long,
    val externalKey: String?,
    val name: String?,
    val accountGroup: String?,
    val employeeCode: String?,
    val branchCode: String?,
    val branchName: String?,
    val address1: String?,
    val address2: String?,
    val zipCode: String?,
    val latitude: String?,
    val longitude: String?,
    val phone: String?,
    val mobilePhone: String?,
    val fax: String?,
    val representative: String?,
    val email: String?,
    val website: String?,
    val industry: Industry?,
    val description: String?,
    val businessLicenseNumber: String?,
    val businessType: String?,
    val businessCategory: String?,
    val abcType: String?,
    val abcTypeCode: String?,
    /** 주문가능 거래처유형 여부 — abcTypeCode 가 주문 셀렉터 허용 코드([Account.ORDER_ABC_TYPE_CODES])에 속하는지. */
    val orderableType: Boolean,
    val accountType: String?,
    val accountStatusName: String?,
    val accountStatusCode: String?,
    val accountNumber: String?,
    val site: String?,
    val accountSource: AccountSource?,
    val mapCoordinate: String?,
    val rating: Rating?,
    val ownership: Ownership?,
    val freezerInstalled: Boolean?,
    val freezerType: FreezerType?,
    val firstInstalled: LocalDate?,
    val orderEndTime: LocalTime?,
    val closingTime1: String?,
    val closingTime2: String?,
    val closingTime3: String?,
    val remainingCredit: BigDecimal?,
    val totalCredit: BigDecimal?,
    val annualRevenue: BigDecimal?,
    val numberOfEmployees: BigDecimal?,
    val consignmentAcc: String?,
    val distribution: String?
) {
    companion object {
        fun from(account: Account): AccountDetailResponse = AccountDetailResponse(
            id = account.id,
            externalKey = account.externalKey,
            name = account.name,
            accountGroup = account.accountGroup,
            employeeCode = account.employeeCode,
            branchCode = account.branchCode,
            branchName = account.branchName,
            address1 = account.address1,
            address2 = account.address2,
            zipCode = account.zipCode,
            latitude = account.latitude,
            longitude = account.longitude,
            phone = account.phone,
            mobilePhone = account.mobilePhone,
            fax = account.fax,
            representative = account.representative,
            email = account.email,
            website = account.website,
            industry = account.industry,
            description = account.description,
            businessLicenseNumber = account.businessLicenseNumber,
            businessType = account.businessType,
            businessCategory = account.businessCategory,
            abcType = account.abcType,
            abcTypeCode = account.abcTypeCode,
            orderableType = account.isOrderableType(),
            accountType = account.accountType,
            accountStatusName = account.accountStatusName,
            accountStatusCode = account.accountStatusCode,
            accountNumber = account.accountNumber,
            site = account.site,
            accountSource = account.accountSource,
            mapCoordinate = account.mapCoordinate,
            rating = account.rating,
            ownership = account.ownership,
            freezerInstalled = account.freezerInstalled,
            freezerType = account.freezerType,
            firstInstalled = account.firstInstalled,
            orderEndTime = account.orderEndTime,
            closingTime1 = account.closingTime1,
            closingTime2 = account.closingTime2,
            closingTime3 = account.closingTime3,
            remainingCredit = account.remainingCredit,
            totalCredit = account.totalCredit,
            annualRevenue = account.annualRevenue,
            numberOfEmployees = account.numberOfEmployees,
            consignmentAcc = account.consignmentAcc,
            distribution = account.distribution
        )
    }
}

/**
 * 관리자 웹 신규 거래처 등록 응답 DTO. (Spec #640)
 *
 * `account_group` 은 자동 set `'9999'` 고정. `branch_code` 는 Employee.cost_center_code 직접 사용으로
 * 등록 시점에 항상 비-NULL. `branch_name` 은 Organization 매칭 결과의 deepest non-blank,
 * 매칭 실패 시 NULL.
 */
data class AdminAccountCreateResponse(
    val id: Long,
    val name: String,
    val accountGroup: String,
    val employeeCode: String,
    val branchCode: String?,
    val branchName: String?
) {
    companion object {
        fun from(account: Account): AdminAccountCreateResponse = AdminAccountCreateResponse(
            id = account.id,
            name = account.name ?: "",
            accountGroup = account.accountGroup ?: "",
            employeeCode = account.employeeCode ?: "",
            branchCode = account.branchCode,
            branchName = account.branchName
        )
    }
}

/**
 * 관리자 웹 거래처 수정 응답 DTO. (Spec #643)
 *
 * 갱신 후 entity 의 수정 가능 필드 + 시스템 필드 일부(`id`/`accountGroup`) 노출.
 * SAP 동기 키 (`external_key`/`werk*`/`sales_dept_*`/`division_*` 등) 와 좌표(`latitude`/`longitude`) 는
 * 응답에서도 제외 — 클라이언트는 별도 상세 조회 endpoint 활용.
 *
 * #640 [AdminAccountCreateResponse] 와 별도 (필드 범위 차이 — 본 응답은 수정 가능 일반 필드 광범위 포함).
 */
data class AdminAccountUpdateResponse(
    val id: Long,
    val name: String?,
    val accountGroup: String?,
    val employeeCode: String?,
    val branchCode: String?,
    val branchName: String?,
    val address1: String?,
    val address2: String?,
    val zipCode: String?,
    val phone: String?,
    val mobilePhone: String?,
    val representative: String?,
    val email: String?,
    val fax: String?,
    val website: String?,
    val industry: Industry?,
    val description: String?,
    val businessLicenseNumber: String?,
    val businessType: String?,
    val businessCategory: String?,
    val abcType: String?,
    val abcTypeCode: String?,
    val accountType: String?,
    val accountStatusName: String?,
    val accountStatusCode: String?,
    val accountNumber: String?,
    val site: String?,
    val accountSource: AccountSource?,
    val mapCoordinate: String?,
    val rating: Rating?,
    val ownership: Ownership?,
    val freezerInstalled: Boolean?,
    val freezerType: FreezerType?,
    val firstInstalled: LocalDate?,
    val orderEndTime: LocalTime?,
    val closingTime1: String?,
    val closingTime2: String?,
    val closingTime3: String?,
    val remainingCredit: BigDecimal?,
    val totalCredit: BigDecimal?,
    val annualRevenue: BigDecimal?,
    val numberOfEmployees: BigDecimal?,
    val consignmentAcc: String?,
    val distribution: String?
) {
    companion object {
        fun from(account: Account): AdminAccountUpdateResponse = AdminAccountUpdateResponse(
            id = account.id,
            name = account.name,
            accountGroup = account.accountGroup,
            employeeCode = account.employeeCode,
            branchCode = account.branchCode,
            branchName = account.branchName,
            address1 = account.address1,
            address2 = account.address2,
            zipCode = account.zipCode,
            phone = account.phone,
            mobilePhone = account.mobilePhone,
            representative = account.representative,
            email = account.email,
            fax = account.fax,
            website = account.website,
            industry = account.industry,
            description = account.description,
            businessLicenseNumber = account.businessLicenseNumber,
            businessType = account.businessType,
            businessCategory = account.businessCategory,
            abcType = account.abcType,
            abcTypeCode = account.abcTypeCode,
            accountType = account.accountType,
            accountStatusName = account.accountStatusName,
            accountStatusCode = account.accountStatusCode,
            accountNumber = account.accountNumber,
            site = account.site,
            accountSource = account.accountSource,
            mapCoordinate = account.mapCoordinate,
            // sfid 는 SF 데이터 마이그레이션 보조 필드 — API 응답에 노출 금지 (정책).
            rating = account.rating,
            ownership = account.ownership,
            freezerInstalled = account.freezerInstalled,
            freezerType = account.freezerType,
            firstInstalled = account.firstInstalled,
            orderEndTime = account.orderEndTime,
            closingTime1 = account.closingTime1,
            closingTime2 = account.closingTime2,
            closingTime3 = account.closingTime3,
            remainingCredit = account.remainingCredit,
            totalCredit = account.totalCredit,
            annualRevenue = account.annualRevenue,
            numberOfEmployees = account.numberOfEmployees,
            consignmentAcc = account.consignmentAcc,
            distribution = account.distribution
        )
    }
}
