package com.otoki.powersales.platform.common.dto.response

import com.otoki.powersales.domain.foundation.account.entity.Account

/**
 * 내 거래처 목록 응답 DTO
 */
data class MyAccountListResponse(
    val accounts: List<MyAccountInfo>,
    val totalCount: Int,
    val meta: MyAccountMeta
)

/**
 * 거래처 표시 기준 안내 메타 정보
 *
 * 로그인 사원의 권한(여사원/조장/부서장)과 화면 유형(scope)에 따라 어떤 거래처가 목록에 노출되는지를
 * 사용자 문구로 설명한다. 권한·scope 분기 로직은 서버(`MyAccountService`)가 단일하게 보유하므로,
 * 모바일은 이 문구를 그대로 표시만 한다(클라이언트 하드코딩 분기 제거).
 */
data class MyAccountMeta(
    /** 표시 기준 본문 (불릿) */
    val criteriaLines: List<String>,
    /** 검색 동작 안내 (목록 내 검색 vs 전체 검색) */
    val searchHint: String
)

/**
 * 내 거래처 정보
 */
data class MyAccountInfo(
    val accountId: Long,
    val accountName: String,
    val accountCode: String,
    val address: String?,
    val addressDetail: String?,
    val representativeName: String?,
    val phoneNumber: String?
) {
    companion object {
        fun from(account: Account): MyAccountInfo = MyAccountInfo(
            accountId = account.id.toLong(),
            accountName = account.name ?: "",
            accountCode = account.externalKey ?: "",
            address = account.address1,
            addressDetail = account.address2,
            representativeName = account.representative,
            phoneNumber = account.phone
        )
    }
}
