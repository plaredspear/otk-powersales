package com.otoki.powersales.platform.auth.entity

/**
 * SF `DKRetail__Employee__c.DKRetail__AppAuthority__c` picklist value 상수.
 *
 * SF 원본 4종 picklist 정의 정합 (운영 실측 확인 — `_raw/DKRetail__Employee__c.json:1636-1664`).
 * `restrictedPicklist = false` 라 운영 UI 에서 신규 값이 추가될 수 있으나 현 시점 0건.
 * 신규 값 추가 시 본 파일 갱신.
 *
 * Employee.role 컬럼은 String 으로 본 picklist value 를 그대로 저장.
 * 6종 운영 분기 (시스템 관리자 / 영업지원실 / 영업부장 / 사업부장 / 본부장 / 영업사원) 는
 * SF AppAuthority 가 아니라 Profile.Name 으로 분기 — `WebUserPrincipal.profileName` 사용.
 */
object AppAuthority {
    const val WOMAN = "여사원"
    const val LEADER = "조장"
    const val BRANCH_MANAGER = "지점장"
    const val ACCOUNT_VIEW_ALL = "AccountViewAll"
}
