package com.otoki.powersales.platform.common.entity

import jakarta.persistence.Entity
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider
import org.springframework.core.type.filter.AnnotationTypeFilter

/**
 * 모든 `@Entity` 클래스가 `@DomainName` 한글 도메인명을 보유하는지 + 한글명이 중복되지
 * 않는지 전수 검증한다. 신규 엔티티 추가 시 한글명 부여 누락을 부팅 전에 차단한다.
 */
@DisplayName("DomainName coverage")
class DomainNameCoverageTest {

    /** 본 프로젝트의 모든 JPA 엔티티가 위치하는 base package 들. */
    private val basePackages = listOf("com.otoki.powersales", "com.otoki.orora", "com.otoki.pos")

    private fun allEntityClasses(): List<Class<*>> {
        val scanner = ClassPathScanningCandidateComponentProvider(false).apply {
            addIncludeFilter(AnnotationTypeFilter(Entity::class.java))
        }
        return basePackages
            .flatMap { scanner.findCandidateComponents(it) }
            .mapNotNull { it.beanClassName }
            // 테스트 fixture 엔티티(테스트 클래스의 nested inner class)는 제외 — 운영 엔티티는 모두 top-level.
            .filterNot { it.contains('$') }
            .distinct()
            .map { Class.forName(it) }
    }

    @Test
    @DisplayName("모든 @Entity 는 @DomainName 한글 도메인명을 보유한다")
    fun everyEntityHasDomainName() {
        val entities = allEntityClasses()
        // 스캔이 실제로 엔티티를 잡았는지 가드 (오탐 방지)
        assertThat(entities).hasSizeGreaterThan(50)

        val missing = entities
            .filter { it.getAnnotation(DomainName::class.java) == null }
            .map { it.simpleName }
            .sorted()

        assertThat(missing)
            .withFailMessage { "다음 @Entity 에 @DomainName 누락: $missing" }
            .isEmpty()
    }

    @Test
    @DisplayName("@DomainName 한글 도메인명은 엔티티 간 중복되지 않는다")
    fun domainNamesAreUnique() {
        val byName = allEntityClasses()
            .mapNotNull { cls ->
                cls.getAnnotation(DomainName::class.java)?.value?.let { it to cls.simpleName }
            }
            .groupBy({ it.first }, { it.second })

        val duplicates = byName.filterValues { it.size > 1 }

        assertThat(duplicates)
            .withFailMessage { "중복된 @DomainName 한글명: $duplicates" }
            .isEmpty()
    }

    @Test
    @DisplayName("@DomainName 값은 공백이 아니다")
    fun domainNamesAreNotBlank() {
        val blank = allEntityClasses()
            .filter { it.getAnnotation(DomainName::class.java)?.value?.isBlank() == true }
            .map { it.simpleName }

        assertThat(blank)
            .withFailMessage { "공백 @DomainName 보유 엔티티: $blank" }
            .isEmpty()
    }
}
