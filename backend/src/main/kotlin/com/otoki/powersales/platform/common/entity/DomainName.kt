package com.otoki.powersales.platform.common.entity

/**
 * 엔티티의 한글 도메인명을 부여하는 어노테이션.
 *
 * 클래스명만으로는 비즈니스 도메인을 명확히 구분하기 어려워, 자연어로 비즈니스 로직을
 * 기술할 때 활용할 표준 한글 이름을 엔티티에 직접 부착한다.
 *
 * 예) `@DomainName("거래처")` — Account 엔티티의 표준 한글 도메인명은 "거래처".
 *
 * 모든 `@Entity` 클래스(mapped superclass 제외)는 본 어노테이션을 보유해야 하며,
 * 누락/중복 여부는 `DomainNameCoverageTest` 가 강제한다.
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class DomainName(val value: String)
