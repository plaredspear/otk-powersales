package com.otoki.powersales.platform.common.entity

/**
 * 엔티티 필드(컬럼)의 한글 이름을 부여하는 어노테이션.
 *
 * 컬럼명/프로퍼티명만으로는 의미를 명확히 알기 어려운 필드에 대해, 자연어로 비즈니스
 * 로직을 기술할 때 활용할 표준 한글 이름을 필드에 직접 부착한다. 클래스 레벨
 * [DomainName] 의 필드 레벨 대응이다.
 *
 * 예) `@FieldName("대표자명")` — Account.representative 필드의 한글명은 "대표자명".
 *
 * 부여 대상은 비즈니스 의미가 있는 컬럼에 한정한다. id, sfid, owner, created/updated
 * 등 공통 기술 컬럼에는 부여하지 않는다. 부여된 필드의 한글명 중복(엔티티 내) 여부는
 * `FieldNameCoverageTest` 가 검증한다.
 */
@Target(AnnotationTarget.FIELD, AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.RUNTIME)
annotation class FieldName(val value: String)
