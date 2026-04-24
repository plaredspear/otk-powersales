package com.otoki.powersales.common.sap

@Target(AnnotationTarget.FIELD)
@Retention(AnnotationRetention.RUNTIME)
annotation class SAPUpsertKey(
    val composite: Boolean = false,
    val components: Array<String> = []
)
