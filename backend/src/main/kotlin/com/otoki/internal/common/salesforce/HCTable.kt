package com.otoki.internal.common.salesforce

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class HCTable(val value: String)
