package com.otoki.powersales.inspection.service

import com.otoki.powersales.platform.auth.web.WebUserPrincipal
import com.otoki.powersales.platform.common.repository.UploadFileRepository
import com.otoki.powersales.domain.org.employee.entity.Employee
import com.otoki.powersales.domain.org.employee.repository.EmployeeRepository
import com.otoki.powersales.inspection.dto.admin.CreateThemeRequest
import com.otoki.powersales.inspection.dto.admin.UpdateThemeRequest
import com.otoki.powersales.inspection.entity.InspectionTheme
import com.otoki.powersales.inspection.repository.InspectionThemeRepository
import com.otoki.powersales.inspection.repository.SiteActivityRepository
import com.otoki.powersales.user.entity.User
import com.otoki.powersales.user.repository.UserRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import java.time.LocalDate
import java.util.Optional
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable

@DisplayName("AdminInspectionThemeService 테스트 (현장점검 등록 테마 관리)")
class AdminInspectionThemeServiceTest {

    private val inspectionThemeRepository: InspectionThemeRepository = mockk()
    private val siteActivityRepository: SiteActivityRepository = mockk()
    private val employeeRepository: EmployeeRepository = mockk()
    private val userRepository: UserRepository = mockk()
    private val uploadFileRepository: UploadFileRepository = mockk()

    private val service = AdminInspectionThemeService(
        inspectionThemeRepository = inspectionThemeRepository,
        siteActivityRepository = siteActivityRepository,
        employeeRepository = employeeRepository,
        userRepository = userRepository,
        uploadFileRepository = uploadFileRepository,
    )

    private fun theme(id: Long = 1L, deleted: Boolean = false) = InspectionTheme(
        id = id,
        name = "TM00000001",
        title = "1분기 점검",
        startDate = LocalDate.of(2026, 1, 1),
        endDate = LocalDate.of(2026, 3, 31),
        department = "영업1팀",
        branchCode = "B001",
        publicFlag = true,
        isDeleted = deleted,
    )

    @Nested
    @DisplayName("목록 검색")
    inner class Search {
        @Test
        fun `검색 결과를 목록 항목으로 변환하고 하위 점검결과 수를 채운다`() {
            every { inspectionThemeRepository.searchForAdmin(any(), any(), any(), any()) } returns
                PageImpl(listOf(theme()), Pageable.ofSize(20), 1)
            every { inspectionThemeRepository.countSiteActivitiesByThemeIds(listOf(1L)) } returns
                mapOf(1L to 3L)

            val response = service.search(keyword = null, department = null, branchCode = null, page = 0, size = 20)

            assertThat(response.totalElements).isEqualTo(1)
            assertThat(response.content[0].name).isEqualTo("TM00000001")
            assertThat(response.content[0].siteActivityCount).isEqualTo(3L)
        }

        @Test
        fun `부서·지점코드 필터를 repository 에 그대로 전달한다`() {
            every {
                inspectionThemeRepository.searchForAdmin(any(), "영업1팀", "B001", any())
            } returns PageImpl(emptyList(), Pageable.ofSize(20), 0)
            every { inspectionThemeRepository.countSiteActivitiesByThemeIds(emptyList()) } returns emptyMap()

            service.search(keyword = null, department = "영업1팀", branchCode = "B001", page = 0, size = 20)

            verify {
                inspectionThemeRepository.searchForAdmin(null, "영업1팀", "B001", any())
            }
        }
    }

    @Nested
    @DisplayName("생성")
    inner class Create {
        @Test
        fun `테마번호를 채번하고 생성자 소속을 부서·지점에 자동 주입한다`() {
            val principal: WebUserPrincipal = mockk()
            every { principal.requireEmployeeId() } returns 100L
            every { employeeRepository.findById(100L) } returns Optional.of(
                Employee(id = 100L, employeeCode = "E100", name = "홍길동", orgName = "영업3팀", costCenterCode = "B003")
            )
            every { inspectionThemeRepository.findMaxThemeNumberSequence() } returns 41L
            val saved = slot<InspectionTheme>()
            every { inspectionThemeRepository.save(capture(saved)) } answers { saved.captured }

            val response = service.create(
                principal,
                CreateThemeRequest(title = "신규 테마", startDate = "2026-06-01", endDate = "2026-06-30"),
            )

            assertThat(saved.captured.name).isEqualTo("TM00000042")
            assertThat(saved.captured.department).isEqualTo("영업3팀")
            assertThat(saved.captured.branchCode).isEqualTo("B003")
            // 레거시 Theme__c.PublicFlag__c 기본값 정합 — 생성 시 false 고정(조회 필터엔 미사용).
            assertThat(saved.captured.publicFlag).isFalse()
            assertThat(saved.captured.title).isEqualTo("신규 테마")
            assertThat(response.name).isEqualTo("TM00000042")
        }
    }

    @Nested
    @DisplayName("수정")
    inner class Update {
        @Test
        fun `테마이름·기간만 변경하고 테마번호·부서·지점은 보존한다`() {
            every { inspectionThemeRepository.findById(1L) } returns Optional.of(theme())
            val saved = slot<InspectionTheme>()
            every { inspectionThemeRepository.save(capture(saved)) } answers { saved.captured }

            service.update(1L, UpdateThemeRequest(title = "수정된 테마", startDate = "2026-02-01", endDate = "2026-04-30"))

            assertThat(saved.captured.name).isEqualTo("TM00000001")
            assertThat(saved.captured.department).isEqualTo("영업1팀")
            assertThat(saved.captured.branchCode).isEqualTo("B001")
            assertThat(saved.captured.title).isEqualTo("수정된 테마")
            assertThat(saved.captured.startDate).isEqualTo(LocalDate.of(2026, 2, 1))
        }

        @Test
        fun `소유권 이전 시 새 소유자 소속으로 부서를 갱신하고 지점은 보존한다`() {
            every { inspectionThemeRepository.findById(1L) } returns Optional.of(theme())
            every { userRepository.findById(200L) } returns Optional.of(
                User(id = 200L, username = "newowner", employeeCode = "E200", password = "x")
            )
            every { employeeRepository.findByEmployeeCode("E200") } returns Optional.of(
                Employee(id = 200L, employeeCode = "E200", name = "신소유자", orgName = "영업5팀", costCenterCode = "B005")
            )
            val saved = slot<InspectionTheme>()
            every { inspectionThemeRepository.save(capture(saved)) } answers { saved.captured }

            service.update(
                1L,
                UpdateThemeRequest(title = "1분기 점검", startDate = "2026-01-01", endDate = "2026-03-31", ownerUserId = 200L),
            )

            assertThat(saved.captured.ownerUser?.id).isEqualTo(200L)
            assertThat(saved.captured.department).isEqualTo("영업5팀")
            assertThat(saved.captured.branchCode).isEqualTo("B001")
            assertThat(saved.captured.ownerGroup).isNull()
        }
    }

    @Nested
    @DisplayName("삭제")
    inner class Delete {
        @Test
        fun `soft delete 로 isDeleted 를 true 로 저장한다`() {
            every { inspectionThemeRepository.findById(1L) } returns Optional.of(theme())
            val saved = slot<InspectionTheme>()
            every { inspectionThemeRepository.save(capture(saved)) } answers { saved.captured }

            service.delete(1L)

            assertThat(saved.captured.isDeleted).isTrue()
            verify { inspectionThemeRepository.save(any()) }
        }

        @Test
        fun `이미 삭제된 테마는 조회되지 않아 예외`() {
            every { inspectionThemeRepository.findById(1L) } returns Optional.of(theme(deleted = true))

            assertThatThrownBy { service.delete(1L) }
                .isInstanceOf(IllegalArgumentException::class.java)
        }
    }
}
