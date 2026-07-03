package com.otoki.powersales.domain.activity.inspection.service

import com.otoki.powersales.admin.dto.DataScope
import com.otoki.powersales.admin.service.AdminDataScopeService
import com.otoki.powersales.domain.activity.inspection.exception.InspectionThemeForbiddenException
import com.otoki.powersales.domain.org.organization.entity.Organization
import com.otoki.powersales.domain.org.organization.repository.OrganizationRepository
import com.otoki.powersales.platform.auth.web.WebUserPrincipal
import com.otoki.powersales.platform.common.dto.response.BranchResponse
import com.otoki.powersales.platform.common.repository.UploadFileRepository
import com.otoki.powersales.domain.org.employee.entity.Employee
import com.otoki.powersales.domain.org.employee.repository.EmployeeRepository
import com.otoki.powersales.domain.activity.inspection.dto.admin.CreateThemeRequest
import com.otoki.powersales.domain.activity.inspection.dto.admin.UpdateThemeRequest
import com.otoki.powersales.domain.activity.inspection.entity.InspectionTheme
import com.otoki.powersales.domain.activity.inspection.repository.InspectionThemeRepository
import com.otoki.powersales.domain.activity.inspection.repository.SiteActivityRepository
import com.otoki.powersales.domain.activity.inspection.service.AdminInspectionThemeService
import com.otoki.powersales.user.entity.User
import com.otoki.powersales.user.repository.UserRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
// isNull()/eq() 는 MockKMatcherScope 확장 — every{}/verify{} 블록 내에서 import 없이 사용
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
    private val dataScopeService: AdminDataScopeService = mockk()
    private val organizationRepository: OrganizationRepository = mockk()

    private val service = AdminInspectionThemeService(
        inspectionThemeRepository = inspectionThemeRepository,
        siteActivityRepository = siteActivityRepository,
        employeeRepository = employeeRepository,
        userRepository = userRepository,
        uploadFileRepository = uploadFileRepository,
        dataScopeService = dataScopeService,
        organizationRepository = organizationRepository,
    )

    private fun theme(id: Long = 1L, deleted: Boolean = false, branchCode: String = "B001") = InspectionTheme(
        id = id,
        name = "TM00000001",
        title = "1분기 점검",
        startDate = LocalDate.of(2026, 1, 1),
        endDate = LocalDate.of(2026, 3, 31),
        department = "영업1팀",
        branchCode = branchCode,
        publicFlag = true,
        isDeleted = deleted,
    )

    private val principal: WebUserPrincipal = mockk()

    /** 본인 지점 스코프 세팅 — DataScope.branchCodes 로 조회 지점을 제한. */
    private fun scopeBranches(vararg codes: String) {
        every { dataScopeService.resolve(principal) } returns
            DataScope(branchCodes = codes.toList(), isAllBranches = false)
    }

    /** 전사 권한자 스코프 세팅 — 전건 조회. */
    private fun scopeAllBranches() {
        every { dataScopeService.resolve(principal) } returns
            DataScope(branchCodes = emptyList(), isAllBranches = true)
    }

    private fun org(
        orgCodeLevel5: String? = null,
        orgNameLevel5: String? = null,
        orgCodeLevel4: String? = null,
        orgNameLevel4: String? = null,
    ) = Organization(
        orgCodeLevel5 = orgCodeLevel5,
        orgNameLevel5 = orgNameLevel5,
        orgCodeLevel4 = orgCodeLevel4,
        orgNameLevel4 = orgNameLevel4,
    )

    @Nested
    @DisplayName("목록 검색")
    inner class Search {
        @Test
        fun `검색 결과를 목록 항목으로 변환하고 하위 점검결과 수를 채운다`() {
            scopeBranches("B001")
            every { inspectionThemeRepository.searchForAdmin(any(), any(), any(), any()) } returns
                PageImpl(listOf(theme()), Pageable.ofSize(20), 1)
            every { inspectionThemeRepository.countSiteActivitiesByThemeIds(listOf(1L)) } returns
                mapOf(1L to 3L)

            val response = service.search(principal, keyword = null, department = null, branchCode = null, page = 0, size = 20)

            assertThat(response.totalElements).isEqualTo(1)
            assertThat(response.content[0].name).isEqualTo("TM00000001")
            assertThat(response.content[0].siteActivityCount).isEqualTo(3L)
        }

        @Test
        fun `부서 필터를 repository 에 전달하고 지점은 본인 지점 스코프로 제한한다`() {
            scopeBranches("B001")
            every {
                inspectionThemeRepository.searchForAdmin(any(), "영업1팀", eq(listOf("B001")), any())
            } returns PageImpl(emptyList(), Pageable.ofSize(20), 0)
            every { inspectionThemeRepository.countSiteActivitiesByThemeIds(emptyList()) } returns emptyMap()

            service.search(principal, keyword = null, department = "영업1팀", branchCode = null, page = 0, size = 20)

            verify {
                inspectionThemeRepository.searchForAdmin(null, "영업1팀", eq(listOf("B001")), any())
            }
        }

        @Test
        fun `전사 권한자는 지점 스코프를 null(전건)로 전달한다`() {
            scopeAllBranches()
            every {
                inspectionThemeRepository.searchForAdmin(any(), any(), isNull(), any())
            } returns PageImpl(emptyList(), Pageable.ofSize(20), 0)
            every { inspectionThemeRepository.countSiteActivitiesByThemeIds(emptyList()) } returns emptyMap()

            service.search(principal, keyword = null, department = null, branchCode = null, page = 0, size = 20)

            verify { inspectionThemeRepository.searchForAdmin(any(), any(), isNull(), any()) }
        }

        @Test
        fun `Select 로 권한 지점 안 지점을 고르면 그 지점으로 스코프를 좁힌다`() {
            scopeBranches("B001", "B002", "B003")
            every {
                inspectionThemeRepository.searchForAdmin(any(), any(), eq(listOf("B002")), any())
            } returns PageImpl(emptyList(), Pageable.ofSize(20), 0)
            every { inspectionThemeRepository.countSiteActivitiesByThemeIds(emptyList()) } returns emptyMap()

            service.search(principal, keyword = null, department = null, branchCode = "B002", page = 0, size = 20)

            verify { inspectionThemeRepository.searchForAdmin(any(), any(), eq(listOf("B002")), any()) }
        }

        @Test
        fun `권한 지점 밖을 요청하면 무시하고 전체 권한 지점으로 스코프한다(IDOR 차단)`() {
            scopeBranches("B001", "B002")
            every {
                inspectionThemeRepository.searchForAdmin(any(), any(), eq(listOf("B001", "B002")), any())
            } returns PageImpl(emptyList(), Pageable.ofSize(20), 0)
            every { inspectionThemeRepository.countSiteActivitiesByThemeIds(emptyList()) } returns emptyMap()

            service.search(principal, keyword = null, department = null, branchCode = "Z999", page = 0, size = 20)

            verify { inspectionThemeRepository.searchForAdmin(any(), any(), eq(listOf("B001", "B002")), any()) }
        }

        @Test
        fun `지점 권한이 비어 있으면 쿼리 없이 빈 목록을 반환한다`() {
            scopeBranches()

            val response = service.search(principal, keyword = null, department = null, branchCode = null, page = 0, size = 20)

            assertThat(response.totalElements).isEqualTo(0)
            assertThat(response.content).isEmpty()
            verify(exactly = 0) { inspectionThemeRepository.searchForAdmin(any(), any(), any(), any()) }
        }
    }

    @Nested
    @DisplayName("상세 지점 스코프 가드")
    inner class DetailScope {
        @Test
        fun `본인 지점 스코프에 해당 지점이 있으면 상세를 조회한다`() {
            scopeBranches("Z999")
            every { inspectionThemeRepository.findById(1L) } returns Optional.of(theme(branchCode = "Z999"))
            every { siteActivityRepository.findByInspectionThemeIdForAdmin(1L) } returns emptyList()

            val response = service.getDetail(principal, 1L)

            assertThat(response.id).isEqualTo(1L)
        }

        @Test
        fun `전사 권한자는 타 지점 테마 상세도 조회한다`() {
            scopeAllBranches()
            every { inspectionThemeRepository.findById(1L) } returns Optional.of(theme(branchCode = "Z999"))
            every { siteActivityRepository.findByInspectionThemeIdForAdmin(1L) } returns emptyList()

            val response = service.getDetail(principal, 1L)

            assertThat(response.id).isEqualTo(1L)
        }

        @Test
        fun `본인 지점 스코프 밖 지점 테마 상세를 조회하면 403`() {
            scopeBranches("B001")
            every { inspectionThemeRepository.findById(1L) } returns Optional.of(theme(branchCode = "Z999"))

            assertThatThrownBy { service.getDetail(principal, 1L) }
                .isInstanceOf(InspectionThemeForbiddenException::class.java)
        }

        @Test
        fun `전사공통 화이트리스트 지점 테마는 스코프 밖 사용자도 조회한다`() {
            // 3473 은 COMMON_BRANCH_CODES(전사공통) 소속 — 본인 지점 스코프에 없어도 조회 가능.
            scopeBranches("B001")
            every { inspectionThemeRepository.findById(1L) } returns Optional.of(theme(branchCode = "3473"))
            every { siteActivityRepository.findByInspectionThemeIdForAdmin(1L) } returns emptyList()

            val response = service.getDetail(principal, 1L)

            assertThat(response.id).isEqualTo(1L)
        }

        @Test
        fun `validateThemeScope 는 스코프 밖이면 403`() {
            scopeBranches("B001")
            every { inspectionThemeRepository.findById(1L) } returns Optional.of(theme(branchCode = "Z999"))

            assertThatThrownBy { service.validateThemeScope(principal, 1L) }
                .isInstanceOf(InspectionThemeForbiddenException::class.java)
        }
    }

    @Nested
    @DisplayName("지점 셀렉터 옵션")
    inner class GetBranches {
        @Test
        fun `전사 권한자는 전 지점 목록을 반환한다`() {
            scopeAllBranches()
            every { organizationRepository.findAllTeamScheduleBranches() } returns listOf(
                BranchResponse("5832", "원주1지점"),
                BranchResponse("5481", "창원1지점"),
            )

            val result = service.getBranches(principal)

            assertThat(result).hasSize(2)
            assertThat(result.map { it.branchName }).contains("원주1지점", "창원1지점")
        }

        @Test
        fun `일반 사용자는 본인 지점 단일만 반환한다(조직 트리 확장 없음)`() {
            scopeBranches("5832")
            every { principal.costCenterCode } returns "5832"
            every { organizationRepository.findFirstByAnyOrgCodeLevel("5832") } returns
                org(orgCodeLevel5 = "5832", orgNameLevel5 = "원주1지점")

            val result = service.getBranches(principal)

            assertThat(result).hasSize(1)
            assertThat(result[0].branchCode).isEqualTo("5832")
            assertThat(result[0].branchName).isEqualTo("원주1지점")
        }

        @Test
        fun `costCenterCode 가 없으면 빈 목록`() {
            scopeBranches()
            every { principal.costCenterCode } returns null

            assertThat(service.getBranches(principal)).isEmpty()
        }

        @Test
        fun `Level5 지점명이 없으면 Level4 명으로 대체한다`() {
            scopeBranches("4888")
            every { principal.costCenterCode } returns "4888"
            every { organizationRepository.findFirstByAnyOrgCodeLevel("4888") } returns
                org(orgCodeLevel5 = null, orgNameLevel5 = null, orgCodeLevel4 = "4888", orgNameLevel4 = "영업지원1팀")

            val result = service.getBranches(principal)

            assertThat(result).hasSize(1)
            assertThat(result[0].branchName).isEqualTo("영업지원1팀")
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
