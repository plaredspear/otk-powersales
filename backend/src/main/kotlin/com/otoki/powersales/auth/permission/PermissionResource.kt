package com.otoki.powersales.auth.permission

/**
 * JPA entity 가 없는 가상 권한 자원 등록 어노테이션 (spec #808).
 *
 * `@RequiresSfPermission(entity = ...)` 가드 시 다음 3종을 자원 식별자로 표현 가능:
 *
 *  - `@SFObject` 부착 JPA entity → `@Table(name)` 자동 등록 (SF objectPermissions 대응)
 *  - `@SFObject` 미부착 JPA entity → `@Table(name)` 자동 등록 (우리 자체 자원)
 *  - JPA entity 가 없는 가상 자원 (Dashboard 집계 / 외부 시스템 proxy / 통합 endpoint) → 본 어노테이션으로 명시 등록
 *
 * 본 어노테이션은 컨트롤러 클래스 또는 service 클래스에 부착한다.
 * [PermissionResourceRegistry] 가 부팅 시 Spring `ApplicationContext` 에서 본 어노테이션 부착 bean 을
 * 스캔하여 권한 자원 카탈로그에 합집합 등록.
 *
 * SF metadata 모델로는 `CustomPermission` 과 1:1 대응 (예: `@PermissionResource("dashboard")`
 * ↔ SF `View_Dashboard.customPermission-meta.xml`). 향후 SF metadata 양방향 sync 가능.
 *
 * @property name 자원 식별자. snake_case 단수형 (기존 entity 컨벤션 정합).
 *                `@RequiresSfPermission(entity = name)` 의 `entity` 값과 매칭.
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class PermissionResource(
    val name: String,
)
