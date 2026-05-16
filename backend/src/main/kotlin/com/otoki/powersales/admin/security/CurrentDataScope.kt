package com.otoki.powersales.admin.security

/**
 * Admin controller 메서드 파라미터에 현재 인증된 사용자의
 * [com.otoki.powersales.admin.dto.DataScope] 를 직접 주입.
 *
 * 동작 흐름:
 *  1. [WebAdminContextFilter] 가 요청 진입 시 Employee 기반으로 DataScope 를 산출하여
 *     request attribute ([AdminContextAttributes.DATA_SCOPE]) 에 저장.
 *  2. [CurrentAdminContextArgumentResolver] 가 본 어노테이션이 부착된 파라미터를 발견하면
 *     동일 request attribute 에서 DataScope 를 꺼내 주입.
 *
 * DataScope 가 attribute 에 없으면 [IllegalStateException]. [CurrentEmployee] 와 동일하게
 * admin 인증 endpoint 한정으로만 부착할 것.
 */
@Target(AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.RUNTIME)
annotation class CurrentDataScope
