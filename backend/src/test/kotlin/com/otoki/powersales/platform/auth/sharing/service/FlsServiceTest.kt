package com.otoki.powersales.platform.auth.sharing.service

import com.otoki.powersales.platform.auth.sharing.entity.PermissionSetFieldPermission
import com.otoki.powersales.platform.auth.sharing.entity.ProfileFieldPermission
import com.otoki.powersales.platform.auth.sharing.repository.PermissionSetFieldPermissionRepository
import com.otoki.powersales.platform.auth.sharing.repository.ProfileFieldPermissionRepository
import com.otoki.powersales.platform.auth.sharing.service.FlsService
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

/**
 * FlsService 단위 테스트 (spec #795).
 */
@DisplayName("FlsService — spec #795")
class FlsServiceTest {

    private val profileRepo = mockk<ProfileFieldPermissionRepository>()
    private val psRepo = mockk<PermissionSetFieldPermissionRepository>()
    private val service = FlsService(profileRepo, psRepo)

    @Test
    @DisplayName("Profile + PermissionSet 합산 — readable=true field 만 반환")
    fun readableFields() {
        every { profileRepo.findAllByProfileIdAndSObjectName(10L, "Account") } returns listOf(
            mkProfile("Name", readable = true),
            mkProfile("AnnualRevenue", readable = false),
        )
        every { psRepo.findAllByPermissionSetIdAndSObjectName(100L, "Account") } returns listOf(
            mkPs("Phone", readable = true),
            mkPs("AnnualRevenue", readable = false),
        )

        val result = service.readableFields(1L, "Account", 10L, setOf(100L))
        assertThat(result).containsExactlyInAnyOrder("Name", "Phone")
    }

    @Test
    @DisplayName("Profile 0건 + PermissionSet 위임 패턴 (운영 패턴)")
    fun permissionSetDominant() {
        every { profileRepo.findAllByProfileIdAndSObjectName(10L, "Account") } returns emptyList()
        every { psRepo.findAllByPermissionSetIdAndSObjectName(100L, "Account") } returns listOf(
            mkPs("AnnualRevenue", readable = true),
        )

        val result = service.readableFields(1L, "Account", 10L, setOf(100L))
        assertThat(result).containsExactly("AnnualRevenue")
    }

    @Test
    @DisplayName("editableFields — write FLS 후행 spec ⇒ 빈 set")
    fun editableFieldsEmpty() {
        val result = service.editableFields(1L, "Account", 10L, setOf(100L))
        assertThat(result).isEmpty()
    }

    private fun mkProfile(field: String, readable: Boolean) = ProfileFieldPermission(
        profileName = "P",
        sObjectName = "Account",
        fieldName = field,
        readable = readable,
        editable = false,
    )

    private fun mkPs(field: String, readable: Boolean) = PermissionSetFieldPermission(
        permissionSetName = "PS",
        sObjectName = "Account",
        fieldName = field,
        readable = readable,
        editable = false,
    )
}
