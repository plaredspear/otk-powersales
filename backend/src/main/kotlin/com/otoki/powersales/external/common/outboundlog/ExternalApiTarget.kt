package com.otoki.powersales.external.common.outboundlog

/**
 * [ExternalApiLog.targetSystem] 에 적재되는 외부 시스템 분류 상수.
 *
 * 신규 외부 HTTP 연동 추가 시 여기에 상수를 추가하고 해당 RestClient builder 에
 * [ExternalApiLogInterceptor] 를 등록한다.
 */
object ExternalApiTarget {
    const val SAP = "SAP"
    const val SF = "SF"
    const val NAVER = "NAVER"
}
