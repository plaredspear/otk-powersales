package com.otoki.powersales.admin.tools.daycoordinate

import com.otoki.powersales.admin.tools.daycoordinate.controller.AccountDayCoordinateController
import com.otoki.powersales.domain.activity.schedule.policy.AccountDayCoordinateOverride
import com.otoki.powersales.domain.activity.schedule.policy.AccountDayCoordinateOverrideStore
import com.otoki.powersales.platform.auth.permission.SystemAdminProfilePolicy
import com.otoki.powersales.platform.common.test.AdminControllerTestSupport
import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.DayOfWeek

/**
 * AccountDayCoordinateController 권한 가드 + 요청 검증 테스트.
 *
 * `requireSystemAdmin` 이 어노테이션이 아니라 명령형 호출이라 회귀 시 조용히 열릴 수 있으므로
 * 엔드포인트별 403/200 을 명시 고정한다. addFilters=false 로 컨트롤러 자체 가드만 단독 시험한다.
 */
@WebMvcTest(AccountDayCoordinateController::class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("AccountDayCoordinateController 테스트")
class AccountDayCoordinateControllerTest : AdminControllerTestSupport() {

    @MockkBean
    private lateinit var store: AccountDayCoordinateOverrideStore

    private val url = "/api/v1/admin/tools/account-day-coordinate"
    private val default = AccountDayCoordinateOverride.DEFAULT_COORDINATE

    @BeforeEach
    fun setUpStore() {
        every { store.getCoordinate() } returns default
        every { store.isCustomized() } returns false
    }

    private fun asSystemAdmin() =
        authenticateAsAdmin(role = null, profileName = SystemAdminProfilePolicy.SYSTEM_ADMIN_PROFILE_NAME)

    private fun asNonAdmin() = authenticateAsAdmin(role = null, profileName = "9. Staff")

    private fun body(
        dayOfWeek: String = "FRIDAY",
        latitude: String = "38.121391",
        longitude: String = "128.208204",
        label: String = "원통점",
    ) = """{"dayOfWeek":"$dayOfWeek","latitude":$latitude,"longitude":$longitude,"label":"$label"}"""

    @Test
    @DisplayName("GET - 시스템 관리자는 200 + 현재 설정 + 기본값 병기")
    fun get_systemAdmin_ok() {
        asSystemAdmin()

        mockMvc.perform(get(url))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.externalKey").value(AccountDayCoordinateOverride.TARGET_EXTERNAL_KEY))
            .andExpect(jsonPath("$.data.dayOfWeek").value(default.dayOfWeek.name))
            .andExpect(jsonPath("$.data.latitude").value(default.latitude))
            .andExpect(jsonPath("$.data.longitude").value(default.longitude))
            .andExpect(jsonPath("$.data.customized").value(false))
            .andExpect(jsonPath("$.data.defaultDayOfWeek").value(default.dayOfWeek.name))
            .andExpect(jsonPath("$.data.defaultLabel").value(default.label))
    }

    @Test
    @DisplayName("GET - 비 시스템 관리자는 403 (store 미호출)")
    fun get_nonAdmin_forbidden() {
        asNonAdmin()

        mockMvc.perform(get(url))
            .andExpect(status().isForbidden)
            .andExpect(jsonPath("$.error.code").value("PERMISSION_DENIED"))

        verify(exactly = 0) { store.getCoordinate() }
    }

    @Test
    @DisplayName("POST - 시스템 관리자는 200 + store 에 파싱된 좌표 저장")
    fun update_systemAdmin_ok() {
        asSystemAdmin()
        every { store.setCoordinate(any()) } returns Unit

        mockMvc.perform(post(url).contentType(MediaType.APPLICATION_JSON).content(body()))
            .andExpect(status().isOk)

        verify(exactly = 1) {
            store.setCoordinate(
                AccountDayCoordinateOverride.DayCoordinate(
                    DayOfWeek.FRIDAY, 38.121391, 128.208204, "원통점",
                ),
            )
        }
    }

    @Test
    @DisplayName("POST - 요일은 소문자/공백이어도 파싱 성공")
    fun update_lowercaseDayOfWeek_ok() {
        asSystemAdmin()
        every { store.setCoordinate(any()) } returns Unit

        mockMvc.perform(
            post(url).contentType(MediaType.APPLICATION_JSON).content(body(dayOfWeek = " friday ")),
        ).andExpect(status().isOk)

        verify(exactly = 1) {
            store.setCoordinate(match { it.dayOfWeek == DayOfWeek.FRIDAY })
        }
    }

    @Test
    @DisplayName("POST - 비 시스템 관리자는 403 (store 미호출)")
    fun update_nonAdmin_forbidden() {
        asNonAdmin()

        mockMvc.perform(post(url).contentType(MediaType.APPLICATION_JSON).content(body()))
            .andExpect(status().isForbidden)

        verify(exactly = 0) { store.setCoordinate(any()) }
    }

    @Test
    @DisplayName("POST - 알 수 없는 요일은 400 INVALID_DAY_OF_WEEK")
    fun update_invalidDayOfWeek_badRequest() {
        asSystemAdmin()

        mockMvc.perform(
            post(url).contentType(MediaType.APPLICATION_JSON).content(body(dayOfWeek = "FUNDAY")),
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.error.code").value("INVALID_DAY_OF_WEEK"))

        verify(exactly = 0) { store.setCoordinate(any()) }
    }

    @Test
    @DisplayName("POST - 위도 범위 초과는 400 (@Valid)")
    fun update_latitudeOutOfRange_badRequest() {
        asSystemAdmin()

        mockMvc.perform(
            post(url).contentType(MediaType.APPLICATION_JSON).content(body(latitude = "91.0")),
        ).andExpect(status().isBadRequest)

        verify(exactly = 0) { store.setCoordinate(any()) }
    }

    @Test
    @DisplayName("POST - 경도 범위 초과는 400 (@Valid)")
    fun update_longitudeOutOfRange_badRequest() {
        asSystemAdmin()

        mockMvc.perform(
            post(url).contentType(MediaType.APPLICATION_JSON).content(body(longitude = "181.0")),
        ).andExpect(status().isBadRequest)

        verify(exactly = 0) { store.setCoordinate(any()) }
    }

    @Test
    @DisplayName("POST - 라벨 공백은 400 (로그 추적 단서 보존)")
    fun update_blankLabel_badRequest() {
        asSystemAdmin()

        mockMvc.perform(
            post(url).contentType(MediaType.APPLICATION_JSON).content(body(label = "  ")),
        ).andExpect(status().isBadRequest)

        verify(exactly = 0) { store.setCoordinate(any()) }
    }

    @Test
    @DisplayName("POST - 라벨에 구분자(|) 포함은 400 (저장 포맷 보호)")
    fun update_labelWithDelimiter_badRequest() {
        asSystemAdmin()

        mockMvc.perform(
            post(url).contentType(MediaType.APPLICATION_JSON).content(body(label = "양구|점")),
        ).andExpect(status().isBadRequest)

        verify(exactly = 0) { store.setCoordinate(any()) }
    }

    @Test
    @DisplayName("POST - 위도 필드 누락은 400 (non-null 역직렬화)")
    fun update_missingLatitude_badRequest() {
        asSystemAdmin()

        mockMvc.perform(
            post(url).contentType(MediaType.APPLICATION_JSON)
                .content("""{"dayOfWeek":"FRIDAY","longitude":128.2,"label":"원통점"}"""),
        ).andExpect(status().isBadRequest)

        verify(exactly = 0) { store.setCoordinate(any()) }
    }

    @Test
    @DisplayName("DELETE - 시스템 관리자는 200 + store.reset 호출")
    fun reset_systemAdmin_ok() {
        asSystemAdmin()
        every { store.reset() } returns Unit

        mockMvc.perform(delete(url))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.customized").value(false))

        verify(exactly = 1) { store.reset() }
    }

    @Test
    @DisplayName("DELETE - 비 시스템 관리자는 403 (store 미호출)")
    fun reset_nonAdmin_forbidden() {
        asNonAdmin()

        mockMvc.perform(delete(url))
            .andExpect(status().isForbidden)

        verify(exactly = 0) { store.reset() }
    }
}
