package com.otoki.powersales.domain.org.employee.dto.request

import jakarta.validation.constraints.NotNull

/**
 * 사원 앱 로그인 활성(appLoginActive) 전용 수정 요청 DTO.
 *
 * 일반 사원 수정([AdminEmployeeUpdateRequest]) 은 origin=SAP 사원을 차단하므로, 운영 데이터의
 * 사원(전량 origin=SAP — 컬럼 DEFAULT + SF 마이그레이션 미매핑) 은 앱 로그인을 수동으로 켤 방법이
 * 없었다. 권한(role) 전용 경로([AdminEmployeeRoleUpdateRequest]) 와 동일하게 단일 축만 다루는
 * 별도 경로로 분리해 origin 과 무관하게 조작할 수 있도록 한다.
 *
 * 권한 경로와 달리 이 축은 **SAP 인입이 갱신하는 컬럼**(`LockingFlag` → appLoginActive) 이라
 * 다음 인사 인입 시 SAP 값으로 덮어써진다. 즉 본 경로는 SoT 를 바꾸는 것이 아니라 인입 사이의
 * 수동 구제(임시 활성화) 수단이다 — 호출 UI 가 이 점을 안내한다.
 */
data class AdminEmployeeAppLoginActiveUpdateRequest(

    /** true = 앱 로그인 활성, false = 비활성. */
    @field:NotNull(message = "앱 로그인 활성 여부는 필수입니다")
    val appLoginActive: Boolean? = null
)
