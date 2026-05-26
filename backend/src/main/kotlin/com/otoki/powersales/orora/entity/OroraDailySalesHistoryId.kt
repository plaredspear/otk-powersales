package com.otoki.powersales.orora.entity

import java.io.Serializable

/**
 * [OroraDailySalesHistory] 의 복합 PK 클래스 (JPA `@IdClass` 정합).
 *
 * - JPA 요구사항: `Serializable` 구현 + 기본 생성자 + 필드명/타입이 entity 의 `@Id` 와 동일
 * - Kotlin data class + 모든 필드 default value → no-arg 생성자 자동 생성 + `equals`/`hashCode` 자동
 * - DB 측 PK 부재 (view 성질) 라도 JPA 1차 캐시 / `EntityManager.find(id)` 가 본 클래스로 entity identity 추적
 */
data class OroraDailySalesHistoryId(
	val sapAccountCode: String = "",
	val salesDate: String = "",
) : Serializable
