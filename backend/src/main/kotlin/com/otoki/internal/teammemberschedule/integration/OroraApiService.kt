package com.otoki.internal.teammemberschedule.integration

/**
 * Orora WorkReport 외부 API 연동 인터페이스
 *
 * 레거시 출근 등록의 실제 저장소는 Orora 외부 API (POST {rootUrl}WorkReport).
 * 본 인터페이스만 정의하고, Mock/실 구현을 분리한다.
 */
interface OroraApiService {

    /**
     * 출근 보고 전송
     * @param scheduleSfid 스케줄 sfid
     * @param reason 출근 사유
     * @return WorkReport 전송 결과
     */
    fun sendWorkReport(scheduleSfid: String, reason: String? = null): OroraWorkReportResult
}

/**
 * Orora WorkReport 전송 결과
 */
data class OroraWorkReportResult(
    val resultCode: String,
    val resultMessage: String
)
