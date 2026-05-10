package com.otoki.powersales.product.service.dto

/**
 * 도메인 측 String → T 변환 결과.
 *
 * SAP 페이로드의 숫자/날짜 String 값을 도메인이 받아 변환할 때 사용한다.
 * 변환 성공: `value` 보유, `isFailure = false`. 변환 실패: `value = null`, `isFailure = true`.
 */
data class ParseResult<T>(val value: T?, val isFailure: Boolean)
