package com.otoki.internal.common.salesforce

@Target(AnnotationTarget.FIELD)
@Retention(AnnotationRetention.RUNTIME)
annotation class SFField(val value: String)
