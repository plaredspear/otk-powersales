package com.otoki.internal.common.salesforce

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class SFObject(val value: String)
