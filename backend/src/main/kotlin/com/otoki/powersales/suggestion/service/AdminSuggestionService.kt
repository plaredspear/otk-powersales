package com.otoki.powersales.suggestion.service

import com.otoki.powersales.account.repository.AccountRepository
import com.otoki.powersales.common.entity.UploadFile
import com.otoki.powersales.common.repository.UploadFileRepository
import com.otoki.powersales.common.service.FileStorageService
import com.otoki.powersales.common.storage.UploadFileParentTypes
import com.otoki.powersales.employee.repository.EmployeeRepository
import com.otoki.powersales.organization.service.OrgCostCenterMatchService
import com.otoki.powersales.product.entity.Product
import com.otoki.powersales.product.repository.ProductRepository
import com.otoki.powersales.suggestion.dto.admin.AdminSuggestionCreateRequest
import com.otoki.powersales.suggestion.dto.admin.AdminSuggestionDetailResponse
import com.otoki.powersales.suggestion.dto.admin.AdminSuggestionFilter
import com.otoki.powersales.suggestion.dto.admin.AdminSuggestionListItem
import com.otoki.powersales.suggestion.dto.admin.AdminSuggestionListResponse
import com.otoki.powersales.suggestion.dto.admin.AdminSuggestionUpdateRequest
import com.otoki.powersales.suggestion.dto.response.SuggestionAttachment
import com.otoki.powersales.suggestion.dto.response.SuggestionCreateResponse
import com.otoki.powersales.suggestion.entity.Suggestion
import com.otoki.powersales.suggestion.entity.SuggestionStatus
import com.otoki.powersales.suggestion.exception.InvalidSuggestionIdException
import com.otoki.powersales.suggestion.exception.InvalidSuggestionPhotoIdException
import com.otoki.powersales.suggestion.exception.SuggestionNotFoundException
import com.otoki.powersales.suggestion.exception.SuggestionPhotoNotFoundException
import com.otoki.powersales.suggestion.repository.SuggestionRepository
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

/**
 * admin 권한 제안 Service — Spec #830 P1-B.
 *
 * ## 레거시 매핑
 * - SF Lightning Record Page (`FlexiPage17`) + 표준 List View — 데스크탑 사용자의 admin 권한 진입점.
 * - origin spec: #830 P1-B.
 *
 * ## 신규 차이
 * - **본인 row 검증 미실시** — mobile [SuggestionService] 와의 핵심 차이. SF Permission 기반 권한 평가 (@RequiresSfPermission) 가
 *   Controller 단에서 통과되면 admin 은 전체 row 조회/편집/삭제.
 * - **공통 검증 / 자동 채움 재사용** — [SuggestionValidator] (BR1~BR7) + [SuggestionService] 의 internal helper
 *   (`computeWerkCenters` / `generateProposalNumber` / `composeS3Url` / `formatFileSize`) 호출.
 * - **수정 시 자동 채움 미실행** — 레거시 SF Trigger 의 `beforeInsert` 동등 정합. admin 이 거래처/제품을 변경하면
 *   물류센터 등 종속 필드는 수동 입력.
 */
@Service
@Transactional(readOnly = true)
class AdminSuggestionService(
    private val suggestionRepository: SuggestionRepository,
    private val uploadFileRepository: UploadFileRepository,
    private val accountRepository: AccountRepository,
    private val employeeRepository: EmployeeRepository,
    private val productRepository: ProductRepository,
    private val orgCostCenterMatchService: OrgCostCenterMatchService,
    private val fileStorageService: FileStorageService,
    private val validator: SuggestionValidator,
    private val suggestionService: SuggestionService
) {

    companion object {
        private const val MAX_PHOTO_COUNT = 10
        private const val MAX_PAGE_SIZE = 100
    }

    /**
     * admin 목록 조회 — 7종 필터 + 페이징 (Spec #830 P1-B §2.4).
     *
     * 기간 미지정 시 최근 30일. soft-delete 자동 제외.
     */
    fun search(
        startDate: LocalDate?,
        endDate: LocalDate?,
        filter: AdminSuggestionFilterParams,
        page: Int,
        size: Int
    ): AdminSuggestionListResponse {
        val effectiveStart = startDate ?: LocalDate.now().minusDays(30)
        val effectiveEnd = endDate ?: LocalDate.now()
        if (effectiveEnd.isBefore(effectiveStart)) {
            throw IllegalArgumentException("종료일이 시작일보다 빠를 수 없습니다")
        }

        val pageable = PageRequest.of(page.coerceAtLeast(0), size.coerceIn(1, MAX_PAGE_SIZE))
        val filterDto = AdminSuggestionFilter(
            startDateTime = effectiveStart.atStartOfDay(),
            endDateTime = effectiveEnd.atTime(LocalTime.MAX),
            category = filter.category,
            employeeName = filter.employeeName,
            accountCode = filter.accountCode,
            actionStatus = filter.actionStatus,
            productCode = filter.productCode
        )
        val result = suggestionRepository.searchForAdmin(filterDto, pageable)
        return AdminSuggestionListResponse(
            content = result.content.map { AdminSuggestionListItem.from(it) },
            page = result.number,
            size = result.size,
            totalElements = result.totalElements,
            totalPages = result.totalPages
        )
    }

    /**
     * admin 단건 상세 조회 (Spec #830 P1-B §3.2).
     */
    fun getDetail(id: Long): AdminSuggestionDetailResponse {
        if (id <= 0) throw InvalidSuggestionIdException()
        val suggestion = suggestionRepository.findByIdAndIsDeletedFalse(id)
            ?: throw SuggestionNotFoundException()

        val attachments = uploadFileRepository
            .findByParentTypeAndParentIdAndIsDeletedFalse(UploadFileParentTypes.SUGGESTION, suggestion.id)
            .filter { !it.uniqueKey.isNullOrBlank() }
            .sortedBy { it.createdAt }
            .mapIndexed { index, file ->
                SuggestionAttachment(
                    id = file.id,
                    s3Url = suggestionService.composeS3Url(file.uniqueKey!!),
                    fileName = file.name,
                    sortOrder = index
                )
            }
        return AdminSuggestionDetailResponse.from(suggestion, attachments)
    }

    /**
     * admin 등록 (Spec #830 P1-B §3.3).
     *
     * `request.employeeId` null 이면 admin 본인이 작성자. BR1~BR7 + 자동 채움 5종은 mobile 과 동일.
     */
    @Transactional
    fun create(
        adminEmployeeId: Long,
        request: AdminSuggestionCreateRequest,
        photos: List<MultipartFile>?
    ): SuggestionCreateResponse {
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

        val targetEmployeeId = request.employeeId ?: adminEmployeeId
        val employee = employeeRepository.findById(targetEmployeeId)
            .orElseThrow { IllegalArgumentException("사원을 찾을 수 없습니다") }

        val product: Product? = request.productCode
            ?.takeIf { it.isNotBlank() }
            ?.let { productRepository.findByProductCode(it) }

        val account = request.accountId?.let {
            accountRepository.findById(it.toInt()).orElse(null)
        }

        val (receptionCenter, responsibleCenter) = suggestionService.computeWerkCenters(account = account, product = product)
        val orgCostCenterCode = employee.costCenterCode
            ?.takeIf { it.isNotBlank() }
            ?.let { orgCostCenterMatchService.findMatchingCostCenterCode(it).orElse(null) }

        val proposalNumber = suggestionService.generateProposalNumber(LocalDate.now())

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
        val saved = suggestionRepository.save(suggestion)

        val attachments = photos?.mapIndexedNotNull { index, file ->
            if (file.isEmpty) return@mapIndexedNotNull null
            val key = fileStorageService.uploadSuggestionPhoto(file, saved.id)
            val uploadFile = UploadFile(
                name = file.originalFilename,
                uniqueKey = key,
                fileSize = suggestionService.formatFileSize(file.size),
                parentType = UploadFileParentTypes.SUGGESTION,
                parentId = saved.id,
                isDeleted = false
            )
            val savedFile = uploadFileRepository.save(uploadFile)
            SuggestionAttachment(
                id = savedFile.id,
                s3Url = suggestionService.composeS3Url(key),
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

    /**
     * admin 수정 (Spec #830 P1-B §3.4).
     *
     * 본인 row 검증 미실시. 자동 채움 미실행 (수정 시 정합). actionStatus / duplicateProposalNum 도 수정 가능.
     */
    @Transactional
    fun update(id: Long, request: AdminSuggestionUpdateRequest): AdminSuggestionDetailResponse {
        if (id <= 0) throw InvalidSuggestionIdException()
        val suggestion = suggestionRepository.findByIdAndIsDeletedFalse(id)
            ?: throw SuggestionNotFoundException()

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

        suggestionRepository.save(suggestion)
        return getDetail(id)
    }

    /**
     * admin soft delete (Spec #830 P1-B §3.5).
     *
     * 본인 row 검증 미실시. UploadFile cascade 미실행 (별 책임 — 본 스펙 비범위).
     */
    @Transactional
    fun softDelete(id: Long) {
        if (id <= 0) throw InvalidSuggestionIdException()
        val suggestion = suggestionRepository.findByIdAndIsDeletedFalse(id)
            ?: throw SuggestionNotFoundException()
        suggestion.isDeleted = true
        suggestionRepository.save(suggestion)
    }

    /**
     * admin 이미지 추가 업로드 (Spec #830 P1-B §2.5).
     *
     * 기존 첨부 + 신규 photos 의 합이 [MAX_PHOTO_COUNT] 초과 시 400. soft-deleted 메타는 카운팅 대상에서 제외.
     * 응답은 신규 추가된 사진만.
     */
    @Transactional
    fun uploadPhotos(id: Long, photos: List<MultipartFile>): List<SuggestionAttachment> {
        if (id <= 0) throw InvalidSuggestionIdException()
        if (photos.isEmpty()) throw IllegalArgumentException("업로드할 사진이 없습니다")

        val suggestion = suggestionRepository.findByIdAndIsDeletedFalse(id)
            ?: throw SuggestionNotFoundException()

        val existing = uploadFileRepository
            .findByParentTypeAndParentIdAndIsDeletedFalse(UploadFileParentTypes.SUGGESTION, suggestion.id)
        if (existing.size + photos.size > MAX_PHOTO_COUNT) {
            throw IllegalArgumentException("첨부 파일은 최대 ${MAX_PHOTO_COUNT}건 입니다")
        }

        val baseIndex = existing.size
        return photos.mapIndexedNotNull { index, file ->
            if (file.isEmpty) return@mapIndexedNotNull null
            val key = fileStorageService.uploadSuggestionPhoto(file, suggestion.id)
            val uploadFile = UploadFile(
                name = file.originalFilename,
                uniqueKey = key,
                fileSize = suggestionService.formatFileSize(file.size),
                parentType = UploadFileParentTypes.SUGGESTION,
                parentId = suggestion.id,
                isDeleted = false
            )
            val saved = uploadFileRepository.save(uploadFile)
            SuggestionAttachment(
                id = saved.id,
                s3Url = suggestionService.composeS3Url(key),
                fileName = file.originalFilename,
                sortOrder = baseIndex + index
            )
        }
    }

    /**
     * admin 단건 사진 삭제 (Spec #830 P1-B — Q-add admin endpoint).
     *
     * 본인 row 검증 미실시 (admin 우회). S3 선행 → DB soft delete (mobile [SuggestionService.deletePhoto] 정합).
     */
    @Transactional
    fun deletePhoto(suggestionId: Long, photoId: Long) {
        if (suggestionId <= 0) throw InvalidSuggestionIdException()
        if (photoId <= 0) throw InvalidSuggestionPhotoIdException()

        val suggestion = suggestionRepository.findByIdAndIsDeletedFalse(suggestionId)
            ?: throw SuggestionNotFoundException()

        val uploadFile = uploadFileRepository
            .findByIdAndParentTypeAndParentIdAndIsDeletedFalse(photoId, UploadFileParentTypes.SUGGESTION, suggestion.id)
            ?: throw SuggestionPhotoNotFoundException()

        uploadFile.uniqueKey?.takeIf { it.isNotBlank() }?.let {
            fileStorageService.deleteSuggestionPhoto(it)
        }
        uploadFile.isDeleted = true
        uploadFileRepository.save(uploadFile)
    }
}

/**
 * admin 목록 검색의 컨트롤러 입력 파라미터 묶음 (date range 제외 — service 에서 default 처리).
 */
data class AdminSuggestionFilterParams(
    val category: com.otoki.powersales.suggestion.entity.SuggestionCategory? = null,
    val employeeName: String? = null,
    val accountCode: String? = null,
    val actionStatus: com.otoki.powersales.suggestion.entity.SuggestionActionStatus? = null,
    val productCode: String? = null
)
