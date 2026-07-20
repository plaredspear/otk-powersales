package com.otoki.powersales.domain.activity.promotion.sap

/**
 * 전문행사조 마스터 SAP 송신 row (Spec #765 §6.1).
 *
 * 레거시 `IF_REST_SAP_PPTMToSAP.cls:99-124` 의 paraMap 17개 키와 정합. 레거시 `Account` 키
 * (cls:104 `obj.Account__c` = SF Account Lookup → **SF Record Id(sfid)**) 는 신규 시스템에 SF sfid 가
 * 없으므로, 신규 거래처 식별자 `Account.id` 를 문자열로 채워 송신한다 (sfid 자리를 신규 PK 로 대체).
 * 별도 식별 키 `AccountCode` (ExternalKey) 도 레거시대로 함께 송신한다.
 *
 * 값은 모두 `String` 타입 — 레거시 `Map<String,String>` 정합. null 가능 필드는 빈 문자열이 아닌
 * JSON `null` 로 직렬화된다 (Apex `String.valueOf` 는 null 입력 시 null 을 반환하고,
 * `JSON.serialize` 가 map 의 null 값을 JSON `null` 로 내보내는 것과 동일).
 */
@Suppress("ConstructorParameterNaming", "PropertyName")
data class PPTMasterSapPayloadRow(
    /** SF auto-number `PM{0000000}` (레거시 `obj.Name`). */
    val Name: String?,
    /** 전문행사조 picklist 한글 값 (레거시 `obj.ProfessionalPromotionTeam__c`). */
    val ProfessionalPromotionTeam: String?,
    /** 거래처 식별자 (레거시 `obj.Account__c` = SF Account sfid → 신규 `Account.id` 문자열로 대체). */
    val Account: String?,
    /** 직원 한글 이름 (레거시 `obj.FullName__c` Lookup 표시값). */
    val FullName: String?,
    /** 사번 (레거시 수식 `FullName__r.DKRetail__EmpCode__c`). */
    val EmployeeNumber: String?,
    /** 거래처 상태 한글 (레거시 수식 `Account__r.AccountStatusName__c`). */
    val AccountStatus: String?,
    /** 거래처 유형 (레거시 수식 `TEXT(Account__r.Type)`). */
    val AccountType: String?,
    /** 거래처 외부키 (레거시 수식 `Account__r.ExternalKey__c`). */
    val AccountCode: String?,
    /** 시작일 `YYYY-MM-DD` (레거시 `String.valueOf(StartDate__c)`). */
    val StartDate: String,
    /** 종료일 `YYYY-MM-DD` 또는 JSON null (레거시 `String.valueOf(EndDate__c)` 는 null 입력 시 null 반환). */
    val EndDate: String?,
    /** 유효데이터 (`미확정`/`유효`/`예정`/`종료` — 레거시 `ValidData__c` 수식 재현). */
    val ValidData: String,
    /** 재직상태 (`재직`/`휴직`/`퇴직YYYY-MM-DD`/`퇴직예정YYYY-MM-DD` — 레거시 `ValidConditionData__c` 수식 재현). */
    val ValidConditionData: String,
    /** 조직유형 (레거시 `obj.CostCenterCode__c` — 신규 entity `branch_code` 매핑). */
    val CostCenterCode: String?,
    /** 지점명 (레거시 수식 `FullName__r.DKRetail__OrgName__c`). */
    val BranchName: String?,
    /** 직위 (레거시 수식 `FullName__r.DKRetail__Jikwee__c`). */
    val Title: String?,
    /** 확정 여부 `"true"`/`"false"` (레거시 `String.valueOf(Confirmed__c)`). */
    val Confirmed: String,
    /** 송신 시점 연월 `yyyyMM` (레거시 `getYYYYMM()`). */
    val YearMonth: String
)
