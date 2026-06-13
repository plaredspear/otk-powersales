package com.otoki.powersales.external.sap.auth.sanity

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class SapDestructiveEndpoint(
    /**
     * 직전 적재 건수 대비 허용 변동 폭 (%). 기본 20.
     */
    val threshold: Int = 20,

    /**
     * 받은 건수를 측정할 메서드 인자명. 비워두면 첫 번째 Collection 인자를 자동 탐지.
     */
    val countArgName: String = ""
)
