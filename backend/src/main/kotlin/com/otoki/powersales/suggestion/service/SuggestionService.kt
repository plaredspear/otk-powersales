package com.otoki.powersales.suggestion.service

import com.otoki.powersales.account.repository.AccountRepository
import com.otoki.powersales.common.entity.UploadFile
import com.otoki.powersales.common.repository.UploadFileRepository
import com.otoki.powersales.common.service.FileStorageService
import com.otoki.powersales.employee.repository.EmployeeRepository
import com.otoki.powersales.organization.service.OrgCostCenterMatchService
import com.otoki.powersales.product.entity.Product
import com.otoki.powersales.product.enums.StorageCondition
import com.otoki.powersales.product.repository.ProductRepository
import com.otoki.powersales.suggestion.dto.request.SuggestionCreateRequest
import com.otoki.powersales.suggestion.dto.request.SuggestionUpdateRequest
import com.otoki.powersales.suggestion.dto.response.SuggestionAttachment
import com.otoki.powersales.suggestion.dto.response.SuggestionCreateResponse
import com.otoki.powersales.suggestion.dto.response.SuggestionListItem
import com.otoki.powersales.suggestion.dto.response.SuggestionResponse
import com.otoki.powersales.suggestion.entity.Suggestion
import com.otoki.powersales.suggestion.entity.SuggestionCategory
import com.otoki.powersales.suggestion.entity.SuggestionStatus
import com.otoki.powersales.suggestion.exception.InvalidSuggestionIdException
import com.otoki.powersales.suggestion.exception.SuggestionAccessDeniedException
import com.otoki.powersales.suggestion.exception.SuggestionNotFoundException
import com.otoki.powersales.suggestion.repository.SuggestionRepository
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * 제안 Service — Spec #664 P2-B.
 *
 * ## 레거시 매핑
 * - SF Apex: `IF_REST_MOBILE_ProposalRegist.cls#doPost`, `ProposalTriggerHandler.cls#beforeInsertProposal`
 * - origin spec: #664 P2-B
 *
 * ## 신규 차이
 * - **R10-bis 자동 보강** — `@Transactional` 로 parent (Suggestion) + child (UploadFile N건) 단일 트랜잭션
 * - **BR1~BR7 Category 분기 검증** — `SuggestionValidator` 이식 (P2-B §2.4)
 * - **5종 trigger 부수효과 이식** — service create() mapping step (P2-B §2.5)
 * - **R17 WERK bug 레거시 동등 재현 (Q3 옵션 B)** — `Product.storageCondition` 가 SF picklist 2값 (`실온` / `냉장`)
 *   만 가지므로 레거시 분기 조건 `'냉동/냉장'` 은 실제로 실행되지 않을 가능성. 그러나 명시적 분기 코드는
 *   레거시 동등하게 작성하여 정합성 보존.
 * - **proposal_number 채번 (Q12)** — `nextProposalNumberSeqValue()` 의 nextval → race-free 합성
 */
@Service
@Transactional(readOnly = true)
class SuggestionService(
    private val suggestionRepository: SuggestionRepository,
    private val uploadFileRepository: UploadFileRepository,
    private val accountRepository: AccountRepository,
    private val employeeRepository: EmployeeRepository,
    private val productRepository: ProductRepository,
    private val orgCostCenterMatchService: OrgCostCenterMatchService,
    private val fileStorageService: FileStorageService,
    private val validator: SuggestionValidator,
    @Value("\${app.aws.s3.bucket:otoki-bucket}")
    private val s3BucketName: String,
    @Value("\${app.aws.s3.region:ap-northeast-2}")
    private val s3Region: String
) {

    companion object {
        private const val MAX_PHOTO_COUNT = 10
        private val PROPOSAL_NUMBER_DATE_FMT: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMdd")
    }

    @Transactional
    fun create(employeeId: Long, request: SuggestionCreateRequest, photos: List<MultipartFile>?): SuggestionCreateResponse {
        // step 1~2 (validate)
        val category = request.category ?: throw IllegalArgumentException("category is required")
        validator.validate(
            category = category,
            claimType = request.claimType,
            claimDate = request.claimDate,
            carNumber = request.carNumber,
            duplicateProposalNum = request.duplicateProposalNum,
            actionStatus = request.actionStatus
        )

        if ((photos?.size ?: 0) > MAX_PHOTO_COUNT) {
            throw IllegalArgumentException("첨부 파일은 최대 ${MAX_PHOTO_COUNT}건 입니다")
        }

        // step 3 (lookup)
        val employee = employeeRepository.findById(employeeId)
            .orElseThrow { IllegalArgumentException("사원을 찾을 수 없습니다") }

        val product: Product? = request.productCode
            ?.takeIf { it.isNotBlank() }
            ?.let { productRepository.findByProductCode(it) }

        val account = request.accountId?.let {
            accountRepository.findById(it.toInt()).orElse(null)
        }

        // step 4 (mapping — 5종 trigger 부수효과 이식 + R17 WERK)
        val (receptionCenter, responsibleCenter) = computeWerkCenters(account = account, product = product)
        val orgCostCenterCode = employee.costCenterCode
            ?.takeIf { it.isNotBlank() }
            ?.let { orgCostCenterMatchService.findMatchingCostCenterCode(it).orElse(null) }

        val proposalNumber = generateProposalNumber(LocalDate.now())

        val suggestion = Suggestion(
            proposalNumber = proposalNumber,
            title = request.title ?: throw IllegalArgumentException("제목은 필수입니다"),
            content = request.content ?: throw IllegalArgumentException("내용은 필수입니다"),
            category = category,
            category1 = product?.productCategory1,
            category2 = product?.productCategory2,
            category3 = product?.productCategory3,
            sapAccountCode = request.sapAccountCode,
            productCode = product?.productCode ?: request.productCode,
            orgCostCenterCode = orgCostCenterCode,
            carNumber = request.carNumber,
            claimDate = request.claimDate,
            claimType = request.claimType,
            // Q9 옵션 1 — service 단 자동 복사 (레거시 동등)
            claimTypeMeasures = request.claimType,
            logisticsResponsibility = request.logisticsResponsibility,
            receptionLogisticsCenter = receptionCenter,
            responsibleLogisticsCenter = responsibleCenter,
            status = SuggestionStatus.SUBMITTED,
            actionStatus = request.actionStatus,
            duplicateProposalNum = request.duplicateProposalNum,
            isDeleted = false,
            account = account,
            employee = employee
        )

        // step 5 — INSERT + 첨부 N건
        val saved = suggestionRepository.save(suggestion)

        val attachments = photos?.mapIndexedNotNull { index, file ->
            if (file.isEmpty) return@mapIndexedNotNull null
            val key = fileStorageService.uploadSuggestionPhoto(file, saved.id)
            val uploadFile = UploadFile(
                name = file.originalFilename,
                uniqueKey = key,
                fileSize = formatFileSize(file.size),
                parentType = "SUGGESTION",
                parentId = saved.id,
                isDeleted = false
            )
            val savedFile = uploadFileRepository.save(uploadFile)
            SuggestionAttachment(
                id = savedFile.id,
                s3Url = composeS3Url(key),
                fileName = file.originalFilename,
                sortOrder = index
            )
        } ?: emptyList()

        return SuggestionCreateResponse(
            id = saved.id,
            proposalNumber = saved.proposalNumber,
            attachments = attachments
        )
    }

    fun getDetail(suggestionId: Long, requesterEmployeeId: Long): SuggestionResponse {
        if (suggestionId <= 0) throw InvalidSuggestionIdException()
        val suggestion = suggestionRepository.findByIdAndIsDeletedFalse(suggestionId)
            ?: throw SuggestionNotFoundException()

        // 본인만 조회 허용 (관리자 정책은 별 endpoint)
        if (suggestion.employee?.id != requesterEmployeeId) {
            throw SuggestionAccessDeniedException()
        }

        val attachments = uploadFileRepository
            .findByParentTypeAndParentIdAndIsDeletedFalse("SUGGESTION", suggestion.id)
            .filter { !it.uniqueKey.isNullOrBlank() }
            .sortedBy { it.createdAt }
            .mapIndexed { index, file ->
                SuggestionAttachment(
                    id = file.id,
                    s3Url = composeS3Url(file.uniqueKey!!),
                    fileName = file.name,
                    sortOrder = index
                )
            }

        return SuggestionResponse.from(suggestion, attachments)
    }

    fun listMine(employeeId: Long, page: Int, size: Int): Page<SuggestionListItem> {
        val pageable: Pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))
        return suggestionRepository
            .findByEmployeeIdAndIsDeletedFalseOrderByCreatedAtDesc(employeeId, pageable)
            .map { SuggestionListItem.from(it) }
    }

    @Transactional
    fun update(suggestionId: Long, requesterEmployeeId: Long, request: SuggestionUpdateRequest): SuggestionResponse {
        if (suggestionId <= 0) throw InvalidSuggestionIdException()
        val suggestion = suggestionRepository.findByIdAndIsDeletedFalse(suggestionId)
            ?: throw SuggestionNotFoundException()

        if (suggestion.employee?.id != requesterEmployeeId) {
            throw SuggestionAccessDeniedException()
        }

        val category = request.category ?: throw IllegalArgumentException("category is required")
        validator.validate(
            category = category,
            claimType = request.claimType,
            claimDate = request.claimDate,
            carNumber = request.carNumber,
            duplicateProposalNum = request.duplicateProposalNum,
            actionStatus = request.actionStatus
        )

        suggestion.title = request.title ?: throw IllegalArgumentException("제목은 필수입니다")
        suggestion.content = request.content ?: throw IllegalArgumentException("내용은 필수입니다")
        suggestion.category = category
        suggestion.claimType = request.claimType
        suggestion.claimTypeMeasures = request.claimType
        suggestion.claimDate = request.claimDate
        suggestion.carNumber = request.carNumber
        suggestion.logisticsResponsibility = request.logisticsResponsibility
        suggestion.actionStatus = request.actionStatus
        suggestion.duplicateProposalNum = request.duplicateProposalNum

        val saved = suggestionRepository.save(suggestion)
        return getDetail(saved.id, requesterEmployeeId)
    }

    @Transactional
    fun softDelete(suggestionId: Long, requesterEmployeeId: Long) {
        if (suggestionId <= 0) throw InvalidSuggestionIdException()
        val suggestion = suggestionRepository.findByIdAndIsDeletedFalse(suggestionId)
            ?: throw SuggestionNotFoundException()
        if (suggestion.employee?.id != requesterEmployeeId) {
            throw SuggestionAccessDeniedException()
        }
        suggestion.isDeleted = true
        suggestionRepository.save(suggestion)
    }

    /**
     * `proposal_number` 채번 — Spec #664 Q12 채택 (DB Sequence + 패딩, race condition free).
     *
     * 형식: `S-YYYYMMDD-NNNNNN` (NNNNNN = `suggestion_proposal_number_seq` nextval, 6자리 zero-padded).
     * PostgreSQL `nextval()` 원자성 보장.
     */
    private fun generateProposalNumber(date: LocalDate): String {
        val seq = suggestionRepository.nextProposalNumberSeqValue()
        return "S-${date.format(PROPOSAL_NUMBER_DATE_FMT)}-${"%06d".format(seq)}"
    }

    /**
     * R17 WERK 분기 — Q3 옵션 B (레거시 동등 재현). `ProposalTriggerHandler.cls:89-95`.
     *
     * 레거시 분기 조건 문자열 `'냉동/냉장'` 은 SF picklist 실측 값 `실온` / `냉장` 과 매치 안 되어 실제 운영
     * row 는 거의 else 분기로 빠짐 (R17 bug 정황). 그러나 명시적 분기 코드는 레거시 동등하게 작성.
     */
    private fun computeWerkCenters(
        account: com.otoki.powersales.account.entity.Account?,
        product: Product?
    ): Pair<String?, String?> {
        if (account == null) return null to null
        val productCondition = product?.storageCondition
        return if (productCondition == StorageCondition.REFRIGERATED) {
            // 레거시 SF 분기 — '냉동/냉장' (실측 picklist 2값 중 매치 안 됨). 그러나 명시적 분기 보존.
            // bug: 양쪽 모두 werk3Tx 할당.
            account.werk3Tx to account.werk3Tx
        } else {
            // else 분기: 양쪽 모두 werk1Tx 할당.
            account.werk1Tx to account.werk1Tx
        }
    }

    private fun composeS3Url(key: String): String =
        "https://$s3BucketName.s3.$s3Region.amazonaws.com/$key"

    private fun formatFileSize(bytes: Long): String =
        when {
            bytes >= 1024 * 1024 -> "%.1fMB".format(bytes / (1024.0 * 1024.0))
            bytes >= 1024 -> "%.1fKB".format(bytes / 1024.0)
            else -> "${bytes}B"
        }
}
