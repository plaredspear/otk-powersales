package com.otoki.powersales.admin.security

/**
 * Admin controller 메서드 파라미터에 현재 인증된 [com.otoki.powersales.employee.entity.Employee] 를
 * 직접 주입.
 *
 * 동작 흐름:
 *  1. [WebAdminContextFilter] 가 요청 진입 시 JWT principal 로부터 Employee 를 조회하여
 *     request attribute ([AdminContextAttributes.EMPLOYEE]) 에 저장.
 *  2. [CurrentAdminContextArgumentResolver] 가 본 어노테이션이 부착된 파라미터를 발견하면
 *     동일 request attribute 에서 Employee 를 꺼내 주입.
 *
 * holder (`@RequestScope` 빈) 대신 ArgumentResolver 를 사용하는 이유는 controller 가
 * ambient context 빈을 비인지 — Spring 권장 패턴 (`@AuthenticationPrincipal` 와 동형).
 *
 * Employee 가 attribute 에 없으면 (예: `/auth/login` 처럼 admin 컨텍스트 비통과 경로 또는
 * Employee 매핑 실패) [IllegalStateException] 발생. admin 인증 endpoint 한정으로만 부착할 것.
 */
@Target(AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.RUNTIME)
annotation class CurrentEmployee
