package com.otoki.powersales.inspection.service

import com.otoki.powersales.account.entity.Account
import com.otoki.powersales.account.repository.AccountRepository
import com.otoki.powersales.common.repository.UploadFileRepository
import com.otoki.powersales.common.service.FileStorageService
import com.otoki.powersales.employee.entity.Employee
import com.otoki.powersales.employee.repository.EmployeeRepository
import com.otoki.powersales.inspection.dto.admin.AdminCreateSiteActivityRequest
import com.otoki.powersales.inspection.dto.admin.AdminUpdateSiteActivityRequest
import com.otoki.powersales.inspection.entity.InspectionTheme
import com.otoki.powersales.inspection.entity.SiteActivity
import com.otoki.powersales.inspection.repository.InspectionThemeRepository
import com.otoki.powersales.inspection.repository.SiteActivityRepository
import com.otoki.powersales.product.entity.Product
import com.otoki.powersales.product.repository.ProductRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import java.util.Optional
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("AdminSiteActivityMutationService 테스트 (현장점검 결과 admin 등록)")
class AdminSiteActivityMutationServiceTest {

    private val siteActivityRepository: SiteActivityRepository = mockk(relaxUnitFun = true)
    private val inspectionThemeRepository: InspectionThemeRepository = mockk()
    private val accountRepository: AccountRepository = mockk()
    private val employeeRepository: EmployeeRepository = mockk()
    private val productRepository: ProductRepository = mockk()
    private val uploadFileRepository: UploadFileRepository = mockk(relaxUnitFun = true)
    private val fileStorageService: FileStorageService = mockk()

    private val service = AdminSiteActivityMutationService(
        siteActivityRepository = siteActivityRepository,
        inspectionThemeRepository = inspectionThemeRepository,
        accountRepository = accountRepository,
        employeeRepository = employeeRepository,
        productRepository = productRepository,
        uploadFileRepository = uploadFileRepository,
        fileStorageService = fileStorageService,
    )

    private fun employee() = Employee(id = 100L, employeeCode = "E100", name = "사원100", costCenterCode = "CC1")
    private fun account() = Account(id = 1, name = "테스트마트", externalKey = "SAP1")
    private fun theme() = InspectionTheme(id = 10L, title = "1분기 점검")

    private fun request(category: String = "OWN") = AdminCreateSiteActivityRequest(
        themeId = 10L,
        accountId = 1,
        employeeId = 100L,
        inspectionDate = "2026-05-01",
        category = category,
        fieldTypeCode = "MAIN_SHELF",
        description = "진열 양호",
        productCode = "P001",
        competitorTasting = true,
        competitorProductPrice = 1500,
        competitorSalesQuantity = 30,
    )

    @Test
    fun `자사 등록 시 제품 lookup + costCenterCode 자동주입 + 변환값을 채운다`() {
        every { employeeRepository.findById(100L) } returns Optional.of(employee())
        every { accountRepository.findById(1) } returns Optional.of(account())
        every { inspectionThemeRepository.findById(10L) } returns Optional.of(theme())
        every { productRepository.findByProductCode("P001") } returns Product(id = 5, name = "제품A", productCode = "P001")
        val saved = slot<SiteActivity>()
        every { siteActivityRepository.save(capture(saved)) } answers { saved.captured }

        val response = service.create(request(), null)

        val c = saved.captured
        assertThat(c.productType).isEqualTo("자사")
        assertThat(c.category).isEqualTo("본매대")
        assertThat(c.product?.id).isEqualTo(5)
        assertThat(c.employee?.id).isEqualTo(100L)
        assertThat(c.costCenterCode).isEqualTo("CC1")  // SiteActivityTrigger.beforeInsert 동등
        assertThat(c.sapAccountCode).isEqualTo("SAP1")
        assertThat(c.sampleTastFlag).isEqualTo("Y")
        assertThat(c.competitorProudctPrice?.toInt()).isEqualTo(1500)
        assertThat(response.id).isEqualTo(c.id)
    }

    @Test
    fun `경쟁사 등록 시 제품 lookup 을 건너뛴다`() {
        every { employeeRepository.findById(100L) } returns Optional.of(employee())
        every { accountRepository.findById(1) } returns Optional.of(account())
        every { inspectionThemeRepository.findById(10L) } returns Optional.of(theme())
        val saved = slot<SiteActivity>()
        every { siteActivityRepository.save(capture(saved)) } answers { saved.captured }

        service.create(request(category = "COMPETITOR").copy(competitorName = "경쟁사X"), null)

        assertThat(saved.captured.productType).isEqualTo("경쟁사")
        assertThat(saved.captured.product).isNull()
        assertThat(saved.captured.competitorName).isEqualTo("경쟁사X")
    }

    @Test
    fun `유효하지 않은 fieldTypeCode 면 예외`() {
        assertThatThrownBy {
            service.create(request().copy(fieldTypeCode = "INVALID"), null)
        }.isInstanceOf(IllegalArgumentException::class.java)
    }

    @Test
    fun `점검 사원을 찾을 수 없으면 예외`() {
        every { employeeRepository.findById(100L) } returns Optional.empty()

        assertThatThrownBy { service.create(request(), null) }
            .isInstanceOf(IllegalArgumentException::class.java)
    }

    private fun updateRequest() = AdminUpdateSiteActivityRequest(
        themeId = 10L,
        accountId = 1,
        employeeId = 100L,
        inspectionDate = "2026-05-02",
        category = "OWN",
        fieldTypeCode = "EVENT_SHELF",
        description = "수정된 설명",
        productCode = "P001",
    )

    @Test
    fun `수정 시 기존 sfid·name 보존 + 본문·lookup 재설정`() {
        val existing = SiteActivity(id = 7L, sfid = "a0X000", name = "SA00000007", isDeleted = false)
        every { siteActivityRepository.findByIdAndIsDeletedFalse(7L) } returns existing
        every { employeeRepository.findById(100L) } returns Optional.of(employee())
        every { accountRepository.findById(1) } returns Optional.of(account())
        every { inspectionThemeRepository.findById(10L) } returns Optional.of(theme())
        every { productRepository.findByProductCode("P001") } returns Product(id = 5, name = "제품A", productCode = "P001")
        val saved = slot<SiteActivity>()
        every { siteActivityRepository.save(capture(saved)) } answers { saved.captured }

        service.update(7L, updateRequest())

        val c = saved.captured
        assertThat(c.id).isEqualTo(7L)
        assertThat(c.sfid).isEqualTo("a0X000")
        assertThat(c.name).isEqualTo("SA00000007")
        assertThat(c.category).isEqualTo("행사매대")
        assertThat(c.description).isEqualTo("수정된 설명")
        assertThat(c.costCenterCode).isEqualTo("CC1")
        assertThat(c.isDeleted).isFalse()
    }

    @Test
    fun `수정 대상이 없으면 예외`() {
        every { siteActivityRepository.findByIdAndIsDeletedFalse(404L) } returns null

        assertThatThrownBy { service.update(404L, updateRequest()) }
            .isInstanceOf(IllegalArgumentException::class.java)
    }

    @Test
    fun `삭제 시 soft delete 로 isDeleted true 저장`() {
        val existing = SiteActivity(id = 7L, name = "SA00000007", isDeleted = false)
        every { siteActivityRepository.findByIdAndIsDeletedFalse(7L) } returns existing
        val saved = slot<SiteActivity>()
        every { siteActivityRepository.save(capture(saved)) } answers { saved.captured }

        service.delete(7L)

        assertThat(saved.captured.id).isEqualTo(7L)
        assertThat(saved.captured.isDeleted).isTrue()
    }
}
