package com.otoki.powersales.external.rdp.inbound.config

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * RDP 인바운드 조회 API 설정.
 *
 * @property mfeis 월별 여사원 통합일정(MFEIS) 조회 페이지네이션 설정
 */
@ConfigurationProperties(prefix = "rdp.inbound")
data class RdpInboundProperties(
    val mfeis: Mfeis = Mfeis()
) {
    /**
     * @property defaultPageSize cursor 미지정 size 요청 시 기본 페이지 크기
     * @property maxPageSize 클라이언트 요청 size 상한 (초과 시 이 값으로 clamp)
     */
    data class Mfeis(
        val defaultPageSize: Int = 100,
        val maxPageSize: Int = 1000
    )
}
