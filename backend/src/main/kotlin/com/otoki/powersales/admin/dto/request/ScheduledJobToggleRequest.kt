package com.otoki.powersales.admin.dto.request

/**
 * 스케줄 잡 런타임 활성/비활성 변경 요청.
 *
 * @property jobName 카탈로그의 jobName (예: `sap-outbox-worker`)
 * @property enabled true = 활성(본문 실행) / false = 비활성(SKIPPED 이력만 남기고 생략)
 */
data class ScheduledJobToggleRequest(
    val jobName: String,
    val enabled: Boolean,
)
