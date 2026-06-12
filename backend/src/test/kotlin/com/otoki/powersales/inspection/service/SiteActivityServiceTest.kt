package com.otoki.powersales.inspection.service

import com.otoki.powersales.account.entity.Account
import com.otoki.powersales.account.repository.AccountRepository
import com.otoki.powersales.common.entity.UploadFile
import com.otoki.powersales.common.repository.UploadFileRepository
import com.otoki.powersales.common.service.FileStorageService
import com.otoki.powersales.common.storage.StorageService
import com.otoki.powersales.common.storage.UploadFileParentTypes
import com.otoki.powersales.employee.entity.Employee
import com.otoki.powersales.employee.repository.EmployeeRepository
import com.otoki.powersales.inspection.dto.request.InspectionRegisterRequest
import com.otoki.powersales.inspection.entity.InspectionTheme
import com.otoki.powersales.inspection.entity.SiteActivity
import com.otoki.powersales.inspection.enums.InspectionCategory
import com.otoki.powersales.inspection.repository.InspectionThemeRepository
import com.otoki.powersales.inspection.repository.SiteActivityDraftRepository
import com.otoki.powersales.inspection.repository.SiteActivityRepository
import com.otoki.powersales.product.repository.ProductRepository
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
import org.springframework.mock.web.MockMultipartFile

@DisplayName("SiteActivityService 테스트 (현장점검 backend 도메인)")
class SiteActivityServiceTest {

    private val siteActivityRepository: SiteActivityRepository = mockk(relaxUnitFun = true)
    private val inspectionThemeRepository: InspectionThemeRepository = mockk()
    private val accountRepository: AccountRepository = mockk()
    private val employeeRepository: EmployeeRepository = mockk()
    private val productRepository: ProductRepository = mockk()
    private val uploadFileRepository: UploadFileRepository = mockk(relaxUnitFun = true)
    private val fileStorageService: FileStorageService = mockk()
    // 사진 조회는 presigned URL 발급(storageService.getPresignedUrl) — 본 테스트는 사진 없는 케이스라 호출 없음.
    private val storageService: StorageService = mockk()
    // 등록 성공 시 임시저장 삭제 호출 — findByEmployeeId 는 기본 null(삭제 미수행)로 동작한다.
    private val siteActivityDraftRepository: SiteActivityDraftRepository = mockk(relaxed = true)

    private val service = SiteActivityService(
        siteActivityRepository = siteActivityRepository,
        inspectionThemeRepository = inspectionThemeRepository,
        accountRepository = accountRepository,
        employeeRepository = employeeRepository,
        productRepository = productRepository,
        uploadFileRepository = uploadFileRepository,
        fileStorageService = fileStorageService,
        storageService = storageService,
        siteActivityDraftRepository = siteActivityDraftRepository
    )

    private fun account(id: Long = 1L, name: String = "테스트마트") =
        Account(id = id, name = name, externalKey = "SAP$id")

    private fun employee(id: Long = 100L) =
        Employee(id = id, employeeCode = "E$id", name = "사원$id", costCenterCode = "CC1")

    private fun theme(id: Long = 10L) =
        InspectionTheme(id = id, title = "1분기 점검")

    private fun activity(
        id: Long = 1L,
        emp: Employee = employee(),
        acc: Account = account(),
        productType: String? = "자사",
        category: String? = "본매대"
    ) = SiteActivity(
        id = id,
        activityDate = LocalDate.of(2026, 5, 1),
        category = category,
        productType = productType,
        isDeleted = false,
        account = acc,
        employee = emp,
        inspectionTheme = theme()
    )

    @Nested
    @DisplayName("목록 조회")
    inner class GetList {
        @Test
        fun `productType-자사를 OWN 으로, category 한글을 fieldType 으로 변환한다`() {
            every {
                siteActivityRepository.searchByEmployee(100L, null, null, any(), any())
            } returns listOf(activity(productType = "자사", category = "본매대"))

            val result = service.getList(
                employeeId = 100L,
                accountId = null,
                category = null,
                fromDate = LocalDate.of(2026, 5, 1),
                toDate = LocalDate.of(2026, 5, 31)
            )

            assertThat(result).hasSize(1)
            assertThat(result[0].category).isEqualTo("OWN")
            assertThat(result[0].fieldType).isEqualTo("본매대")
            assertThat(result[0].fieldTypeCode).isEqualTo("MAIN_SHELF")
            assertThat(result[0].accountName).isEqualTo("테스트마트")
        }
    }

    @Nested
    @DisplayName("상세 조회")
    inner class GetDetail {
        @Test
        fun `본인 row 는 상세를 반환한다`() {
            every { siteActivityRepository.findByIdAndIsDeletedFalse(1L) } returns activity(emp = employee(100L))
            every {
                uploadFileRepository.findByParentTypeAndParentIdAndIsDeletedFalse(UploadFileParentTypes.SITE_ACTIVITY, 1L)
            } returns emptyList()

            val result = service.getDetail(1L, 100L)

            assertThat(result.id).isEqualTo(1L)
            assertThat(result.themeName).isEqualTo("1분기 점검")
            assertThat(result.photos).isEmpty()
        }

        @Test
        fun `타인 row 조회 시 예외`() {
            every { siteActivityRepository.findByIdAndIsDeletedFalse(1L) } returns activity(emp = employee(100L))

            assertThatThrownBy { service.getDetail(1L, 999L) }
                .isInstanceOf(IllegalArgumentException::class.java)
        }

        @Test
        fun `없는 ID 조회 시 예외`() {
            every { siteActivityRepository.findByIdAndIsDeletedFalse(404L) } returns null

            assertThatThrownBy { service.getDetail(404L, 100L) }
                .isInstanceOf(IllegalArgumentException::class.java)
        }
    }

    @Nested
    @DisplayName("등록")
    inner class Register {
        private fun request(category: InspectionCategory = InspectionCategory.OWN) =
            InspectionRegisterRequest(
                themeId = 10L,
                category = category,
                accountId = 1,
                inspectionDate = LocalDate.of(2026, 5, 1),
                fieldTypeCode = "MAIN_SHELF",
                description = "진열 양호",
                productCode = "P001",
                competitorTasting = true,
                competitorProductPrice = 1500,
                competitorSalesQuantity = 30
            )

        @Test
        fun `lookup 후 SiteActivity 를 저장하고 변환값을 채운다`() {
            every { employeeRepository.findById(100L) } returns Optional.of(employee(100L))
            every { accountRepository.findById(1) } returns Optional.of(account())
            every { inspectionThemeRepository.findById(10L) } returns Optional.of(theme())
            every { productRepository.findByProductCode("P001") } returns null
            val saved = slot<SiteActivity>()
            every { siteActivityRepository.save(capture(saved)) } answers { saved.captured.apply { } }

            service.register(100L, request(), null)

            val captured = saved.captured
            assertThat(captured.productType).isEqualTo("자사")
            assertThat(captured.category).isEqualTo("본매대")
            assertThat(captured.sampleTastFlag).isEqualTo("Y")
            assertThat(captured.competitorProudctPrice?.toInt()).isEqualTo(1500)
            assertThat(captured.salesQuantity?.toInt()).isEqualTo(30)
            assertThat(captured.costCenterCode).isEqualTo("CC1")
            assertThat(captured.sapAccountCode).isEqualTo("SAP1")
        }

        @Test
        fun `사진이 MAX 초과면 예외`() {
            val photos = (1..11).map {
                MockMultipartFile("photos", "p$it.jpg", "image/jpeg", byteArrayOf(1))
            }

            assertThatThrownBy { service.register(100L, request(), photos) }
                .isInstanceOf(IllegalArgumentException::class.java)
        }

        @Test
        fun `유효하지 않은 fieldTypeCode 면 예외`() {
            assertThatThrownBy {
                service.register(100L, request().copy(fieldTypeCode = "INVALID"), null)
            }.isInstanceOf(IllegalArgumentException::class.java)
        }

        @Test
        fun `사진 첨부 시 UploadFile 을 SITE_ACTIVITY parentType 으로 저장한다`() {
            every { employeeRepository.findById(100L) } returns Optional.of(employee(100L))
            every { accountRepository.findById(1) } returns Optional.of(account())
            every { inspectionThemeRepository.findById(10L) } returns Optional.of(theme())
            every { productRepository.findByProductCode("P001") } returns null
            every { siteActivityRepository.save(any()) } answers { (it.invocation.args[0] as SiteActivity) }
            every { fileStorageService.uploadSiteActivityPhoto(any(), any()) } returns "uploads/site-activity/2026/05/01/x.jpg"
            val savedFile = slot<UploadFile>()
            every { uploadFileRepository.save(capture(savedFile)) } answers { savedFile.captured }

            val photos = listOf(MockMultipartFile("photos", "p.jpg", "image/jpeg", byteArrayOf(1, 2, 3)))
            service.register(100L, request(), photos)

            verify(exactly = 1) { fileStorageService.uploadSiteActivityPhoto(any(), any()) }
            assertThat(savedFile.captured.parentType).isEqualTo(UploadFileParentTypes.SITE_ACTIVITY)
        }
    }

    @Nested
    @DisplayName("현장유형")
    inner class FieldTypes {
        @Test
        fun `4개 현장유형 코드를 반환한다`() {
            val result = service.getFieldTypes()
            assertThat(result.map { it.code })
                .containsExactly("MAIN_SHELF", "EVENT_SHELF", "TASTING", "ETC")
            assertThat(result.first { it.code == "MAIN_SHELF" }.name).isEqualTo("본매대")
        }
    }
}
