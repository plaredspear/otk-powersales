package com.otoki.powersales.domain.activity.claim.service

import com.otoki.powersales.domain.foundation.account.entity.Account
import com.otoki.powersales.domain.foundation.account.repository.AccountRepository
import com.otoki.powersales.domain.activity.claim.dto.request.ClaimCreateRequest
import com.otoki.powersales.domain.activity.claim.entity.Claim
import com.otoki.powersales.domain.activity.claim.entity.sfpicklist.RequestType
import com.otoki.powersales.domain.activity.claim.enums.ClaimChannel
import com.otoki.powersales.domain.activity.claim.enums.ClaimDateType
import com.otoki.powersales.domain.activity.claim.enums.ClaimStatus
import com.otoki.powersales.domain.activity.claim.exception.ClaimTypeHierarchyMismatchException
import com.otoki.powersales.domain.activity.claim.exception.InvalidClaimDateException
import com.otoki.powersales.domain.activity.claim.exception.InvalidClaimType1Exception
import com.otoki.powersales.domain.activity.claim.exception.ReceiptRequiredException
import com.otoki.powersales.domain.activity.claim.exception.RequestTypeMaxExceededException
import com.otoki.powersales.domain.activity.claim.event.ClaimRegisteredEvent
import com.otoki.powersales.domain.activity.claim.repository.ClaimDraftRepository
import com.otoki.powersales.domain.activity.claim.repository.ClaimRepository
import com.otoki.powersales.platform.common.entity.UploadFile
import com.otoki.powersales.platform.common.repository.UploadFileRepository
import com.otoki.powersales.platform.common.service.FileStorageService
import com.otoki.powersales.domain.org.employee.entity.Employee
import com.otoki.powersales.domain.org.employee.repository.EmployeeRepository
import com.otoki.powersales.domain.foundation.product.entity.Product
import com.otoki.powersales.domain.foundation.product.repository.ProductRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.springframework.context.ApplicationEventPublisher
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.mock.web.MockMultipartFile
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.support.TransactionCallback
import org.springframework.transaction.support.TransactionTemplate
import java.time.LocalDate
import java.util.Optional
import java.math.BigDecimal
import java.time.temporal.ChronoUnit
import kotlin.reflect.full.findAnnotation

@DisplayName("MobileClaimService 테스트 — 클레임 등록 (UC-02/UC-10)")
class MobileClaimServiceTest {

    private val claimRepository: ClaimRepository = mockk(relaxUnitFun = true)
    private val claimDraftRepository: ClaimDraftRepository = mockk(relaxed = true)
    private val uploadFileRepository: UploadFileRepository = mockk(relaxUnitFun = true)
    private val employeeRepository: EmployeeRepository = mockk()
    private val accountRepository: AccountRepository = mockk()
    private val productRepository: ProductRepository = mockk()
    private val fileStorageService: FileStorageService = mockk(relaxUnitFun = true)
    private val eventPublisher: ApplicationEventPublisher = mockk(relaxUnitFun = true)
    private val txTemplate: TransactionTemplate = mockk()

    // SF push 는 등록 트랜잭션 커밋 후 비동기(ClaimSfPushDispatcher)로 분리됐다 — 본 테스트는 등록(INSERT)과
    // SF 송신 이벤트 발행까지만 검증한다. 실제 SF 호출/status 전이 검증은 ClaimSfDispatchService 테스트 책임.
    private val service = MobileClaimService(
        claimDraftRepository,
        employeeRepository,
        accountRepository,
        productRepository,
        fileStorageService,
        claimRepository,
        uploadFileRepository,
        eventPublisher,
        txTemplate,
    )

    private val userId = 100L

    private lateinit var employee: Employee
    private lateinit var account: Account
    private lateinit var product: Product

    @BeforeEach
    fun setup() {
        employee = Employee(
            id = userId,
            employeeCode = "EMP001",
            name = "홍길동",
            orgName = "FS사업부",
            costCenterCode = "CC100"
        )
        account = Account(id = 1, name = "테스트거래처", branchCode = "B001", externalKey = "SAP001")
        product = Product(id = 1L, name = "테스트제품", productCode = "P0001")

        // txTemplate.execute { ... } 가 람다를 실제 실행하도록 stub (트랜잭션 분할 구조 검증).
        every { txTemplate.execute(any<TransactionCallback<*>>()) } answers {
            firstArg<TransactionCallback<*>>().doInTransaction(mockk(relaxed = true))
        }
    }

    private fun validRequest(
        claimType1: String = "A",
        claimType2: String = "AA",
        date: String = "2026-01-01",
        dateType: String = ClaimDateType.EXPIRY_DATE.name,
        purchaseAmount: BigDecimal? = null,
        purchaseMethodCode: String? = null,
        requestTypeCode: String? = null
    ) = ClaimCreateRequest(
        accountId = 1L,
        productCode = "P0001",
        dateType = dateType,
        date = date,
        claimType1 = claimType1,
        claimType2 = claimType2,
        defectDescription = "포장이 찢어져 있음",
        defectQuantity = BigDecimal.valueOf(2L),
        purchaseAmount = purchaseAmount,
        purchaseMethodCode = purchaseMethodCode,
        requestTypeCode = requestTypeCode
    )

    private fun mockPhoto(name: String) = MockMultipartFile(name, "$name.jpg", "image/jpeg", byteArrayOf(1, 2, 3))

    private fun stubCreateDeps() {
        every { employeeRepository.findById(userId) } returns Optional.of(employee)
        every { accountRepository.findById(1) } returns Optional.of(account)
        every { productRepository.findByProductCode("P0001") } returns product
        val saved = slot<Claim>()
        every { claimRepository.save(capture(saved)) } answers { firstArg() }
        every { fileStorageService.uploadClaimPhoto(any(), any(), any(), any()) } returns
            "uploads/claim/2026/01/01/uuid.jpg"
        every { uploadFileRepository.save(any<UploadFile>()) } answers { firstArg() }
        every { uploadFileRepository.saveAll(any<List<UploadFile>>()) } answers { firstArg() }
    }

    @Test
    @DisplayName("정상 요청 - 클레임 + 사진 2장 저장 (uploadKbn = claim/part)")
    fun createsClaimWithRequiredPhotos() {
        stubCreateDeps()
        val savedSlots = slot<List<UploadFile>>()
        every { uploadFileRepository.saveAll(capture(savedSlots)) } answers { firstArg() }

        val result = service.createClaim(
            userId, validRequest(),
            defectPhoto = mockPhoto("defectPhoto"),
            labelPhoto = mockPhoto("labelPhoto"),
            receiptPhoto = null
        )

        assertThat(result.accountName).isEqualTo("테스트거래처")
        assertThat(result.productCode).isEqualTo("P0001")
        assertThat(savedSlots.captured).hasSize(2)
        assertThat(savedSlots.captured.map { it.uploadKbn }).containsExactly("claim", "part")
    }

    @Test
    @DisplayName("등록 시 channel=CAP INSERT(status=SF_PENDING) + SF 송신 이벤트 발행")
    fun insertsWithCapChannelAndPublishesSfEvent() {
        stubCreateDeps()
        val claimSlot = slot<Claim>()
        // save 가 PK 가 채워진 영속 entity 를 반환하도록 — 이벤트의 claimId 검증용.
        val persisted = Claim(id = 77L, status = ClaimStatus.SF_PENDING, channel = ClaimChannel.CAP)
        every { claimRepository.save(capture(claimSlot)) } returns persisted
        val eventSlot = slot<ClaimRegisteredEvent>()
        every { eventPublisher.publishEvent(capture(eventSlot)) } answers {}

        val result = service.createClaim(userId, validRequest(), mockPhoto("d"), mockPhoto("l"), null)

        // INSERT 시점 channel=CAP (레거시 모바일 정합) + 등록 직후 상태는 SF_PENDING (전송대기)
        assertThat(claimSlot.captured.channel).isEqualTo(ClaimChannel.CAP)
        assertThat(claimSlot.captured.status).isEqualTo(ClaimStatus.SF_PENDING)
        // 응답은 claim 정보만 (SF 결과 미포함)
        assertThat(result.id).isEqualTo(77L)
        // 커밋 후 SF 송신을 트리거할 이벤트가 (PK 가 채워진) claimId 로 발행됨
        verify { eventPublisher.publishEvent(any<ClaimRegisteredEvent>()) }
        assertThat(eventSlot.captured.claimId).isEqualTo(77L)
    }

    @Test
    @DisplayName("개인카드(B) + 영수증 미첨부 - ReceiptRequiredException")
    fun rejectsPersonalCardWithoutReceipt() {
        every { employeeRepository.findById(userId) } returns Optional.of(employee)
        every { accountRepository.findById(1) } returns Optional.of(account)
        every { productRepository.findByProductCode("P0001") } returns product

        assertThatThrownBy {
            service.createClaim(
                userId, validRequest(purchaseMethodCode = "B"),
                mockPhoto("d"), mockPhoto("l"), receiptPhoto = null
            )
        }.isInstanceOf(ReceiptRequiredException::class.java)
    }

    @Test
    @DisplayName("법인카드(A) + 영수증 미첨부 - 허용 (영수증 면제)")
    fun allowsCorporateCardWithoutReceipt() {
        stubCreateDeps()

        service.createClaim(
            userId, validRequest(purchaseMethodCode = "A"),
            mockPhoto("d"), mockPhoto("l"), receiptPhoto = null
        )

        verify { claimRepository.save(any<Claim>()) }
    }

    @Test
    @DisplayName("ClaimType2.parent != ClaimType1 - hierarchy mismatch 예외")
    fun rejectsHierarchyMismatch() {
        every { employeeRepository.findById(userId) } returns Optional.of(employee)
        every { accountRepository.findById(1) } returns Optional.of(account)
        every { productRepository.findByProductCode("P0001") } returns product

        assertThatThrownBy {
            service.createClaim(
                userId, validRequest(claimType1 = "A", claimType2 = "BA"),
                mockPhoto("d"), mockPhoto("l"), null
            )
        }.isInstanceOf(ClaimTypeHierarchyMismatchException::class.java)
    }

    @Test
    @DisplayName("ClaimType1 유효값 외 - InvalidClaimType1Exception")
    fun rejectsInvalidClaimType1() {
        every { employeeRepository.findById(userId) } returns Optional.of(employee)
        every { accountRepository.findById(1) } returns Optional.of(account)
        every { productRepository.findByProductCode("P0001") } returns product

        assertThatThrownBy {
            service.createClaim(
                userId, validRequest(claimType1 = "Z"), mockPhoto("d"), mockPhoto("l"), null
            )
        }.isInstanceOf(InvalidClaimType1Exception::class.java)
    }

    @Test
    @DisplayName("제조일자 미래 - InvalidClaimDateException")
    fun rejectsFutureManufactureDate() {
        every { employeeRepository.findById(userId) } returns Optional.of(employee)
        every { accountRepository.findById(1) } returns Optional.of(account)
        every { productRepository.findByProductCode("P0001") } returns product
        val futureDate = LocalDate.now().plus(1, ChronoUnit.DAYS).toString()

        assertThatThrownBy {
            service.createClaim(
                userId,
                validRequest(dateType = ClaimDateType.MANUFACTURE_DATE.name, date = futureDate),
                mockPhoto("d"), mockPhoto("l"), null
            )
        }.isInstanceOf(InvalidClaimDateException::class.java)
            .hasMessageContaining("제조일자")
    }

    @Test
    @DisplayName("유통기한 미래 - 허용")
    fun allowsFutureExpiryDate() {
        stubCreateDeps()
        val futureDate = LocalDate.now().plus(30, ChronoUnit.DAYS).toString()

        service.createClaim(
            userId,
            validRequest(dateType = ClaimDateType.EXPIRY_DATE.name, date = futureDate),
            mockPhoto("d"), mockPhoto("l"), null
        )

        verify { claimRepository.save(any<Claim>()) }
    }

    @Test
    @DisplayName("요청사항 4개 초과 - RequestTypeMaxExceededException")
    fun rejectsMoreThanFourRequestTypes() {
        every { employeeRepository.findById(userId) } returns Optional.of(employee)
        every { accountRepository.findById(1) } returns Optional.of(account)
        every { productRepository.findByProductCode("P0001") } returns product
        val joined = listOf(
            RequestType.OPINION_REPORT.displayName,
            RequestType.CONSULTATION.displayName,
            RequestType.URGENT_FS.displayName,
            RequestType.SALE_CANCEL_NEEDED.displayName,
            RequestType.PRODUCTION_SCHEDULE_ADJUSTMENT.displayName
        ).joinToString(";")

        assertThatThrownBy {
            service.createClaim(
                userId, validRequest(requestTypeCode = joined),
                mockPhoto("d"), mockPhoto("l"), null
            )
        }.isInstanceOf(RequestTypeMaxExceededException::class.java)
    }

    @Test
    @DisplayName("SF ClaimRegist 정합 - claim.costCenterCode 는 거래처 branchCode 로 자동 채움")
    fun fillsCostCenterCodeFromAccountBranch() {
        stubCreateDeps()
        val claimSlot = slot<Claim>()
        every { claimRepository.save(capture(claimSlot)) } answers { firstArg() }

        service.createClaim(userId, validRequest(), mockPhoto("d"), mockPhoto("l"), null)

        // SF ClaimRegist.cls:90 정합 — 거래처(Account) BranchCode 기준 (사원 costCenterCode 아님)
        assertThat(claimSlot.captured.costCenterCode).isEqualTo("B001")
        // SF 정합 — division 은 등록 시 공란 (컨트롤러 미설정 + 트리거 Interface 가드 미실행)
        assertThat(claimSlot.captured.division).isNull()
    }

    @Test
    @DisplayName("정식 등록 성공 시 해당 사원의 임시저장(draft) 삭제")
    fun deletesDraftAfterRegister() {
        stubCreateDeps()
        val draft = mockk<com.otoki.powersales.domain.activity.claim.entity.ClaimDraft>(relaxed = true)
        every { claimDraftRepository.findByEmployeeId(userId) } returns draft

        service.createClaim(userId, validRequest(), mockPhoto("d"), mockPhoto("l"), null)

        verify { claimDraftRepository.delete(draft) }
    }

    @Test
    @DisplayName("등록 경로는 @Transactional(propagation=NEVER) 로 read-only 상속 능동 차단 - 회귀 가드")
    fun registrationPathGuardsAgainstReadOnlyInheritance() {
        // 회귀 가드: createClaim 은 txTemplate(REQUIRED)으로 트랜잭션을 직접 관리한다.
        // 진입 시점에 트랜잭션(특히 readOnly)이 있으면 내부 txTemplate 이 거기 참여해 INSERT 가
        // "cannot execute INSERT in a read-only transaction" 으로 막힌다.
        // ① 클래스 레벨 @Transactional 이 없어야 하고(상속 차단), ② 진입 메서드는 NEVER 로 능동 차단.
        assertThat(MobileClaimService::class.findAnnotation<Transactional>())
            .withFailMessage("MobileClaimService 에 클래스 레벨 @Transactional 을 두면 read-only 상속 위험")
            .isNull()

        assertThat(MobileClaimService::createClaim.findAnnotation<Transactional>()?.propagation)
            .withFailMessage("MobileClaimService.createClaim 은 @Transactional(propagation=NEVER) 여야 한다")
            .isEqualTo(Propagation.NEVER)
    }
}
