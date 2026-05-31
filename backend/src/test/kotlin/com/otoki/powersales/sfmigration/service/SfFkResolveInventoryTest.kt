package com.otoki.powersales.sfmigration.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

/**
 * Stage 2-A FK Resolve — `*_sfid` 컬럼 인벤토리 회귀 테스트.
 *
 * 목적: 새 `*_sfid` 컬럼이 entity 에 추가되거나 매핑이 변경되면 이 테스트가 깨지도록 하여,
 *      "매핑 없이 자동추론(AUTO_INFERRED)으로 빠져 잘못된 ref 로 silent UPDATE" 되는
 *      위험(검증축 4 R1/R6)을 컴파일/CI 단계에서 잡는다.
 *
 * 인벤토리 출처: backend entity 의 `@Column(name = "..._sfid")` 전수 (2026-05-31 추출).
 *   - 갱신 방법:
 *       cd backend/src/main/kotlin
 *       find com -name '*.kt' -print0 | xargs -0 grep -hoE 'name = "[a-z_]+_sfid"' \
 *         | sed 's/name = "//;s/"//' | sort -u
 *   - 위 명령 결과가 EXPECTED 의 key set 과 일치해야 한다 (신규 컬럼 추가 시 EXPECTED 갱신 필수).
 *
 * 각 entry: sfidColumn → 기대 (FkResolutionKind, 기대 refTable or null).
 *   - SKIP            : null ref (polymorphic 전용 / 자연 키 substep / sfid 단독)
 *   - MAPPED          : FK_PREFIX_MAPPING 명시 ref
 *   - AUTO_INFERRED   : prefix == table 명인 도메인 FK (추론이 정확). ref = prefix.
 *
 * ⚠️ notice_employee_sfid 는 AUTO_INFERRED → ref "notice_employee" 로 잡히나, 이 테이블의
 *    notice_employee_id 는 PK 라 sfid lookup 으로 PK 를 덮을 수 없다. 실제 referent 가
 *    employee 인지 사용자 데이터 확인 대상 (DANGLING 으로 잔존할 가능성 — reportDangling 가 보고).
 *    현재 동작을 pin 해 두고, 데이터 확인 후 매핑 정정 시 본 기대값을 갱신한다.
 */
@DisplayName("FK Resolve — *_sfid 컬럼 인벤토리 회귀")
class SfFkResolveInventoryTest {

    /** (sfidColumn, sourceTable?) → 기대 분류 + 기대 refTable(null = SKIP). */
    private data class Expectation(val kind: FkResolutionKind, val refTable: String?)

    private val expected: Map<String, Expectation> = mapOf(
        // ── 도메인 FK (FK_PREFIX_MAPPING 명시 또는 prefix==table 자동추론) ──
        "account_sfid" to Expectation(FkResolutionKind.MAPPED, "account"),
        "employee_sfid" to Expectation(FkResolutionKind.MAPPED, "employee"),
        "product_sfid" to Expectation(FkResolutionKind.MAPPED, "product"),
        "product_code_sfid" to Expectation(FkResolutionKind.MAPPED, "product"),
        "promotion_sfid" to Expectation(FkResolutionKind.MAPPED, "promotion"),
        "promotion_employee_sfid" to Expectation(FkResolutionKind.MAPPED, "promotion_employee"),
        "new_product_sfid" to Expectation(FkResolutionKind.MAPPED, "new_product"),
        "agreement_word_sfid" to Expectation(FkResolutionKind.MAPPED, "agreement_word"),
        "order_request_sfid" to Expectation(FkResolutionKind.MAPPED, "order_request"),
        "erp_order_sfid" to Expectation(FkResolutionKind.MAPPED, "erp_order"),
        "push_message_sfid" to Expectation(FkResolutionKind.MAPPED, "push_message"),
        "display_work_schedule_sfid" to Expectation(FkResolutionKind.MAPPED, "display_work_schedule"),
        "team_member_schedule_sfid" to Expectation(FkResolutionKind.MAPPED, "team_member_schedule"),
        "monthly_female_employee_integration_schedule_sfid" to
            Expectation(FkResolutionKind.MAPPED, "monthly_female_employee_integration_schedule"),
        "employee_input_criteria_master_sfid" to
            Expectation(FkResolutionKind.MAPPED, "employee_input_criteria_master"),
        "profile_sfid" to Expectation(FkResolutionKind.MAPPED, "profile"),
        "user_role_sfid" to Expectation(FkResolutionKind.MAPPED, "user_role"),
        "parent_user_role_sfid" to Expectation(FkResolutionKind.MAPPED, "user_role"),
        "record_type_sfid" to Expectation(FkResolutionKind.MAPPED, "record_type"),

        // ── audit (User lookup) ──
        "created_by_sfid" to Expectation(FkResolutionKind.MAPPED, "user"),
        "last_modified_by_sfid" to Expectation(FkResolutionKind.MAPPED, "user"),
        "owner_sfid" to Expectation(FkResolutionKind.MAPPED, "user"), // polymorphic owner — 기본 user, CASE 분기는 service
        "assignee_user_sfid" to Expectation(FkResolutionKind.MAPPED, "user"),

        // ── alias / self-reference (prefix ≠ table) ──
        "manager_sfid" to Expectation(FkResolutionKind.MAPPED, "employee"), // sourceTable 미지정 시 기본 employee
        "parent_sfid" to Expectation(FkResolutionKind.MAPPED, "account"),
        "full_name_sfid" to Expectation(FkResolutionKind.MAPPED, "employee"),
        "team_leader_sfid" to Expectation(FkResolutionKind.MAPPED, "employee"),
        "primary_product_sfid" to Expectation(FkResolutionKind.MAPPED, "product"),
        "alt_holiday_sfid" to Expectation(FkResolutionKind.MAPPED, "alternative_holiday"),
        "postponed_appointment_sfid" to Expectation(FkResolutionKind.MAPPED, "appointment"),
        "commute_log_sfid" to Expectation(FkResolutionKind.MAPPED, "attendance_log"),
        "theme_sfid" to Expectation(FkResolutionKind.MAPPED, "inspection_theme"),
        "category_sfid" to Expectation(FkResolutionKind.MAPPED, "account_category_master"),

        // ── prefix == table 자동추론 (정확) ──
        // group_sfid: group_member.group_sfid → group.group_id. FK_PREFIX_MAPPING 미등록이나
        // prefix == table 라 (group, group_id) 자동추론이 정확.
        "group_sfid" to Expectation(FkResolutionKind.AUTO_INFERRED, "group"),

        // ── SKIP (polymorphic 전용 / 자연 키 substep — SKIP_FK_PREFIXES) ──
        "related_sfid" to Expectation(FkResolutionKind.SKIP, null),
        "user_or_group_sfid" to Expectation(FkResolutionKind.SKIP, null),
        "target_sfid" to Expectation(FkResolutionKind.SKIP, null),

        // permission_set_sfid: SKIP_FK_PREFIXES 에 없어 sfid prefix 경로상 AUTO_INFERRED (ref permission_set).
        // 실제 해소는 NaturalKey FK Service 가 permission_set_assignment.permission_set_sfid →
        // permission_set_flags lookup 으로 별도 처리 (NATURAL_KEY_FK_MAPPINGS). 즉 sfid prefix 경로의
        // AUTO_INFERRED UPDATE 는 permission_set_id 컬럼 부재로 buildPlansByTable 에서 error 후 skip 된다.
        "permission_set_sfid" to Expectation(FkResolutionKind.AUTO_INFERRED, "permission_set"),
    )

    @Test
    @DisplayName("인벤토리 크기 = 38 (backend entity 의 @Column(name=..._sfid) 전수, 2026-05-31 기준)")
    fun inventorySizeTripwire() {
        // 신규 *_sfid 컬럼 추가 시 이 숫자가 어긋나 사람이 EXPECTED 를 갱신하도록 강제.
        // 갱신 명령은 클래스 KDoc 참조. 38 = 권위 목록 (sfid 단독 PK 컬럼은 제외).
        assertThat(expected).hasSize(38)
    }

    @Test
    @DisplayName("모든 알려진 *_sfid 컬럼의 분류 + ref 가 인벤토리 고정값과 일치 (신규 컬럼/매핑 변경 감지)")
    fun inventoryMatchesExpectations() {
        for ((sfidColumn, exp) in expected) {
            val kind = classifyFkResolution(sfidColumn)
            assertThat(kind)
                .describedAs("$sfidColumn 분류")
                .isEqualTo(exp.kind)

            val spec = deriveFkResolveSpec(sfidColumn)
            if (exp.refTable == null) {
                assertThat(spec).describedAs("$sfidColumn 은 SKIP → null spec").isNull()
            } else {
                assertThat(spec).describedAs("$sfidColumn spec").isNotNull
                assertThat(spec!!.refTable)
                    .describedAs("$sfidColumn refTable")
                    .isEqualTo(exp.refTable)
            }
        }
    }

    @Test
    @DisplayName("table-scoped override — user.manager_sfid 는 user self-ref, employee.manager_sfid 는 employee self-ref")
    fun tableScopedManagerClassification() {
        assertThat(classifyFkResolution("manager_sfid", "user")).isEqualTo(FkResolutionKind.TABLE_SCOPED)
        assertThat(classifyFkResolution("manager_sfid", "employee")).isEqualTo(FkResolutionKind.MAPPED)
        assertThat(deriveFkResolveSpec("manager_sfid", "user")!!.refTable).isEqualTo("user")
        assertThat(deriveFkResolveSpec("manager_sfid", "employee")!!.refTable).isEqualTo("employee")
    }
}
