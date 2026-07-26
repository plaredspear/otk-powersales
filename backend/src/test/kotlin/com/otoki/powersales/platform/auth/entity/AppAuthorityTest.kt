package com.otoki.powersales.platform.auth.entity

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("AppAuthority 테스트")
class AppAuthorityTest {

    @Test
    @DisplayName("isTeamManager - 조장은 팀 관리 권한")
    fun isTeamManager_leader() {
        assertThat(AppAuthority.isTeamManager(AppAuthority.LEADER)).isTrue()
    }

    @Test
    @DisplayName("isTeamManager - 지점장도 조장과 동일하게 팀 관리 권한 (레거시 `eq '조장'` 확장)")
    fun isTeamManager_branchManager() {
        assertThat(AppAuthority.isTeamManager(AppAuthority.BRANCH_MANAGER)).isTrue()
    }

    @Test
    @DisplayName("isTeamManager - 여사원 / 부서장 / null 은 팀 관리 권한 아님")
    fun isTeamManager_others() {
        assertThat(AppAuthority.isTeamManager(AppAuthority.WOMAN)).isFalse()
        assertThat(AppAuthority.isTeamManager(AppAuthority.ACCOUNT_VIEW_ALL)).isFalse()
        assertThat(AppAuthority.isTeamManager(null)).isFalse()
        assertThat(AppAuthority.isTeamManager("")).isFalse()
    }
}
