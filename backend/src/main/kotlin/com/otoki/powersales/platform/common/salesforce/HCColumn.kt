package com.otoki.powersales.platform.common.salesforce

@Target(AnnotationTarget.FIELD)
@Retention(AnnotationRetention.RUNTIME)
annotation class HCColumn(val value: String)
