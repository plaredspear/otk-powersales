package com.otoki.powersales.auth.sharing.service

import com.otoki.powersales.auth.sharing.entity.PermissionSetRecordType
import com.otoki.powersales.auth.sharing.entity.ProfileRecordType
import com.otoki.powersales.auth.sharing.repository.PermissionSetRecordTypeRepository
import com.otoki.powersales.auth.sharing.repository.ProfileRecordTypeRepository
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

/**
 * RecordTypePermissionEvaluator 단위 테스트 (spec #794).
 *
 * Q1~Q4 옵션 1 정합 — Profile + PermissionSet 합산 가시 RT 집합 산출.
 */
@DisplayName("RecordTypePermissionEvaluator — spec #794")
class RecordTypePermissionEvaluatorTest {

    private val profileRepo = mockk<ProfileRecordTypeRepository>()
    private val psRepo = mockk<PermissionSetRecordTypeRepository>()
    private val evaluator = RecordTypePermissionEvaluator(profileRepo, psRepo)

    @Test
    @DisplayName("Profile 0건 + PermissionSet 2건 visible — PermissionSet 위임 패턴 (운영 패턴)")
    fun permissionSetDominant() {
        every { profileRepo.findAllByProfileId(10L) } returns emptyList()
        every { psRepo.findAllByPermissionSetId(100L) } returns listOf(
            mkPsRt(101L, true), mkPsRt(102L, false),
        )
        every { psRepo.findAllByPermissionSetId(200L) } returns listOf(mkPsRt(103L, true))

        val result = evaluator.visibleRecordTypeIds(1L, 10L, setOf(100L, 200L))
        assertThat(result).containsExactlyInAnyOrder(101L, 103L)
    }

    @Test
    @DisplayName("Profile + PermissionSet 모두 visible 0건 — 빈 set (Q2 옵션 1: sObject 차단)")
    fun noVisible() {
        every { profileRepo.findAllByProfileId(10L) } returns emptyList()
        every { psRepo.findAllByPermissionSetId(100L) } returns listOf(mkPsRt(101L, false))

        val result = evaluator.visibleRecordTypeIds(1L, 10L, setOf(100L))
        assertThat(result).isEmpty()
    }

    @Test
    @DisplayName("profileId null — Profile 측 lookup skip")
    fun profileIdNull() {
        every { psRepo.findAllByPermissionSetId(100L) } returns listOf(mkPsRt(101L, true))

        val result = evaluator.visibleRecordTypeIds(1L, null, setOf(100L))
        assertThat(result).containsExactly(101L)
    }

    @Test
    @DisplayName("PermissionSet 0건 + Profile 1건 visible — Profile 단독")
    fun profileOnly() {
        every { profileRepo.findAllByProfileId(10L) } returns listOf(mkProfileRt(101L, true))

        val result = evaluator.visibleRecordTypeIds(1L, 10L, emptySet())
        assertThat(result).containsExactly(101L)
    }

    @Test
    @DisplayName("record_type_id NULL — 결과에 포함 안 함")
    fun recordTypeIdNull() {
        every { profileRepo.findAllByProfileId(10L) } returns listOf(
            ProfileRecordType(
                profileName = "X",
                sObjectName = "Account",
                recordTypeDeveloperName = "Master",
                visible = true,
                isDefault = false,
                recordTypeId = null, // 명시적 null
            ),
        )

        val result = evaluator.visibleRecordTypeIds(1L, 10L, emptySet())
        assertThat(result).isEmpty()
    }

    private fun mkPsRt(rtId: Long, visible: Boolean) = PermissionSetRecordType(
        permissionSetName = "PS",
        sObjectName = "Account",
        recordTypeDeveloperName = "RT$rtId",
        visible = visible,
        isDefault = false,
        recordTypeId = rtId,
    )

    private fun mkProfileRt(rtId: Long, visible: Boolean) = ProfileRecordType(
        profileName = "P",
        sObjectName = "Account",
        recordTypeDeveloperName = "RT$rtId",
        visible = visible,
        isDefault = false,
        recordTypeId = rtId,
    )
}
