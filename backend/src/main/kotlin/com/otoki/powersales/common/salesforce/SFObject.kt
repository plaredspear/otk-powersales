package com.otoki.powersales.common.salesforce

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class SFObject(val value: String)
