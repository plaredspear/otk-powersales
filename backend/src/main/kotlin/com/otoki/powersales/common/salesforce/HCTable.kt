package com.otoki.powersales.common.salesforce

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class HCTable(val value: String)
