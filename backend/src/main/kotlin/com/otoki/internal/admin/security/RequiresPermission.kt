package com.otoki.internal.admin.security

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class RequiresPermission(vararg val value: AdminPermission)
