package com.otoki.powersales.admin.security

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class RequiresPermission(vararg val value: AdminPermission)
