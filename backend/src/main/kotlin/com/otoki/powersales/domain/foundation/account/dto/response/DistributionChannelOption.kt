package com.otoki.powersales.domain.foundation.account.dto.response

/**
 * 유통형태(거래처유형마스터) 조회조건 드롭다운 옵션 1건.
 *
 * - [code] = `AccountCategoryMaster.accountCode` (거래처유형코드). 화면이 조회 요청에 되돌려 보내는 값.
 * - [label] = `"{거래처유형코드} {이름}"` (예 `"06 슈퍼"`). 목록 컬럼 라벨과 동일 규칙이므로
 *   드롭다운 표시와 결과 컬럼 표기가 항상 일치한다.
 *
 * 값이 코드인 이유: 레거시 SF `SalesComparisonSearchController.getCategoryList` 가
 * `label = Name / value = AccountCode__c` 로 내려주던 것과 동일 계약이고, 코드가 마스터의
 * external key(unique) 라 이름 변경에 영향받지 않는다.
 */
data class DistributionChannelOption(
    val code: String,
    val label: String,
)
