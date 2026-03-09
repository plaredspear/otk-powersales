package com.otoki.internal.admin.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.otoki.internal.admin.dto.request.PromotionTypeRequest
import com.otoki.internal.admin.dto.response.PromotionTypeResponse
import com.otoki.internal.admin.security.AdminAuthorityFilter
import com.otoki.internal.admin.service.AdminPromotionTypeService
import com.otoki.internal.common.security.GpsConsentFilter
import com.otoki.internal.common.security.JwtAuthenticationFilter
import com.otoki.internal.common.security.JwtTokenProvider
import com.otoki.internal.common.security.UserPrincipal
import com.otoki.internal.promotion.exception.PromotionTypeDuplicateException
import com.otoki.internal.promotion.exception.PromotionTypeNotFoundException
import com.otoki.internal.sap.entity.UserRole
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@WebMvcTest(AdminPromotionTypeController::class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("AdminPromotionTypeController 테스트")
class AdminPromotionTypeControllerTest {

    @Autowired private lateinit var mockMvc: MockMvc
    @Autowired private lateinit var objectMapper: ObjectMapper

    @MockitoBean private lateinit var adminPromotionTypeService: AdminPromotionTypeService
    @MockitoBean private lateinit var jwtTokenProvider: JwtTokenProvider
    @MockitoBean private lateinit var jwtAuthenticationFilter: JwtAuthenticationFilter
    @MockitoBean private lateinit var adminAuthorityFilter: AdminAuthorityFilter
    @MockitoBean private lateinit var gpsConsentFilter: GpsConsentFilter

    @BeforeEach
    fun setUp() {
        val principal = UserPrincipal(userId = 1L, role = UserRole.ADMIN)
        SecurityContextHolder.getContext().authentication =
            UsernamePasswordAuthenticationToken(principal, null, principal.authorities)
    }

    @Nested
    @DisplayName("GET /api/v1/admin/promotion-types - 목록 조회")
    inner class GetPromotionTypes {

        @Test
        @DisplayName("성공 - 활성 행사유형 목록 반환")
        fun getPromotionTypes_success() {
            val response = listOf(
                PromotionTypeResponse(id = 1L, name = "시식", displayOrder = 1, isActive = true),
                PromotionTypeResponse(id = 2L, name = "시음", displayOrder = 2, isActive = true)
            )
            whenever(adminPromotionTypeService.getPromotionTypes()).thenReturn(response)

            mockMvc.perform(get("/api/v1/admin/promotion-types"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].name").value("시식"))
                .andExpect(jsonPath("$.data[0].display_order").value(1))
                .andExpect(jsonPath("$.data[1].name").value("시음"))
        }
    }

    @Nested
    @DisplayName("POST /api/v1/admin/promotion-types - 생성")
    inner class CreatePromotionType {

        @Test
        @DisplayName("성공 - 행사유형 생성")
        fun createPromotionType_success() {
            val response = PromotionTypeResponse(id = 6L, name = "시연", displayOrder = 6, isActive = true)
            whenever(adminPromotionTypeService.createPromotionType(any())).thenReturn(response)

            mockMvc.perform(
                post("/api/v1/admin/promotion-types")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(PromotionTypeRequest(name = "시연", displayOrder = 6)))
            )
                .andExpect(status().isCreated)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(6))
                .andExpect(jsonPath("$.data.name").value("시연"))
        }

        @Test
        @DisplayName("실패 - 이름 중복")
        fun createPromotionType_duplicate() {
            whenever(adminPromotionTypeService.createPromotionType(any()))
                .thenThrow(PromotionTypeDuplicateException())

            mockMvc.perform(
                post("/api/v1/admin/promotion-types")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(PromotionTypeRequest(name = "시식", displayOrder = 1)))
            )
                .andExpect(status().isConflict)
                .andExpect(jsonPath("$.error.code").value("PROMOTION_TYPE_DUPLICATE"))
        }

        @Test
        @DisplayName("실패 - 이름 빈 문자열")
        fun createPromotionType_emptyName() {
            val invalidJson = """{"name": "", "display_order": 1}"""

            mockMvc.perform(
                post("/api/v1/admin/promotion-types")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(invalidJson)
            )
                .andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.success").value(false))
        }
    }

    @Nested
    @DisplayName("PUT /api/v1/admin/promotion-types/{id} - 수정")
    inner class UpdatePromotionType {

        @Test
        @DisplayName("성공 - 행사유형 수정")
        fun updatePromotionType_success() {
            val response = PromotionTypeResponse(id = 1L, name = "시식행사", displayOrder = 1, isActive = true)
            whenever(adminPromotionTypeService.updatePromotionType(eq(1L), any())).thenReturn(response)

            mockMvc.perform(
                put("/api/v1/admin/promotion-types/1")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(PromotionTypeRequest(name = "시식행사", displayOrder = 1)))
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.data.name").value("시식행사"))
        }

        @Test
        @DisplayName("실패 - 미존재 ID")
        fun updatePromotionType_notFound() {
            whenever(adminPromotionTypeService.updatePromotionType(eq(999L), any()))
                .thenThrow(PromotionTypeNotFoundException())

            mockMvc.perform(
                put("/api/v1/admin/promotion-types/999")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(PromotionTypeRequest(name = "시연", displayOrder = 6)))
            )
                .andExpect(status().isNotFound)
                .andExpect(jsonPath("$.error.code").value("NOT_FOUND"))
        }
    }

    @Nested
    @DisplayName("DELETE /api/v1/admin/promotion-types/{id} - 비활성화")
    inner class DeletePromotionType {

        @Test
        @DisplayName("성공 - 비활성화")
        fun deletePromotionType_success() {
            doNothing().whenever(adminPromotionTypeService).deletePromotionType(eq(5L))

            mockMvc.perform(delete("/api/v1/admin/promotion-types/5"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
        }

        @Test
        @DisplayName("실패 - 미존재 ID")
        fun deletePromotionType_notFound() {
            whenever(adminPromotionTypeService.deletePromotionType(eq(999L)))
                .thenThrow(PromotionTypeNotFoundException())

            mockMvc.perform(delete("/api/v1/admin/promotion-types/999"))
                .andExpect(status().isNotFound)
                .andExpect(jsonPath("$.error.code").value("NOT_FOUND"))
        }
    }
}
