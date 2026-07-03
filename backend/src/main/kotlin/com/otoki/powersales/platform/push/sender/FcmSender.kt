package com.otoki.powersales.platform.push.sender

/**
 * FCM(Firebase Cloud Messaging) 푸시 발송 추상화.
 *
 * 발송 transport (Firebase Admin SDK HTTP v1) 를 캡슐화한다. 운영(`!local`) 은 [RealFcmSender],
 * 로컬(`local`) 은 [StubFcmSender] 가 바인딩된다.
 */
interface FcmSender {

    /**
     * 다수 디바이스 토큰에 동일 notification 푸시를 발송한다.
     *
     * @param tokens 대상 FCM 디바이스 토큰 목록 (빈 목록이면 발송 없이 0 반환)
     * @param title notification 제목
     * @param body notification 본문
     * @param data 알림 탭 시 딥링크 라우팅에 쓰이는 data payload (예: {"type":"notice","noticeId":"12"}). 기본 빈 맵.
     * @return 성공/실패 건수 집계
     */
    fun sendNotificationToTokens(
        tokens: List<String>,
        title: String,
        body: String,
        data: Map<String, String> = emptyMap(),
    ): FcmSendResult
}

/**
 * 발송 결과 집계.
 *
 * @property successCount 발송 성공 토큰 수
 * @property failureCount 발송 실패 토큰 수 (무효 토큰 등)
 */
data class FcmSendResult(
    val successCount: Int,
    val failureCount: Int,
) {
    companion object {
        val EMPTY = FcmSendResult(0, 0)
    }
}
