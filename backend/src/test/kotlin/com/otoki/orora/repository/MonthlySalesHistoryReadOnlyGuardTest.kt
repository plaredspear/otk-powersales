package com.otoki.orora.repository

import com.otoki.orora.entity.OroraMonthlySalesHistory
import org.assertj.core.api.Assertions.assertThat
import org.hibernate.annotations.Immutable
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import kotlin.reflect.KMutableProperty
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.full.functions
import kotlin.reflect.full.hasAnnotation

/**
 * ORORA MonthlySalesHistory 절대 수정 불가 가드 검증 (DailySalesHistory 와 동일 정책).
 *
 * 본 테스트는 다음 가드의 회귀를 방지한다:
 * 1. Repository 인터페이스에 mutation API (save / delete) 미노출
 * 2. Entity 의 모든 필드가 `val` immutable
 * 3. Entity 에 `@org.hibernate.annotations.Immutable` 부착 (Hibernate dirty checking 스킵)
 */
@DisplayName("ORORA MonthlySalesHistory 절대 수정 불가 가드 검증")
class MonthlySalesHistoryReadOnlyGuardTest {

	@Test
	@DisplayName("OroraMonthlySalesHistoryRepository 가 mutation API (save / delete) 를 노출하지 않는다")
	fun `repository does not expose mutation api`() {
		val mutationMethods = listOf("save", "saveAll", "delete", "deleteAll", "deleteById", "deleteAllById")
		val exposedMutations = OroraMonthlySalesHistoryRepository::class.functions
			.map { it.name }
			.filter { it in mutationMethods }

		assertThat(exposedMutations)
			.withFailMessage(
				"OroraMonthlySalesHistoryRepository 가 mutation API 를 노출하고 있음: %s. " +
					"본 Repository 는 Spring Data marker `Repository<>` 만 상속해야 한다. " +
					"`JpaRepository` / `CrudRepository` 상속은 절대 수정 불가 정책 위반.",
				exposedMutations,
			)
			.isEmpty()
	}

	@Test
	@DisplayName("OroraMonthlySalesHistory entity 의 모든 필드가 val (immutable) 이다")
	fun `entity has only immutable val fields`() {
		val mutableProps = OroraMonthlySalesHistory::class.declaredMemberProperties
			.filter { it is KMutableProperty<*> }
			.map { it.name }

		assertThat(mutableProps)
			.withFailMessage(
				"OroraMonthlySalesHistory 가 mutable (var) 필드를 보유: %s. " +
					"본 entity 는 모든 필드 `val` 의무 (read-only 가드). " +
					"JPA dirty checking 으로 인한 의도치 않은 UPDATE 발사를 컴파일 시점에 차단해야 함.",
				mutableProps,
			)
			.isEmpty()
	}

	@Test
	@DisplayName("OroraMonthlySalesHistory entity 에 @Immutable 어노테이션이 부착되어 있다")
	fun `entity is annotated with Immutable`() {
		assertThat(OroraMonthlySalesHistory::class.hasAnnotation<Immutable>())
			.withFailMessage(
				"OroraMonthlySalesHistory 에 `@org.hibernate.annotations.Immutable` 어노테이션 부재. " +
					"본 어노테이션 부착 의무 — Hibernate 가 entity 를 read-only 로 " +
					"인식하여 dirty checking / flush 단계에서 스킵하도록 강제.",
			)
			.isTrue()
	}
}
