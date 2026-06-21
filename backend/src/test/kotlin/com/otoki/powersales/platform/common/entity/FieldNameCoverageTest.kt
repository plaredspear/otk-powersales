package com.otoki.powersales.platform.common.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider
import org.springframework.core.type.filter.AnnotationTypeFilter
import java.lang.reflect.Field

/**
 * `@FieldName` 한글 필드명의 정합성을 전수 검증한다.
 * - 같은 엔티티 안에서 한글명이 중복되지 않는다.
 * - `@FieldName` 값은 공백이 아니다.
 * - `@FieldName` 이 붙은 필드는 `@Column` 매핑 필드여야 한다(어노테이션 오부착 방지).
 *
 * 누락(coverage) 은 강제하지 않는다 — id, sfid, owner, created/updated 등 공통
 * 기술 컬럼은 의도적으로 제외하기 때문이다.
 */
@DisplayName("FieldName coverage")
class FieldNameCoverageTest {

    private val basePackages = listOf("com.otoki.powersales", "com.otoki.orora", "com.otoki.pos")

    private fun allEntityClasses(): List<Class<*>> {
        val scanner = ClassPathScanningCandidateComponentProvider(false).apply {
            addIncludeFilter(AnnotationTypeFilter(Entity::class.java))
        }
        return basePackages
            .flatMap { scanner.findCandidateComponents(it) }
            .mapNotNull { it.beanClassName }
            .filterNot { it.contains('$') } // 테스트 fixture nested class 제외
            .distinct()
            .map { Class.forName(it) }
    }

    /** 엔티티의 선언 필드 중 @FieldName 을 가진 (Field, 한글명) 목록. */
    private fun fieldNamesOf(cls: Class<*>): List<Pair<Field, String>> =
        cls.declaredFields.mapNotNull { f ->
            f.getAnnotation(FieldName::class.java)?.let { f to it.value }
        }

    @Test
    @DisplayName("@FieldName 한글명은 엔티티 안에서 중복되지 않는다")
    fun fieldNamesAreUniqueWithinEntity() {
        val violations = mutableListOf<String>()
        for (cls in allEntityClasses()) {
            val names = fieldNamesOf(cls).map { it.second }
            val dups = names.groupingBy { it }.eachCount().filterValues { it > 1 }.keys
            if (dups.isNotEmpty()) violations += "${cls.simpleName}: $dups"
        }
        assertThat(violations)
            .withFailMessage { "엔티티 내 @FieldName 한글명 중복: $violations" }
            .isEmpty()
    }

    @Test
    @DisplayName("@FieldName 값은 공백이 아니다")
    fun fieldNamesAreNotBlank() {
        val blank = allEntityClasses().flatMap { cls ->
            fieldNamesOf(cls).filter { it.second.isBlank() }.map { "${cls.simpleName}.${it.first.name}" }
        }
        assertThat(blank)
            .withFailMessage { "공백 @FieldName: $blank" }
            .isEmpty()
    }

    @Test
    @DisplayName("@FieldName 은 @Column 매핑 필드에만 부착된다")
    fun fieldNameOnlyOnColumns() {
        val misplaced = allEntityClasses().flatMap { cls ->
            fieldNamesOf(cls)
                .filter { it.first.getAnnotation(Column::class.java) == null }
                .map { "${cls.simpleName}.${it.first.name}" }
        }
        assertThat(misplaced)
            .withFailMessage { "@Column 없는 필드에 @FieldName 부착됨: $misplaced" }
            .isEmpty()
    }

    @Test
    @DisplayName("스캔이 엔티티를 실제로 잡았는지 가드")
    fun scanFindsEntities() {
        assertThat(allEntityClasses()).hasSizeGreaterThan(50)
    }
}
