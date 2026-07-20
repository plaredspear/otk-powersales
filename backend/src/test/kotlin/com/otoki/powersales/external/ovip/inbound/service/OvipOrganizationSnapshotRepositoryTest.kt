package com.otoki.powersales.external.ovip.inbound.service

import com.otoki.powersales.domain.org.organization.entity.Organization
import com.otoki.powersales.domain.org.organization.repository.OrganizationRepository
import com.otoki.powersales.external.ovip.inbound.dto.OrganizationRow
import com.otoki.powersales.platform.common.config.QueryDslConfig
import jakarta.persistence.EntityManager
import org.hibernate.Session
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles

/**
 * OVIP 조직 전량 스냅샷 조회의 DB 레벨 정합 + **N+1 회귀 방어**.
 *
 * 커서 페이지네이션 계열([OvipSnapshotKeysetRepositoryTest]) 과 분리한 이유는 조직 조회가 keyset 을
 * 쓰지 않기 때문 — 검증 대상이 "커서/limit 동작" 이 아니라 "전건이 단일 쿼리로 나오는가" 다.
 *
 * 서비스 단위 테스트([OvipOrganizationQueryServiceTest])는 repository 를 mock 으로 대체하므로 아래 두
 * 가지를 잡지 못한다.
 * - soft delete 제외가 실제 쿼리에서 동작하는지
 * - **LAZY 관계가 row 변환 과정에서 초기화되지 않는지** (초기화되면 row 당 추가 SELECT → N+1)
 *
 * 판정은 `isLoaded` 가 아니라 **실제 실행 쿼리 수**로 한다 — `isLoaded` 는 연관 대상이 이미 영속성
 * 컨텍스트에 있으면 쿼리가 나가지 않았어도 true 라 N+1 판정 근거가 되지 못한다.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@ActiveProfiles("test")
@Import(QueryDslConfig::class)
class OvipOrganizationSnapshotRepositoryTest {

    @Autowired
    private lateinit var testEntityManager: TestEntityManager

    @Autowired
    private lateinit var entityManager: EntityManager

    @Autowired
    private lateinit var organizationRepository: OrganizationRepository

    @Test
    @DisplayName("페이지네이션 없이 전건 반환 + is_deleted=true 는 제외")
    fun returnsEveryRowExceptDeleted() {
        // Given — 활성 3건 + 삭제 1건
        (1..3).forEach {
            testEntityManager.persist(Organization(name = "조직$it", externalKey = "O-$it"))
        }
        testEntityManager.persist(Organization(name = "폐지조직", externalKey = "O-X", isDeleted = true))
        testEntityManager.flush()
        testEntityManager.clear()

        // When
        val snapshots = organizationRepository.findAllSnapshot()

        // Then — 삭제분만 빠지고 나머지는 전건
        assertThat(snapshots.map { it.organization.externalKey })
            .containsExactlyInAnyOrder("O-1", "O-2", "O-3")
    }

    @Test
    @DisplayName("id 오름차순으로 정렬되어 반환된다")
    fun orderedById() {
        (1..5).forEach {
            testEntityManager.persist(Organization(name = "조직$it", externalKey = "S-$it"))
        }
        testEntityManager.flush()
        testEntityManager.clear()

        val ids = organizationRepository.findAllSnapshot().map { it.organization.id }

        assertThat(ids).isSorted
    }

    @Test
    @DisplayName("전건 조회 + row 변환 전체가 쿼리 1회 — 건수와 무관하게 N+1 없음")
    fun singleQueryRegardlessOfRowCount() {
        // Given — **서로 다른** owner 를 가진 조직 10건.
        // owner 를 공유하면 첫 1건 로드 후 나머지는 영속성 컨텍스트 캐시에 적중해 N+1 이 가려진다.
        (1..10).forEach {
            testEntityManager.persist(
                Organization(name = "조직$it", externalKey = "N-$it").apply {
                    ownerSfid = "005OWNER$it"
                }
            )
        }
        testEntityManager.flush()
        testEntityManager.clear()

        val stats = entityManager.unwrap(Session::class.java).sessionFactory.statistics
        stats.isStatisticsEnabled = true
        stats.clear()

        // When — 운영과 동일 경로: 전량 조회 → DTO 변환
        val rows = organizationRepository.findAllSnapshot().map { OrganizationRow.from(it) }

        // Then — 10건을 변환해도 쿼리는 1회 (관계마다 추가 SELECT 가 나가면 여기서 깨진다)
        assertThat(rows).hasSize(10)
        assertThat(stats.prepareStatementCount)
            .`as`("전량 조회 + 변환은 쿼리 1회여야 한다 (관계 초기화로 인한 N+1 방지)")
            .isEqualTo(1)
    }
}
