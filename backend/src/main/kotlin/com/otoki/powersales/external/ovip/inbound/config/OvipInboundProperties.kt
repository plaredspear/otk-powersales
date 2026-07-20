package com.otoki.powersales.external.ovip.inbound.config

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * OVIP 인바운드 조회 API 설정.
 *
 * @property mfeis 월별 여사원 통합일정(MFEIS) 조회 페이지네이션 설정
 * @property account 거래처(Account) 전량 스냅샷 조회 페이지네이션 설정
 */
@ConfigurationProperties(prefix = "ovip.inbound")
data class OvipInboundProperties(
    val mfeis: Mfeis = Mfeis(),
    val account: Pagination = Pagination()
) {
    /**
     * @property defaultPageSize cursor 미지정 size 요청 시 기본 페이지 크기
     * @property maxPageSize 클라이언트 요청 size 상한 (초과 시 이 값으로 clamp)
     */
    data class Mfeis(
        val defaultPageSize: Int = 100,
        val maxPageSize: Int = 1000
    )

    /**
     * 마스터 전량 스냅샷 페이지네이션 설정 (거래처).
     *
     * MFEIS 대비 상한이 낮은 이유: 거래처 마스터는 entity 전 컬럼을 노출하므로 row 당 페이로드가
     * MFEIS projection(21개 필드) 보다 3배 이상 크다. 동일 상한을 쓰면 응답 크기가 과대해진다.
     *
     * @property defaultPageSize cursor 미지정 size 요청 시 기본 페이지 크기
     * @property maxPageSize 클라이언트 요청 size 상한 (초과 시 이 값으로 clamp)
     */
    data class Pagination(
        val defaultPageSize: Int = 100,
        val maxPageSize: Int = 500
    )
}
