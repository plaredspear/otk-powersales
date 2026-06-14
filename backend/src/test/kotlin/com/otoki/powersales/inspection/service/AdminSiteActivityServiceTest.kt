package com.otoki.powersales.inspection.service

import com.otoki.powersales.domain.foundation.account.entity.Account
import com.otoki.powersales.admin.dto.DataScope
import com.otoki.powersales.platform.auth.sharing.service.SharingRulePolicyEvaluator
import com.otoki.powersales.platform.common.repository.UploadFileRepository
import com.otoki.powersales.platform.common.storage.StorageService
import com.otoki.powersales.platform.common.storage.UploadFileParentTypes
import com.otoki.powersales.employee.entity.Employee
import com.otoki.powersales.inspection.entity.InspectionTheme
import com.otoki.powersales.inspection.entity.SiteActivity
import com.otoki.powersales.inspection.enums.InspectionCategory
import com.otoki.powersales.inspection.enums.InspectionFieldType
import com.otoki.powersales.inspection.repository.SiteActivityRepository
import com.querydsl.core.types.dsl.Expressions
import io.mockk.every
import io.mockk.mockk
import java.time.LocalDate
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable

@DisplayName("AdminSiteActivityService 테스트 (web admin 현장점검 조회)")
class AdminSiteActivityServiceTest {

    private val siteActivityRepository: SiteActivityRepository = mockk()
    private val uploadFileRepository: UploadFileRepository = mockk()
    private val policyEvaluator: SharingRulePolicyEvaluator = mockk()
    // 사진 조회는 presigned URL 발급 — 본 테스트는 사진 없는 케이스라 호출 없음.
    private val storageService: StorageService = mockk()

    private val service = AdminSiteActivityService(
        siteActivityRepository = siteActivityRepository,
        uploadFileRepository = uploadFileRepository,
        policyEvaluator = policyEvaluator,
        storageService = storageService
    )

    private val allowAllScope = DataScope(branchCodes = emptyList(), isAllBranches = true)

    init {
        every { policyEvaluator.buildPredicate(any(), any(), any()) } returns
            Expressions.asBoolean(true).isTrue
    }

    private fun activity(id: Long = 1L) = SiteActivity(
        id = id,
        activityDate = LocalDate.of(2026, 5, 1),
        category = "본매대",
        productType = "자사",
        isDeleted = false,
        account = Account(id = 1, name = "테스트마트", externalKey = "SAP1"),
        employee = Employee(id = 100L, employeeCode = "E100", name = "홍길동", orgName = "영업1팀"),
        inspectionTheme = InspectionTheme(id = 10L, title = "1분기 점검")
    )

    @Nested
    @DisplayName("목록 검색")
    inner class Search {
        @Test
        fun `가시 범위 + 필터로 검색하고 admin 목록 항목으로 변환한다`() {
            every {
                siteActivityRepository.searchForAdmin(any(), any(), any())
            } returns PageImpl(listOf(activity()), Pageable.ofSize(20), 1)

            val response = service.search(
                scope = allowAllScope,
                startDate = LocalDate.of(2026, 5, 1),
                endDate = LocalDate.of(2026, 5, 31),
                category = InspectionCategory.OWN,
                fieldType = InspectionFieldType.MAIN_SHELF,
                employeeName = "홍",
                accountCode = "SAP1",
                page = 0,
                size = 20
            )

            assertThat(response.totalElements).isEqualTo(1)
            assertThat(response.content[0].category).isEqualTo("OWN")
            assertThat(response.content[0].fieldType).isEqualTo("본매대")
            assertThat(response.content[0].employeeName).isEqualTo("홍길동")
            assertThat(response.content[0].employeeOrgName).isEqualTo("영업1팀")
        }

        @Test
        fun `종료일이 시작일보다 빠르면 예외`() {
            assertThatThrownBy {
                service.search(
                    scope = allowAllScope,
                    startDate = LocalDate.of(2026, 5, 31),
                    endDate = LocalDate.of(2026, 5, 1),
                    category = null, fieldType = null, employeeName = null, accountCode = null,
                    page = 0, size = 20
                )
            }.isInstanceOf(IllegalArgumentException::class.java)
        }
    }

    @Nested
    @DisplayName("상세 조회")
    inner class GetDetail {
        @Test
        fun `가시 범위 내 row 는 상세를 반환한다`() {
            every { siteActivityRepository.findByIdAndIsDeletedFalse(1L) } returns activity()
            every { siteActivityRepository.existsVisibleById(any(), 1L) } returns true
            every {
                uploadFileRepository.findByParentTypeAndParentIdAndIsDeletedFalse(UploadFileParentTypes.SITE_ACTIVITY, 1L)
            } returns emptyList()

            val result = service.getDetail(allowAllScope, 1L)

            assertThat(result.id).isEqualTo(1L)
            assertThat(result.themeName).isEqualTo("1분기 점검")
            assertThat(result.employeeName).isEqualTo("홍길동")
        }

        @Test
        fun `가시 범위 밖 row 는 예외 (404 동등)`() {
            every { siteActivityRepository.findByIdAndIsDeletedFalse(1L) } returns activity()
            every { siteActivityRepository.existsVisibleById(any(), 1L) } returns false

            assertThatThrownBy { service.getDetail(allowAllScope, 1L) }
                .isInstanceOf(IllegalArgumentException::class.java)
        }

        @Test
        fun `없는 row 는 예외`() {
            every { siteActivityRepository.findByIdAndIsDeletedFalse(404L) } returns null

            assertThatThrownBy { service.getDetail(allowAllScope, 404L) }
                .isInstanceOf(IllegalArgumentException::class.java)
        }
    }
}
