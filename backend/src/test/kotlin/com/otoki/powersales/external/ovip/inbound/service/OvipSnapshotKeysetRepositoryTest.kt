package com.otoki.powersales.external.ovip.inbound.service

import com.otoki.powersales.domain.foundation.account.entity.Account
import com.otoki.powersales.domain.foundation.account.repository.AccountRepository
import com.otoki.powersales.external.ovip.inbound.dto.AccountRow
import com.otoki.powersales.platform.common.config.QueryDslConfig
import jakarta.persistence.EntityManager
import org.hibernate.Session
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles

/**
 * OVIP 전량 스냅샷 keyset 조회의 DB 레벨 정합 + **N+1 회귀 방어**.
 *
 * 서비스 단위 테스트([OvipAccountQueryServiceTest])는 repository 를 mock 으로 대체하므로 아래 두
 * 가지를 잡지 못한다.
 * - keyset 커서 / soft delete 제외가 실제 쿼리에서 동작하는지
 * - **LAZY 관계가 row 변환 과정에서 초기화되지 않는지** (초기화되면 row 당 추가 SELECT → N+1)
 *
 * [AccountRow] 는 관계 FK 를 쿼리에서 함께 select 한 값으로 채우므로 entity 의 LAZY 필드를 건드리지
 * 않는다. 다만 향후 누군가 `account.parent.name` 같은 참조를 무심코 추가하면 컴파일도 통과하고 기존
 * 단위 테스트도 깨지지 않은 채 N+1 이 부활한다. 본 테스트가 그 방어선이다.
 *
 * 판정은 `isLoaded` 가 아니라 **실제 실행 쿼리 수**로 한다 — `isLoaded` 는 연관 대상이 이미 영속성
 * 컨텍스트에 있으면 쿼리가 나가지 않았어도 true 라 N+1 판정 근거가 되지 못한다.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@ActiveProfiles("test")
@Import(QueryDslConfig::class)
class OvipSnapshotKeysetRepositoryTest {

    @Autowired
    private lateinit var testEntityManager: TestEntityManager

    @Autowired
    private lateinit var entityManager: EntityManager

    @Autowired
    private lateinit var accountRepository: AccountRepository

    @Nested
    @DisplayName("거래처 스냅샷")
    inner class AccountSnapshot {

        @Test
        @DisplayName("row 변환까지 마쳐도 관계는 미초기화 — FK 는 쿼리에서 함께 조회")
        fun relationsNeverInitialized() {
            // Given — parent 를 가진 거래처 (관계가 실제로 존재해야 프록시 초기화 여부가 의미를 갖는다)
            val parent = testEntityManager.persist(Account(name = "본사", externalKey = "P-1"))
            testEntityManager.persist(Account(name = "지점", externalKey = "C-1", parent = parent))
            testEntityManager.flush()
            testEntityManager.clear()

            // When — 운영과 동일 경로: keyset 조회 → DTO 변환
            val snapshots = accountRepository.findSnapshotByKeyset(null, 10)
            val rows = snapshots.map { AccountRow.from(it) }

            // Then — 관계를 로드하지 않고도 FK id 를 얻는다
            val child = rows.single { it.externalKey == "C-1" }
            assertThat(child.parentId).isEqualTo(parent.id)
            assertThat(snapshots).hasSize(2)
        }

        @Test
        @DisplayName("스냅샷 조회 + row 변환 전체가 쿼리 1회 — 관계/건수와 무관하게 N+1 없음")
        fun singleQueryRegardlessOfRowCount() {
            // Given — **서로 다른** parent 를 가진 거래처 5건.
            // parent 를 공유하면 첫 1건 로드 후 나머지는 영속성 컨텍스트 캐시에 적중해 N+1 이 가려진다.
            (1..5).forEach {
                val p = testEntityManager.persist(Account(name = "본사$it", externalKey = "PP-$it"))
                testEntityManager.persist(Account(name = "지점$it", externalKey = "CC-$it", parent = p))
            }
            testEntityManager.flush()
            testEntityManager.clear()

            val stats = entityManager.unwrap(Session::class.java).sessionFactory.statistics
            stats.isStatisticsEnabled = true
            stats.clear()

            // When — 운영과 동일 경로: keyset 조회 → DTO 변환
            val rows = accountRepository.findSnapshotByKeyset(null, 10).map { AccountRow.from(it) }

            // Then — 10건을 변환해도 쿼리는 1회 (관계마다 추가 SELECT 가 나가면 여기서 깨진다)
            assertThat(rows).hasSize(10)
            assertThat(stats.prepareStatementCount)
                .`as`("스냅샷 조회 + 변환은 쿼리 1회여야 한다 (관계 초기화로 인한 N+1 방지)")
                .isEqualTo(1)
        }

        @Test
        @DisplayName("cursor 초과분만 조회 + is_deleted=true 는 제외")
        fun keysetAndSoftDelete() {
            val first = testEntityManager.persist(Account(name = "가", externalKey = "A-1"))
            val second = testEntityManager.persist(Account(name = "나", externalKey = "A-2"))
            testEntityManager.persist(Account(name = "다", externalKey = "A-3", isDeleted = true))
            testEntityManager.flush()
            testEntityManager.clear()

            val all = accountRepository.findSnapshotByKeyset(null, 10)
            val afterFirst = accountRepository.findSnapshotByKeyset(first.id, 10)

            assertThat(all.map { it.account.id }).containsExactly(first.id, second.id)
            assertThat(afterFirst.map { it.account.id }).containsExactly(second.id)
        }

        @Test
        @DisplayName("limit 이 조회 건수를 제한한다 (hasNext 판정 근거)")
        fun limitApplies() {
            (1..5).forEach { testEntityManager.persist(Account(name = "거래처$it", externalKey = "L-$it")) }
            testEntityManager.flush()
            testEntityManager.clear()

            assertThat(accountRepository.findSnapshotByKeyset(null, 3)).hasSize(3)
        }
    }
}
