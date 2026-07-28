package com.otoki.powersales._migration.sf.service

import com.otoki.powersales.platform.auth.permission.LeaderProfileFlagsSeed
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

/**
 * 조장(`6.조장`) 권한 SoT ↔ 회수 키 집합 정합 검증.
 *
 * `leader-erp-org-revoke` substep 은 이미 DB 에 적재된 권한을 **회수**하는 축이고,
 * [LeaderProfileFlagsSeed] 는 "조장이 가져야 할 권한" 의 SoT 다. 두 축이 어긋나면
 * (SoT 에 키가 남아 있는데 회수 대상이거나, 그 반대) 신규 환경과 기존 환경의 조장 권한이
 * 달라진다 — 그 드리프트를 부팅 전에 잡는다.
 *
 * substep 의 실제 실행은 jsonb `-` 가 PostgreSQL 전용이라 H2 통합 테스트로 검증할 수 없어
 * dev 환경 수동 검증에 맡긴다 (SfMigrationStage2ServiceIntegrationTest 주석 참조).
 */
@DisplayName("조장 권한 SoT ↔ 회수 키 정합")
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
    @DisplayName("회수 대상은 ERP주문(erp_order) + 조직마스터(organization) 자원 — 사용자 결정 범위 고정")
    fun `revoked keys cover erp order and organization`() {
        assertThat(SfMigrationStage2Service.REVOKED_LEADER_OBJECT_KEYS)
            .containsExactlyInAnyOrder("ERP_Order__c", "ERP_OrderProduct__c", "Org__c")
    }

    @Test
    @DisplayName("공휴일/영업일 마스터는 애초에 6.조장 SoT 에 없어 회수 대상이 아니다")
    fun `holiday and working day masters are not granted to leader`() {
        // 회수 substep 이 다루지 않는 자원이므로, SoT 에 실수로 추가되면 조장이 권한을 얻게 된다.
        assertThat(leaderSeed.objectPermissionsJson).doesNotContain("HolidayMaster__c")
        assertThat(leaderSeed.objectPermissionsJson).doesNotContain("WorkingDayMaster__c")
        assertThat(leaderSeed.objectPermissionsJson).doesNotContain("DKRetail__AlternativeHoliday__c")
    }
}
