package com.otoki.powersales.sfmigration.service

import io.mockk.mockk
import jakarta.persistence.EntityManager
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.transaction.support.TransactionTemplate

/**
 * SQL builder 단위 테스트 — 실제 DB 실행 없이 buildChunkUpdateSql 의 결과 SQL 명제를 검증.
 *
 * 통합 테스트는 H2 의 UPDATE FROM + LEFT JOIN 미지원으로 작성하지 않음 (실제 PostgreSQL dev 환경에서 수동 검증).
 */
@DisplayName("SfMigrationStage2FkService SQL builder 단위 테스트")
class SfMigrationStage2FkServiceTest {

    private val service = SfMigrationStage2FkService(
        em = mockk<EntityManager>(),
        transactionTemplate = mockk<TransactionTemplate>(),
        progress = SfFkResolveProgress(),
    )

    @Test
    @DisplayName("단일 FK — UPDATE FROM + LEFT JOIN 1개 + WHERE 절 NULL 조건 포함")
    fun singleFk() {
        val plan = SfMigrationStage2FkService.TablePlan(
            columns = listOf(
                SfMigrationStage2FkService.ColumnPlan(
                    sfidColumn = "created_by_sfid",
                    spec = FkResolveSpec(
                        sfidColumn = "created_by_sfid",
                        idColumn = "created_by_id",
                        refTable = "user",
                        refIdColumn = "user_id",
                    ),
                    joinAlias = "j_created_by",
                ),
            ),
            polymorphicOwner = false,
            pkColumn = "simple_child_id",
        )

        val sql = service.buildChunkUpdateSql("simple_child", plan)

        assertThat(sql).contains("UPDATE powersales.simple_child t")
        assertThat(sql).contains("created_by_id = COALESCE(t.created_by_id, j_created_by.user_id)")
        assertThat(sql).contains("FROM powersales.simple_child src")
        assertThat(sql).contains("LEFT JOIN powersales.\"user\" j_created_by ON j_created_by.sfid = src.created_by_sfid")
        assertThat(sql).contains("WHERE t.simple_child_id = src.simple_child_id")
        assertThat(sql).contains("src.simple_child_id > :lastPk")
        assertThat(sql).contains("src.simple_child_id <= :upperPk")
        assertThat(sql).contains("(t.created_by_id IS NULL AND src.created_by_sfid IS NOT NULL)")
    }

    @Test
    @DisplayName("다중 FK — fullscan 1번에 3개 FK 동시 처리 (LEFT JOIN 3개 + WHERE OR 3개)")
    fun multipleFk() {
        val plan = SfMigrationStage2FkService.TablePlan(
            columns = listOf(
                SfMigrationStage2FkService.ColumnPlan(
                    sfidColumn = "erp_order_sfid",
                    spec = FkResolveSpec("erp_order_sfid", "erp_order_id", "erp_order", "erp_order_id"),
                    joinAlias = "j_erp_order",
                ),
                SfMigrationStage2FkService.ColumnPlan(
                    sfidColumn = "created_by_sfid",
                    spec = FkResolveSpec("created_by_sfid", "created_by_id", "user", "user_id"),
                    joinAlias = "j_created_by",
                ),
                SfMigrationStage2FkService.ColumnPlan(
                    sfidColumn = "last_modified_by_sfid",
                    spec = FkResolveSpec("last_modified_by_sfid", "last_modified_by_id", "user", "user_id"),
                    joinAlias = "j_last_modified_by",
                ),
            ),
            polymorphicOwner = false,
            pkColumn = "erp_order_product_id",
        )

        val sql = service.buildChunkUpdateSql("erp_order_product", plan)

        // SET 절 — 3개 컬럼 모두 COALESCE 로 채움.
        assertThat(sql).contains("erp_order_id = COALESCE(t.erp_order_id, j_erp_order.erp_order_id)")
        assertThat(sql).contains("created_by_id = COALESCE(t.created_by_id, j_created_by.user_id)")
        assertThat(sql).contains("last_modified_by_id = COALESCE(t.last_modified_by_id, j_last_modified_by.user_id)")

        // LEFT JOIN — ref table 별 3개.
        assertThat(sql).contains("LEFT JOIN powersales.erp_order j_erp_order ON j_erp_order.sfid = src.erp_order_sfid")
        assertThat(sql).contains("LEFT JOIN powersales.\"user\" j_created_by ON j_created_by.sfid = src.created_by_sfid")
        assertThat(sql).contains("LEFT JOIN powersales.\"user\" j_last_modified_by ON j_last_modified_by.sfid = src.last_modified_by_sfid")

        // WHERE OR — 3개 NULL 조건 (멱등성 보장).
        assertThat(sql).contains("(t.erp_order_id IS NULL AND src.erp_order_sfid IS NOT NULL)")
        assertThat(sql).contains("(t.created_by_id IS NULL AND src.created_by_sfid IS NOT NULL)")
        assertThat(sql).contains("(t.last_modified_by_id IS NULL AND src.last_modified_by_sfid IS NOT NULL)")
    }

    @Test
    @DisplayName("polymorphic owner_sfid — 005 → owner_user_id, 00G → owner_group_id 분기 SET")
    fun polymorphicOwner() {
        val plan = SfMigrationStage2FkService.TablePlan(
            columns = emptyList(),
            polymorphicOwner = true,
            pkColumn = "promotion_id",
        )

        val sql = service.buildChunkUpdateSql("promotion", plan)

        // SET 절 — owner_user_id / owner_group_id 둘 다 CASE 분기.
        assertThat(sql).contains(
            "owner_user_id = CASE WHEN src.owner_sfid LIKE '005%' " +
                "THEN COALESCE(t.owner_user_id, j_owner_user.user_id) ELSE t.owner_user_id END"
        )
        assertThat(sql).contains(
            "owner_group_id = CASE WHEN src.owner_sfid LIKE '00G%' " +
                "THEN COALESCE(t.owner_group_id, j_owner_group.group_id) ELSE t.owner_group_id END"
        )

        // LEFT JOIN — User 와 Group 각각 prefix 조건 포함.
        assertThat(sql).contains(
            "LEFT JOIN powersales.\"user\" j_owner_user " +
                "ON j_owner_user.sfid = src.owner_sfid AND src.owner_sfid LIKE '005%'"
        )
        assertThat(sql).contains(
            "LEFT JOIN powersales.\"group\" j_owner_group " +
                "ON j_owner_group.sfid = src.owner_sfid AND src.owner_sfid LIKE '00G%'"
        )

        // WHERE OR — polymorphic 별도 조건 2개.
        assertThat(sql).contains("(t.owner_user_id IS NULL AND src.owner_sfid LIKE '005%')")
        assertThat(sql).contains("(t.owner_group_id IS NULL AND src.owner_sfid LIKE '00G%')")
    }

    @Test
    @DisplayName("polymorphic 테이블에서 owner_sfid 일반 FK 매핑 (created_by 'owner') 은 polymorphic 블록에 위임 — 중복 SET 금지")
    fun polymorphicSkipsRegularOwnerColumn() {
        val plan = SfMigrationStage2FkService.TablePlan(
            columns = listOf(
                // deriveFkResolveSpec("owner_sfid") 가 만든 일반 user FK (owner_user_id) — polymorphic 처리 시 skip 되어야 함.
                SfMigrationStage2FkService.ColumnPlan(
                    sfidColumn = "owner_sfid",
                    spec = FkResolveSpec("owner_sfid", "owner_user_id", "user", "user_id"),
                    joinAlias = "j_owner",
                ),
                SfMigrationStage2FkService.ColumnPlan(
                    sfidColumn = "created_by_sfid",
                    spec = FkResolveSpec("created_by_sfid", "created_by_id", "user", "user_id"),
                    joinAlias = "j_created_by",
                ),
            ),
            polymorphicOwner = true,
            pkColumn = "promotion_id",
        )

        val sql = service.buildChunkUpdateSql("promotion", plan)

        // owner_sfid 일반 FK alias (j_owner) 는 SQL 에 나타나지 않아야 함.
        assertThat(sql).doesNotContain("j_owner.user_id")
        assertThat(sql).doesNotContain("LEFT JOIN powersales.\"user\" j_owner ")

        // created_by 는 정상 포함.
        assertThat(sql).contains("created_by_id = COALESCE(t.created_by_id, j_created_by.user_id)")

        // polymorphic 블록은 동작.
        assertThat(sql).contains("j_owner_user.user_id")
        assertThat(sql).contains("j_owner_group.group_id")
    }

    @Test
    @DisplayName("PK chunk 페이징 파라미터 — src.<pk> > :lastPk AND <= :upperPk")
    fun chunkPaging() {
        val plan = SfMigrationStage2FkService.TablePlan(
            columns = listOf(
                SfMigrationStage2FkService.ColumnPlan(
                    sfidColumn = "created_by_sfid",
                    spec = FkResolveSpec("created_by_sfid", "created_by_id", "user", "user_id"),
                    joinAlias = "j_created_by",
                ),
            ),
            polymorphicOwner = false,
            pkColumn = "erp_order_product_id",
        )

        val sql = service.buildChunkUpdateSql("erp_order_product", plan)

        assertThat(sql).contains("t.erp_order_product_id = src.erp_order_product_id")
        assertThat(sql).contains("src.erp_order_product_id > :lastPk")
        assertThat(sql).contains("src.erp_order_product_id <= :upperPk")
    }

    @Test
    @DisplayName("polymorphicRelated — Group.related_sfid prefix 005/00E 분기 (spec #782 P2-B)")
    fun polymorphicRelatedGroup() {
        // group 테이블 — related_sfid + related_user_id + related_user_role_id 의 polymorphic 분기.
        // related 컬럼은 deriveFkResolveSpec 에서 SKIP_FK_PREFIXES 로 제외되어 columns 비어있고
        // polymorphicRelated 분기로만 SQL 생성.
        val plan = SfMigrationStage2FkService.TablePlan(
            columns = emptyList(),
            polymorphicOwner = false,
            pkColumn = "group_id",
            polymorphicRelated = true,
        )

        val sql = service.buildChunkUpdateSql("group", plan)

        assertThat(sql).contains("UPDATE powersales.\"group\" t")
        // User 분기 (005 prefix → related_user_id)
        assertThat(sql).contains("related_user_id = CASE WHEN src.related_sfid LIKE '005%'")
        assertThat(sql).contains("j_related_user.user_id")
        assertThat(sql).contains("LEFT JOIN powersales.\"user\" j_related_user")
        // UserRole 분기 (00E prefix → related_user_role_id)
        assertThat(sql).contains("related_user_role_id = CASE WHEN src.related_sfid LIKE '00E%'")
        assertThat(sql).contains("j_related_user_role.user_role_id")
        assertThat(sql).contains("LEFT JOIN powersales.user_role j_related_user_role")
        // WHERE 절 — 두 분기의 NULL 조건
        assertThat(sql).contains("(t.related_user_id IS NULL AND src.related_sfid LIKE '005%')")
        assertThat(sql).contains("(t.related_user_role_id IS NULL AND src.related_sfid LIKE '00E%')")
    }
}
