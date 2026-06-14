package com.otoki.powersales.admin.controller

import com.otoki.powersales.admin.service.AdminCacheService
import com.otoki.powersales.platform.auth.entity.AppAuthority
import com.otoki.powersales.platform.common.test.AdminControllerTestSupport
import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@WebMvcTest(AdminCacheController::class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("AdminCacheController 테스트")
class AdminCacheControllerTest : AdminControllerTestSupport() {

    @MockkBean private lateinit var adminCacheService: AdminCacheService

    @BeforeEach
    fun setUpAdminPrincipal() {
        authenticateAsAdmin(role = AppAuthority.LEADER, employeeCode = "ADMIN001")
    }

    @Test
    @DisplayName("GET /api/v1/admin/cache - cache name 일람 반환")
    fun list_success() {
        every { adminCacheService.listCaches() } returns listOf(
            AdminCacheService.CacheInfo(name = "teamScheduleBranchesV2", estimatedKeyCount = 5),
            AdminCacheService.CacheInfo(name = "organizationCascadeV2", estimatedKeyCount = 12),
        )

        mockMvc.perform(get("/api/v1/admin/cache"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.length()").value(2))
            .andExpect(jsonPath("$.data[0].name").value("teamScheduleBranchesV2"))
            .andExpect(jsonPath("$.data[0].estimatedKeyCount").value(5))
    }

    @Test
    @DisplayName("POST /api/v1/admin/cache/{cacheName}/evict - 단일 cache evict 성공")
    fun evict_success() {
        every { adminCacheService.evict("teamScheduleBranchesV2", "ADMIN001") } returns
            AdminCacheService.EvictResult(cacheName = "teamScheduleBranchesV2", keysBefore = 5, keysAfter = 0)

        mockMvc.perform(post("/api/v1/admin/cache/teamScheduleBranchesV2/evict"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.message").value("캐시가 무효화되었습니다"))
            .andExpect(jsonPath("$.data.cacheName").value("teamScheduleBranchesV2"))
            .andExpect(jsonPath("$.data.keysBefore").value(5))
            .andExpect(jsonPath("$.data.keysAfter").value(0))

        verify { adminCacheService.evict("teamScheduleBranchesV2", "ADMIN001") }
    }

    @Test
    @DisplayName("POST /api/v1/admin/cache/evict-all - 전체 cache 일괄 evict 성공")
    fun evictAll_success() {
        every { adminCacheService.evictAll("ADMIN001") } returns listOf(
            AdminCacheService.EvictResult(cacheName = "teamScheduleBranchesV2", keysBefore = 5, keysAfter = 0),
            AdminCacheService.EvictResult(cacheName = "mem:adminPermissionCache", keysBefore = 3, keysAfter = 0),
        )

        mockMvc.perform(post("/api/v1/admin/cache/evict-all"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.message").value("전체 캐시가 무효화되었습니다 (2건)"))
            .andExpect(jsonPath("$.data.length()").value(2))
            .andExpect(jsonPath("$.data[1].cacheName").value("mem:adminPermissionCache"))

        verify { adminCacheService.evictAll("ADMIN001") }
    }
}
