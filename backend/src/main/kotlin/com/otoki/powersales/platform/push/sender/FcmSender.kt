package com.otoki.powersales.platform.push.sender

/**
 * FCM(Firebase Cloud Messaging) 푸시 발송 추상화.
 *
 * 발송 transport (Firebase Admin SDK HTTP v1) 를 캡슐화한다. 운영(`!local`) 은 [RealFcmSender],
 * 로컬(`local`) 은 [StubFcmSender] 가 바인딩된다.
 */
interface FcmSender {

    /**
     * 다수 디바이스에 동일 notification 푸시를 발송한다 (배지 값만 대상별로 다름).
     *
     * @param targets 대상 디바이스 토큰 + 표시할 배지 값 (빈 목록이면 발송 없이 0 반환)
     * @param title notification 제목
     * @param body notification 본문
     * @param data 알림 탭 시 딥링크 라우팅에 쓰이는 data payload (예: {"type":"notice","noticeId":"12"}). 기본 빈 맵.
     * @return 성공/실패 건수 집계
     */
    fun sendNotification(
        targets: List<PushTarget>,
        title: String,
        body: String,
        data: Map<String, String> = emptyMap(),
    ): FcmSendResult
}

/**
 * 발송 1건의 수신 대상.
 *
 * @property token 대상 디바이스 FCM 토큰
 * @property badge 앱 아이콘에 표시할 배지 절대값. null 이면 배지 페이로드를 싣지 않는다
 *   (iOS 는 기존 배지 유지, Android 는 런처 기본 동작). APNs `aps.badge` 는 증분이 아니라
 *   "표시할 값" 이므로 서버가 계산한 누적 값을 그대로 넣는다
 *   ([com.otoki.powersales.platform.push.service.PushBadgeService] 참고).
 */
data class PushTarget(
    val token: String,
    val badge: Int? = null,
)

/**
 * 발송 결과 집계.
 *
 * @property successCount 발송 성공 토큰 수
 * @property failureCount 발송 실패 토큰 수 (무효 토큰 등)
 * @property unregisteredTokens FCM 이 `UNREGISTERED` 로 응답한 토큰 목록 — 앱 삭제/기기 초기화/
 *   강제 로그아웃 시 단말이 폐기(`deleteToken`)한 토큰이다. 해당 토큰은 다시는 도달하지 않으므로
 *   저장소에서 제거해야 한다 ([RealFcmSender] 가 발송 직후 정리하며, 집계는 관측/검증용으로 노출).
 */
data class FcmSendResult(
    val successCount: Int,
    val failureCount: Int,
    val unregisteredTokens: List<String> = emptyList(),
) {
    companion object {
        val EMPTY = FcmSendResult(0, 0)
    }
}
