package com.otoki.powersales.sap.inbound.controller

import com.otoki.powersales.admin.security.AdminAuthorityFilter
import com.otoki.powersales.common.security.GpsConsentFilter
import com.otoki.powersales.common.security.JwtAuthenticationFilter
import com.otoki.powersales.common.security.JwtTokenProvider
import com.otoki.powersales.sap.inbound.dto.product.FailureItem
import com.otoki.powersales.sap.inbound.dto.product.ProductMasterDetail
import com.otoki.powersales.sap.inbound.service.SapBarcodeMasterService
import com.otoki.powersales.sap.inbound.service.SapProductMasterService
import com.otoki.powersales.sap.inbound.service.SapSystemCodeMasterService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@WebMvcTest(SapProductMasterController::class)
@AutoConfigureMockMvc(addFilters = false)
@Import(SapInboundExceptionHandler::class)
@DisplayName("SapProductMasterController 테스트")
class SapProductMasterControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockitoBean
    private lateinit var sapProductMasterService: SapProductMasterService

    @MockitoBean
    private lateinit var sapBarcodeMasterService: SapBarcodeMasterService

    @MockitoBean
    private lateinit var sapSystemCodeMasterService: SapSystemCodeMasterService

    @MockitoBean
    private lateinit var jwtTokenProvider: JwtTokenProvider

    @MockitoBean
    private lateinit var jwtAuthenticationFilter: JwtAuthenticationFilter

    @MockitoBean
    private lateinit var adminAuthorityFilter: AdminAuthorityFilter

    @MockitoBean
    private lateinit var gpsConsentFilter: GpsConsentFilter

    @BeforeEach
    fun setUp() {
        SecurityContextHolder.getContext().authentication =
            UsernamePasswordAuthenticationToken(
                "otoki-sap-client",
                null,
                listOf(SimpleGrantedAuthority("SCOPE_sap.product.write"))
            )
    }

    @Nested
    @DisplayName("POST /api/v1/sap/product")
    inner class UpsertProduct {

        @Test
        @DisplayName("성공 - 200, RESULT_CODE 200, success_count=1")
        fun upsert_success() {
            whenever(sapProductMasterService.upsert(any())).thenReturn(
                ProductMasterDetail(successCount = 1, failureCount = 0, failures = emptyList())
            )

            val payload = """
                {
                  "reqItemList": [
                    { "ProductCode": "100100", "ProductName": "진라면 매운맛 5입", "StandardPrice": "4500" }
                  ]
                }
            """.trimIndent()

            mockMvc.perform(
                post("/api/v1/sap/product")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(payload)
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.RESULT_CODE").value("200"))
                .andExpect(jsonPath("$.RESULT_DETAIL.success_count").value(1))
        }

        @Test
        @DisplayName("부분 실패 - 200, failures 페이로드 포함")
        fun upsert_partialFailure() {
            whenever(sapProductMasterService.upsert(any())).thenReturn(
                ProductMasterDetail(
                    successCount = 1,
                    failureCount = 1,
                    failures = listOf(FailureItem("100200", "StandardPrice 변환 실패: abc"))
                )
            )

            val payload = """
                {
                  "reqItemList": [
                    { "ProductCode": "100100", "ProductName": "정상" },
                    { "ProductCode": "100200", "ProductName": "에러", "StandardPrice": "abc" }
                  ]
                }
            """.trimIndent()

            mockMvc.perform(
                post("/api/v1/sap/product")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(payload)
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.RESULT_DETAIL.failure_count").value(1))
                .andExpect(jsonPath("$.RESULT_DETAIL.failures[0].identifier").value("100200"))
        }

        @Test
        @DisplayName("실패 - reqItemList 누락 -> INVALID_PAYLOAD")
        fun upsert_missingReqItemList() {
            mockMvc.perform(
                post("/api/v1/sap/product")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"other": []}""")
            )
                .andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.RESULT_CODE").value("INVALID_PAYLOAD"))

            verify(sapProductMasterService, never()).upsert(any())
        }
    }

    @Nested
    @DisplayName("POST /api/v1/sap/product-barcode")
    inner class UpsertBarcode {

        @Test
        @DisplayName("성공 - 200, RESULT_CODE 200")
        fun upsert_success() {
            whenever(sapBarcodeMasterService.upsert(any())).thenReturn(
                ProductMasterDetail(successCount = 1, failureCount = 0, failures = emptyList())
            )

            val payload = """
                {
                  "reqItemList": [
                    { "ProductCode": "100100", "ProductUnit": "EA", "ProductSequence": "001", "ProductBarcode": "8801045123456" }
                  ]
                }
            """.trimIndent()

            mockMvc.perform(
                post("/api/v1/sap/product-barcode")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(payload)
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.RESULT_CODE").value("200"))
                .andExpect(jsonPath("$.RESULT_DETAIL.success_count").value(1))
        }

        @Test
        @DisplayName("실패 - reqItemList 빈 배열 -> INVALID_PAYLOAD")
        fun upsert_emptyReqItemList() {
            mockMvc.perform(
                post("/api/v1/sap/product-barcode")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"reqItemList": []}""")
            )
                .andExpect(status().`is`(422))
                .andExpect(jsonPath("$.RESULT_CODE").value("INVALID_PAYLOAD"))

            verify(sapBarcodeMasterService, never()).upsert(any())
        }
    }

    @Nested
    @DisplayName("POST /api/v1/sap/system-code")
    inner class UpsertSystemCode {

        @Test
        @DisplayName("성공 - 200, RESULT_CODE 200")
        fun upsert_success() {
            whenever(sapSystemCodeMasterService.upsert(any())).thenReturn(
                ProductMasterDetail(successCount = 1, failureCount = 0, failures = emptyList())
            )

            val payload = """
                {
                  "reqItemList": [
                    { "CompanyCode": "1000", "GroupCode": "H10010", "DetailCode": "10", "DetailCodeName": "재직" }
                  ]
                }
            """.trimIndent()

            mockMvc.perform(
                post("/api/v1/sap/system-code")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(payload)
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.RESULT_CODE").value("200"))
                .andExpect(jsonPath("$.RESULT_DETAIL.success_count").value(1))
        }

        @Test
        @DisplayName("실패 - reqItemList null -> INVALID_PAYLOAD")
        fun upsert_missingReqItemList() {
            mockMvc.perform(
                post("/api/v1/sap/system-code")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{}""")
            )
                .andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.RESULT_CODE").value("INVALID_PAYLOAD"))

            verify(sapSystemCodeMasterService, never()).upsert(any())
        }
    }
}
