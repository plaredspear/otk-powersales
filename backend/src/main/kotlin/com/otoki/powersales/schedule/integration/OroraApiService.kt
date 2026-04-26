package com.otoki.powersales.schedule.integration

/**
 * Orora WorkReport 외부 API 연동 인터페이스
 *
 * 레거시 출근 등록의 실제 저장소는 Orora 외부 API (POST {rootUrl}WorkReport).
 * 본 인터페이스만 정의하고, Mock/실 구현을 분리한다.
 */
interface OroraApiService {

    /**
     * 출근 보고 전송
     * @param request 안전점검 데이터 포함 출근보고 요청
     * @return WorkReport 전송 결과
     */
    fun sendWorkReport(request: OroraWorkReportRequest): OroraWorkReportResult
}

/**
 * Orora WorkReport 전송 결과
 */
data class OroraWorkReportResult(
    val resultCode: String,
    val resultMessage: String
)
