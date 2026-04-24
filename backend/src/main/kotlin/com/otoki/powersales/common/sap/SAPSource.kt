package com.otoki.powersales.common.sap

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class SAPSource(
    val api: String,
    val sfObject: String = "",
    val syncMode: SyncMode
)
