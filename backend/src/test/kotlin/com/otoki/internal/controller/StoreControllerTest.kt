package com.otoki.internal.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.otoki.internal.dto.response.MyStoreInfo
import com.otoki.internal.dto.response.MyStoreListResponse
import com.otoki.internal.entity.UserRole
import com.otoki.internal.exception.UserNotFoundException
import com.otoki.internal.security.JwtAuthenticationFilter
import com.otoki.internal.security.JwtTokenProvider
import com.otoki.internal.security.UserPrincipal
import com.otoki.internal.service.MyStoreService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.eq
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.http.MediaType
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@WebMvcTest(StoreController::class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("StoreController 테스트")
class StoreControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @MockitoBean
    private lateinit var myStoreService: MyStoreService

    @MockitoBean
    private lateinit var jwtTokenProvider: JwtTokenProvider

    @MockitoBean
    private lateinit var jwtAuthenticationFilter: JwtAuthenticationFilter

    private val testPrincipal = UserPrincipal(userId = 1L, role = UserRole.USER)

    @BeforeEach
    fun setUp() {
        val authentication = UsernamePasswordAuthenticationToken(
            testPrincipal, null, testPrincipal.authorities
        )
        SecurityContextHolder.getContext().authentication = authentication
    }

    @Nested
    @DisplayName("GET /api/v1/stores/my - 내 거래처 목록 조회")
    inner class GetMyStores {

        @Test
        @DisplayName("정상 조회 - 거래처 목록 반환")
        fun getMyStores_Success() {
            // given
            val stores = listOf(
                MyStoreInfo(
                    storeId = 1L,
                    storeName = "경산농협",
                    storeCode = "STORE001",
                    address = "경북 경산시 중앙로 123",
                    representativeName = "김영수",
                    phoneNumber = "053-123-4567"
                ),
                MyStoreInfo(
                    storeId = 2L,
                    storeName = "대구중앙마트",
                    storeCode = "STORE002",
                    address = "대구광역시 중구 동성로 456",
                    representativeName = "이철수",
                    phoneNumber = "053-234-5678"
                ),
                MyStoreInfo(
                    storeId = 3L,
                    storeName = "부산농협",
                    storeCode = "STORE003",
                    address = "부산광역시 해운대구 센텀로 789",
                    representativeName = "박민수",
                    phoneNumber = "051-345-6789"
                )
            )
            val response = MyStoreListResponse(stores = stores, totalCount = 3)

            whenever(myStoreService.getMyStores(eq(1L), eq(null)))
                .thenReturn(response)

            // when & then
            mockMvc.perform(
                get("/api/v1/stores/my")
                    .contentType(MediaType.APPLICATION_JSON)
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("내 거래처 목록 조회 성공"))
                .andExpect(jsonPath("$.data.total_count").value(3))
                .andExpect(jsonPath("$.data.stores").isArray)
                .andExpect(jsonPath("$.data.stores.length()").value(3))
                .andExpect(jsonPath("$.data.stores[0].store_id").value(1))
                .andExpect(jsonPath("$.data.stores[0].store_name").value("경산농협"))
                .andExpect(jsonPath("$.data.stores[0].store_code").value("STORE001"))
                .andExpect(jsonPath("$.data.stores[0].address").value("경북 경산시 중앙로 123"))
                .andExpect(jsonPath("$.data.stores[0].representative_name").value("김영수"))
                .andExpect(jsonPath("$.data.stores[0].phone_number").value("053-123-4567"))
                .andExpect(jsonPath("$.data.stores[1].store_id").value(2))
                .andExpect(jsonPath("$.data.stores[1].store_name").value("대구중앙마트"))
                .andExpect(jsonPath("$.data.stores[2].store_id").value(3))
                .andExpect(jsonPath("$.data.stores[2].store_name").value("부산농협"))
        }

        @Test
        @DisplayName("검색어 포함 조회 - 필터링된 결과 반환")
        fun getMyStores_WithKeyword_Success() {
            // given
            val keyword = "경산"
            val stores = listOf(
                MyStoreInfo(
                    storeId = 1L,
                    storeName = "경산농협",
                    storeCode = "STORE001",
                    address = "경북 경산시 중앙로 123",
                    representativeName = "김영수",
                    phoneNumber = "053-123-4567"
                )
            )
            val response = MyStoreListResponse(stores = stores, totalCount = 1)

            whenever(myStoreService.getMyStores(eq(1L), eq(keyword)))
                .thenReturn(response)

            // when & then
            mockMvc.perform(
                get("/api/v1/stores/my")
                    .param("keyword", keyword)
                    .contentType(MediaType.APPLICATION_JSON)
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("내 거래처 목록 조회 성공"))
                .andExpect(jsonPath("$.data.total_count").value(1))
                .andExpect(jsonPath("$.data.stores").isArray)
                .andExpect(jsonPath("$.data.stores.length()").value(1))
                .andExpect(jsonPath("$.data.stores[0].store_id").value(1))
                .andExpect(jsonPath("$.data.stores[0].store_name").value("경산농협"))
                .andExpect(jsonPath("$.data.stores[0].store_code").value("STORE001"))
        }

        @Test
        @DisplayName("결과 없음 - 빈 목록 반환")
        fun getMyStores_EmptyResult() {
            // given
            val response = MyStoreListResponse(stores = emptyList(), totalCount = 0)

            whenever(myStoreService.getMyStores(eq(1L), eq(null)))
                .thenReturn(response)

            // when & then
            mockMvc.perform(
                get("/api/v1/stores/my")
                    .contentType(MediaType.APPLICATION_JSON)
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("내 거래처 목록 조회 성공"))
                .andExpect(jsonPath("$.data.total_count").value(0))
                .andExpect(jsonPath("$.data.stores").isArray)
                .andExpect(jsonPath("$.data.stores.length()").value(0))
        }

        @Test
        @DisplayName("검색어로 필터링 후 결과 없음 - 빈 목록 반환")
        fun getMyStores_WithKeyword_EmptyResult() {
            // given
            val keyword = "존재하지않는거래처"
            val response = MyStoreListResponse(stores = emptyList(), totalCount = 0)

            whenever(myStoreService.getMyStores(eq(1L), eq(keyword)))
                .thenReturn(response)

            // when & then
            mockMvc.perform(
                get("/api/v1/stores/my")
                    .param("keyword", keyword)
                    .contentType(MediaType.APPLICATION_JSON)
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("내 거래처 목록 조회 성공"))
                .andExpect(jsonPath("$.data.total_count").value(0))
                .andExpect(jsonPath("$.data.stores").isArray)
                .andExpect(jsonPath("$.data.stores.length()").value(0))
        }

        @Test
        @DisplayName("사용자 없음 - USER_NOT_FOUND 예외 발생")
        fun getMyStores_UserNotFound() {
            // given
            whenever(myStoreService.getMyStores(eq(1L), eq(null)))
                .thenThrow(UserNotFoundException())

            // when & then
            mockMvc.perform(
                get("/api/v1/stores/my")
                    .contentType(MediaType.APPLICATION_JSON)
            )
                .andExpect(status().isNotFound)
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("USER_NOT_FOUND"))
                .andExpect(jsonPath("$.error.message").value("사용자를 찾을 수 없습니다"))
        }

        @Test
        @DisplayName("거래처 정보 일부 필드 null - 정상 처리")
        fun getMyStores_WithNullFields_Success() {
            // given
            val stores = listOf(
                MyStoreInfo(
                    storeId = 1L,
                    storeName = "경산농협",
                    storeCode = "STORE001",
                    address = null,
                    representativeName = null,
                    phoneNumber = null
                )
            )
            val response = MyStoreListResponse(stores = stores, totalCount = 1)

            whenever(myStoreService.getMyStores(eq(1L), eq(null)))
                .thenReturn(response)

            // when & then
            mockMvc.perform(
                get("/api/v1/stores/my")
                    .contentType(MediaType.APPLICATION_JSON)
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("내 거래처 목록 조회 성공"))
                .andExpect(jsonPath("$.data.total_count").value(1))
                .andExpect(jsonPath("$.data.stores[0].store_id").value(1))
                .andExpect(jsonPath("$.data.stores[0].store_name").value("경산농협"))
                .andExpect(jsonPath("$.data.stores[0].store_code").value("STORE001"))
                .andExpect(jsonPath("$.data.stores[0].address").isEmpty)
                .andExpect(jsonPath("$.data.stores[0].representative_name").isEmpty)
                .andExpect(jsonPath("$.data.stores[0].phone_number").isEmpty)
        }

        @Test
        @DisplayName("다수의 거래처 조회 - 페이지네이션 없이 전체 목록 반환")
        fun getMyStores_MultipleStores_Success() {
            // given
            val stores = (1..10).map { i ->
                MyStoreInfo(
                    storeId = i.toLong(),
                    storeName = "거래처$i",
                    storeCode = "STORE%03d".format(i),
                    address = "주소$i",
                    representativeName = "대표$i",
                    phoneNumber = "010-0000-00%02d".format(i)
                )
            }
            val response = MyStoreListResponse(stores = stores, totalCount = 10)

            whenever(myStoreService.getMyStores(eq(1L), eq(null)))
                .thenReturn(response)

            // when & then
            mockMvc.perform(
                get("/api/v1/stores/my")
                    .contentType(MediaType.APPLICATION_JSON)
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("내 거래처 목록 조회 성공"))
                .andExpect(jsonPath("$.data.total_count").value(10))
                .andExpect(jsonPath("$.data.stores").isArray)
                .andExpect(jsonPath("$.data.stores.length()").value(10))
                .andExpect(jsonPath("$.data.stores[0].store_id").value(1))
                .andExpect(jsonPath("$.data.stores[9].store_id").value(10))
        }
    }
}
