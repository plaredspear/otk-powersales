package com.otoki.powersales.platform.auth.sharing.annotation

/**
 * controller endpoint 에 FLS (Field-Level Security) 적용 표시 (spec #795).
 *
 * 본 annotation 부착 endpoint 의 응답 DTO 는 `FlsResponseBodyAdvice` 가 intercept 하여
 * `@FlsField` 부착 필드 중 readable=false 인 항목을 응답 JSON 에서 omit (Q2 옵션 1 — SF 동등).
 *
 * **점진 도입 정책** (Q1 옵션 1):
 * - 본 annotation 미부착 endpoint 는 FLS 면제 — 안전 default
 * - `application.yml` 의 `fls.strict-mode = true` 설정 시 미부착 endpoint warning log
 * - 최종 100% 부착 시 SF 동등 (전 endpoint 자동 FLS)
 */
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class FlsFiltered(
    /** 본 endpoint 가 반환하는 sObject API 명 (예: "Account", "DKRetail__Promotion__c") */
    val sObject: String,
)
