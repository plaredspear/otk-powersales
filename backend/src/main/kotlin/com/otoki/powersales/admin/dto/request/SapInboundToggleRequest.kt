package com.otoki.powersales.admin.dto.request

/**
 * SAP 인바운드 endpoint 처리 활성/비활성 변경 요청.
 *
 * @property endpointPath 카탈로그의 endpointPath (예: `/api/v1/sap/account`)
 * @property enabled true = 활성(기존 처리) / false = 비활성(적재 생략 + 정상 응답)
 */
data class SapInboundToggleRequest(
    val endpointPath: String,
    val enabled: Boolean,
)
