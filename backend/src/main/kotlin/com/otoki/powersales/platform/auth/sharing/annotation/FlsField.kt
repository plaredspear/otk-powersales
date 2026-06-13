package com.otoki.powersales.platform.auth.sharing.annotation

/**
 * 응답 DTO field 와 SF SObject.Field 매핑 (spec #795).
 *
 * `@FlsFiltered` 부착 endpoint 의 응답 DTO 중 본 annotation 부착 field 만 FLS 평가 대상.
 * 미부착 field 는 항상 응답에 포함 (Q4 옵션 1 — audit field 면제 동등 처리).
 *
 * SF SObject.FieldApiName 형식 — 예: `Account.AnnualRevenue`, `DKRetail__Promotion__c.CostCenterCode__c`.
 *
 * 본 매핑 메타는 SF 에 부재 (SF 는 SObject.Field 단위, DTO 는 신규 시스템 별도 구조). 따라서
 * 신규 시스템 개발자가 DTO 별 수동 부착 (점진 도입 — Q1 옵션 1).
 */
@Target(AnnotationTarget.FIELD, AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.RUNTIME)
annotation class FlsField(
    /** SF SObject.Field API 명 — 예: "Account.AnnualRevenue" */
    val sObjectField: String,
)
