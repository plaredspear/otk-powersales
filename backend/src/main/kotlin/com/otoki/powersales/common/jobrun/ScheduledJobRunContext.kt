package com.otoki.powersales.common.jobrun

/**
 * `ScheduledJobRunner.run` 의 람다에 주입되는 보조 객체.
 *
 * 명명 근거: Spring Batch 의 `JobExecutionContext` 와 import 혼동을 피하기 위해
 * 도메인 접두사를 붙였다. 본 프로젝트는 Spring Batch 미도입이지만 향후 도입 시
 * 충돌이 없도록 한다.
 */
class ScheduledJobRunContext internal constructor(
    val jobName: String
) {
    @Volatile
    private var pendingMetadata: Map<String, Any?>? = null

    /**
     * 종료 시 `metadata` 컬럼에 기록될 값을 등록한다. 여러 번 호출 시 마지막 값이 유지된다.
     */
    fun metadata(value: Map<String, Any?>) {
        this.pendingMetadata = value
    }

    internal fun pendingMetadata(): Map<String, Any?>? = pendingMetadata
}
