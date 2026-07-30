package com.otoki.powersales._migration.sf.service

import com.otoki.powersales.platform.auth.permission.LeaderProfileFlagsSeed
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

/**
 * 조장(`6.조장`) 권한 SoT ↔ 회수/부여 키 집합 정합 검증.
 *
 * [LeaderProfileFlagsSeed] 는 "조장이 가져야 할 권한" 의 SoT 이고, Stage 2 의 두 substep 은 이미 DB 에
 * 적재된 권한을 키 단위로 조정하는 축이다 — `leader-erp-org-revoke` 가 **회수**,
 * `leader-sales-dashboard-grant` 가 **부여**. SoT 와 두 축이 어긋나면 (SoT 에 키가 남아 있는데 회수
 * 대상이거나, 부여 대상이 SoT 에 없거나) 신규 환경과 기존 환경의 조장 권한이 달라진다 — 그 드리프트를
 * 부팅 전에 잡는다.
 *
 * substep 의 실제 실행은 jsonb `-` / `||` 가 PostgreSQL 전용이라 H2 통합 테스트로 검증할 수 없어
 * dev 환경 수동 검증에 맡긴다 (SfMigrationStage2ServiceIntegrationTest 주석 참조).
 */
@DisplayName("조장 권한 SoT ↔ 회수/부여 키 정합")
class LeaderProfileFlagsSeedRevokedKeysTest {

    private val leaderSeed = LeaderProfileFlagsSeed.SEEDS.first { it.profileName == "6.조장" }

    @Test
    @DisplayName("회수 대상 키는 6.조장 SoT 의 object_permissions 에 남아 있지 않다")
    fun `revoked keys are absent from seed`() {
        for (key in SfMigrationStage2Service.REVOKED_LEADER_OBJECT_KEYS) {
            assertThat(leaderSeed.objectPermissionsJson)
                .withFailMessage(
                    "회수 대상 '%s' 가 6.조장 SoT 에 남아 있습니다 — SoT 에서 제거하거나 회수 대상에서 빼야 합니다",
                    key,
                )
                .doesNotContain("\"$key\"")
        }
    }

    @Test
    @DisplayName("회수 대상 범위 고정 — ERP주문 / 조직마스터 / 근무 등록현황 / 대체휴무 / HR 적재 근무기간 / ORORA 일·월매출 (사용자 결정)")
    fun `revoked keys cover leader hidden screens`() {
        assertThat(SfMigrationStage2Service.REVOKED_LEADER_OBJECT_KEYS)
            .containsExactlyInAnyOrder(
                "ERP_Order__c",
                "ERP_OrderProduct__c",
                "Org__c",
                "DKRetail__CommuteLog__c",
                "DKRetail__AlternativeHoliday__c",
                "AttendInfo__c",
                "DailySalesHistory__c",
                "MonthlySalesHistory__c",
            )
    }

    @Test
    @DisplayName("ORORA 일매출/월매출 두 키를 모두 회수한다 (사용자 결정)")
    fun `both sales history keys revoked`() {
        // daily_sales_history   = 기준정보 > ORORA 일매출 (목록 + 전용 거래처 lookup) — 파급이 화면 하나.
        // monthly_sales_history = 기준정보 > ORORA 월매출 + 월별 투입적합성 + 배치 적합성 — 3화면 공유 키라
        //                         회수가 함께 파급된다 (각 화면 전용 지점/거래처 셀렉터 포함).
        for (key in listOf("DailySalesHistory__c", "MonthlySalesHistory__c")) {
            assertThat(leaderSeed.objectPermissionsJson).doesNotContain("\"$key\"")
            assertThat(SfMigrationStage2Service.REVOKED_LEADER_OBJECT_KEYS).contains(key)
        }
    }

    @Test
    @DisplayName("조장은 매출/실적 대시보드 3화면(sales_dashboard) READ 를 보유한다 (사용자 결정)")
    fun `leader has sales dashboard read`() {
        // 물류배부/전산실적/POS매출 3화면을 monthly_sales_history 에서 화면 전용 가상 자원으로 분리했고,
        // 조장은 분리 이후에도 3화면을 계속 조회한다 — SoT 에서 빠지면 신규 환경의 조장이 화면을 잃는다.
        assertThat(leaderSeed.customPermissionsJson)
            .withFailMessage("매출/실적 대시보드 권한(sales_dashboard READ)이 6.조장 SoT 에 없습니다")
            .contains("\"sales_dashboard\"")

        // 3화면 모두 조회 전용이라 READ 단독 — 나머지 비트는 대응 가드가 없는 죽은 키다.
        assertThat(leaderSeed.customPermissionsJson).contains("\"sales_dashboard\": { \"allowRead\": true }")

        // 자원 분리의 핵심 — MonthlySalesHistory__c 는 회수하면서 대시보드 3화면은 남긴다.
        // 분리 전이었다면 두 결과가 양립할 수 없었다 (한 키가 5화면을 함께 여닫았음).
        assertThat(SfMigrationStage2Service.REVOKED_LEADER_OBJECT_KEYS).contains("MonthlySalesHistory__c")
        assertThat(SfMigrationStage2Service.GRANTED_LEADER_CUSTOM_PERMISSIONS).contains("\"sales_dashboard\"")
    }

    @Test
    @DisplayName("부여 substep 의 JSON 과 6.조장 SoT 가 같은 자원을 가리킨다 (SoT ↔ 부여 축 정합)")
    fun `granted custom permissions match seed`() {
        // 신규 환경(clean row)은 leader-profile-flags 가 SoT 전체를, 기존 환경(dirty row)은
        // leader-sales-dashboard-grant 가 이 키만 병합한다. 두 축이 어긋나면 환경별 권한이 달라진다.
        assertThat(SfMigrationStage2Service.GRANTED_LEADER_CUSTOM_PERMISSIONS).contains("\"sales_dashboard\"")
        assertThat(SfMigrationStage2Service.GRANTED_LEADER_CUSTOM_PERMISSIONS).contains("\"allowRead\": true")
        assertThat(leaderSeed.customPermissionsJson).contains("\"sales_dashboard\"")
    }

    @Test
    @DisplayName("공휴일/영업일 마스터는 애초에 6.조장 SoT 에 없어 회수 대상이 아니다")
    fun `holiday and working day masters are not granted to leader`() {
        // 회수 substep 이 다루지 않는 자원이므로, SoT 에 실수로 추가되면 조장이 권한을 얻게 된다.
        assertThat(leaderSeed.objectPermissionsJson).doesNotContain("HolidayMaster__c")
        assertThat(leaderSeed.objectPermissionsJson).doesNotContain("WorkingDayMaster__c")
    }

    @Test
    @DisplayName("AttendInfo__c(HR 적재 근무기간) 와 DKRetail__CommuteLog__c(근무 등록현황) 는 별개 자원으로 각각 회수된다")
    fun `attend info and attendance log are separate resources both revoked`() {
        // 이름이 비슷해 하나로 뭉뚱그리기 쉬운 지점 — 가드 entity 가 서로 다르므로 둘 다 명시해야 한다.
        // attend_info      = 기준정보 > HR 적재 근무기간 (AdminAttendInfoController)
        // attendance_log   = 인사/근무 > 근무 등록현황 (AdminAttendanceLogController)
        assertThat(SfMigrationStage2Service.REVOKED_LEADER_OBJECT_KEYS)
            .contains("AttendInfo__c", "DKRetail__CommuteLog__c")
    }

    @Test
    @DisplayName("조장은 근무기간 조회(work_history) READ 보유 + HR 적재 근무기간(attend_info) 미보유 (사용자 결정)")
    fun `leader has work history read but not attend info`() {
        // 두 화면의 권한 분리 — 조장은 근무 실적 **조회**는 하되 SAP HR 적재 마스터는 편집/조회하지 않는다.
        // 분리 전에는 두 화면이 attend_info 하나를 공유해 AttendInfo__c 회수 시 조회 화면까지 닫혔다.
        assertThat(leaderSeed.customPermissionsJson)
            .withFailMessage("근무기간 조회 권한(work_history READ)이 6.조장 SoT 에 없습니다")
            .contains("\"work_history\"")

        // 조회 전용 화면이라 READ 단독 — EDIT/CREATE/DELETE 비트는 대응 가드가 없는 죽은 키다.
        assertThat(leaderSeed.customPermissionsJson).contains("\"work_history\": { \"allowRead\": true }")

        // attend_info 축은 계속 회수 상태여야 분리가 의미를 갖는다.
        assertThat(leaderSeed.objectPermissionsJson).doesNotContain("AttendInfo__c")
        assertThat(SfMigrationStage2Service.REVOKED_LEADER_OBJECT_KEYS).contains("AttendInfo__c")
    }
}
