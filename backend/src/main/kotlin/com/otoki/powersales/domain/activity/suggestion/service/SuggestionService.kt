package com.otoki.powersales.domain.activity.suggestion.service

import com.otoki.powersales.domain.activity.suggestion.dto.request.SuggestionCreateRequest
import com.otoki.powersales.domain.activity.suggestion.dto.request.SuggestionUpdateRequest
import com.otoki.powersales.domain.activity.suggestion.dto.response.SuggestionAttachment
import com.otoki.powersales.domain.activity.suggestion.dto.response.SuggestionCreateResponse
import com.otoki.powersales.domain.activity.suggestion.dto.response.SuggestionListItem
import com.otoki.powersales.domain.activity.suggestion.dto.response.SuggestionResponse
import com.otoki.powersales.domain.activity.suggestion.entity.Suggestion
import com.otoki.powersales.domain.activity.suggestion.entity.SuggestionCategory
import com.otoki.powersales.domain.activity.suggestion.entity.SuggestionStatus
import com.otoki.powersales.domain.activity.suggestion.exception.InvalidSuggestionIdException
import com.otoki.powersales.domain.activity.suggestion.exception.InvalidSuggestionPhotoIdException
import com.otoki.powersales.domain.activity.suggestion.exception.SuggestionAccessDeniedException
import com.otoki.powersales.domain.activity.suggestion.exception.SuggestionNotFoundException
import com.otoki.powersales.domain.activity.suggestion.exception.SuggestionPhotoNotFoundException
import com.otoki.powersales.domain.activity.suggestion.repository.SuggestionDraftRepository
import com.otoki.powersales.domain.activity.suggestion.repository.SuggestionRepository
import com.otoki.powersales.domain.foundation.account.repository.AccountRepository
import com.otoki.powersales.platform.common.entity.UploadFile
import com.otoki.powersales.platform.common.repository.UploadFileRepository
import com.otoki.powersales.platform.common.service.FileStorageService
import com.otoki.powersales.platform.common.storage.StorageConstants
import com.otoki.powersales.platform.common.storage.StorageService
import com.otoki.powersales.platform.common.storage.UploadFileParentTypes
import com.otoki.powersales.domain.foundation.account.entity.Account
import com.otoki.powersales.domain.org.employee.repository.EmployeeRepository
import com.otoki.powersales.domain.org.organization.service.OrgCostCenterMatchService
import com.otoki.powersales.platform.auth.entity.AppAuthority
import com.otoki.powersales.domain.foundation.product.entity.Product
import com.otoki.powersales.domain.foundation.product.enums.StorageCondition
import com.otoki.powersales.domain.foundation.product.repository.ProductRepository
import com.otoki.powersales.domain.activity.suggestion.entity.SuggestionSfSendStatus
import com.otoki.powersales.external.sf.outbound.SfApiResponse
import com.otoki.powersales.external.sf.outbound.SfOAuthFailedException
import com.otoki.powersales.external.sf.outbound.SfOutboundClient
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.support.TransactionTemplate
import org.springframework.web.multipart.MultipartFile
import java.time.LocalDate
import java.time.LocalDateTime
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
    private val suggestionDraftRepository: SuggestionDraftRepository,
    private val uploadFileRepository: UploadFileRepository,
    private val accountRepository: AccountRepository,
    private val employeeRepository: EmployeeRepository,
    private val productRepository: ProductRepository,
    private val orgCostCenterMatchService: OrgCostCenterMatchService,
    private val fileStorageService: FileStorageService,
    private val validator: SuggestionValidator,
    private val storageService: StorageService,
    private val sfOutboundClient: SfOutboundClient,
    private val txTemplate: TransactionTemplate,
) {

    private val log = LoggerFactory.getLogger(javaClass)

    companion object {
        private const val MAX_PHOTO_COUNT = 10
        private val PROPOSAL_NUMBER_DATE_FMT: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMdd")

        /**
         * SF Apex REST `IF_REST_MOBILE_ProposalRegist` endpoint suffix.
         * `sf.outbound.apex-base-url` prefix 뒤에 붙는다 (클레임 등록 `/ClaimRegist` 와 동일 컨벤션).
         */
        private const val SF_PROPOSAL_REGIST_ENDPOINT = "/ProposalRegist"

        /** SF Apex `Date.valueOf(String)` 는 ISO(yyyy-MM-dd) 만 파싱 (클레임 등록 정합). */
        private val SF_DATE_FMT: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE
    }

    /**
     * 제안/물류클레임 등록 — dual-write (DB INSERT + SF Apex `IF_REST_MOBILE_ProposalRegist` 직접 호출).
     *
     * 클레임 등록(`AdminClaimCreateService`) 과 동일 패턴:
     *  1. 검증 + 의존 entity 조회 + 매핑(트리거 부수효과 이식)
     *  2. [Tx1] suggestion + 첨부 INSERT (sf_send_status=PENDING) + 임시저장 삭제
     *  3. [SF call, 트랜잭션 외부] apiMap 빌드 + SfOutboundClient.callApi("/ProposalRegist", apiMap)
     *  4. [Tx2] 전송상태 update (성공 → SENT + sf_sent_at, 실패 → SEND_FAILED + sf_send_fail_message)
     *
     * SF 호출 실패는 catch 하여 SEND_FAILED 로 기록하고 등록은 성공(201)으로 응답한다 — 사용자 등록을
     * SF 장애로 막지 않는다(클레임 등록 정책 동일). 실패 row 는 sf_send_status=SEND_FAILED 로 남아 추적된다.
     *
     * 클래스 기본 `@Transactional(readOnly=true)` 가 외곽을 감싸지 않도록 NOT_SUPPORTED 로 두고,
     * write 트랜잭션 2건은 [txTemplate] 로 명시 분리한다(SF HTTP 호출 구간을 DB 트랜잭션 밖으로 뺀다).
     */
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
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
            accountRepository.findById(it).orElse(null)
        }

        // step 4 (mapping — 5종 trigger 부수효과 이식 + R17 WERK)
        val (receptionCenter, responsibleCenter) = computeWerkCenters(account = account, product = product)
        val orgCostCenterCode = employee.costCenterCode
            ?.takeIf { it.isNotBlank() }
            ?.let { orgCostCenterMatchService.findMatchingCostCenterCode(it).orElse(null) }

        val proposalNumber = generateProposalNumber(LocalDate.now())

        // step 5 — [Tx1] INSERT + 첨부 N건 (sf_send_status=PENDING) + 임시저장 삭제
        val inserted = txTemplate.execute {
            val suggestion = Suggestion(
                proposalNumber = proposalNumber,
                title = request.title ?: throw IllegalArgumentException("제목은 필수입니다"),
                content = request.content ?: throw IllegalArgumentException("내용은 필수입니다"),
                category = category,
                category1 = product?.productCategory1,
                category2 = product?.productCategory2,
                category3 = product?.productCategory3,
                sapAccountCode = request.sapAccountCode,
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
                sfSendStatus = SuggestionSfSendStatus.PENDING,
                account = account,
                employee = employee,
                product = product,
            )
            val saved = suggestionRepository.save(suggestion)

            val photoMetas = mutableListOf<SfPhotoMeta>()
            val attachments = photos?.mapIndexedNotNull { index, file ->
                if (file.isEmpty) return@mapIndexedNotNull null
                val key = fileStorageService.uploadSuggestionPhoto(file, saved.id)
                val uploadFile = UploadFile(
                    name = file.originalFilename,
                    uniqueKey = key,
                    fileSize = formatFileSize(file.size),
                    parentType = UploadFileParentTypes.SUGGESTION,
                    parentId = saved.id,
                    isDeleted = false
                )
                val savedFile = uploadFileRepository.save(uploadFile)
                photoMetas += SfPhotoMeta(uniqueKey = key, fileSize = file.size, fileName = file.originalFilename)
                SuggestionAttachment(
                    id = savedFile.id,
                    s3Url = composeS3Url(key),
                    fileName = file.originalFilename,
                    sortOrder = index
                )
            } ?: emptyList()

            // 레거시 정합: 정식 등록 성공 시 해당 사원의 임시저장 row 삭제.
            suggestionDraftRepository.findByEmployeeId(employeeId)?.let { suggestionDraftRepository.delete(it) }

            InsertResult(
                id = saved.id,
                proposalNumber = saved.proposalNumber,
                attachments = attachments,
                photoMetas = photoMetas
            )
        }!!

        // step 6 — [SF call, 트랜잭션 외부]
        val sfResult = invokeSf(
            buildSfApiMap(
                pwrskey = inserted.id,
                category = category,
                request = request,
                employeeCode = employee.employeeCode,
                photoMetas = inserted.photoMetas
            )
        )

        // step 7 — [Tx2] 전송상태 update
        txTemplate.execute {
            val persisted = suggestionRepository.findByIdAndIsDeletedFalse(inserted.id)
                ?: throw SuggestionNotFoundException()
            applySfResult(persisted, sfResult)
        }

        return SuggestionCreateResponse(
            id = inserted.id,
            proposalNumber = inserted.proposalNumber,
            attachments = inserted.attachments
        )
    }

    /**
     * SF push — 실패해도 예외를 throw 하지 않고 [SfPushResult] 로 반환(클레임 등록 정합).
     */
    internal fun invokeSf(apiMap: Map<String, Any?>): SfPushResult =
        try {
            val response = sfOutboundClient.callApi(SF_PROPOSAL_REGIST_ENDPOINT, apiMap)
            SfPushResult(success = response.isSuccess(), apiResponse = response, errorSummary = null)
        } catch (e: SfOAuthFailedException) {
            log.warn("[suggestion-create] SF OAuth 실패: {}", e.message)
            SfPushResult(success = false, apiResponse = null, errorSummary = e.message ?: "SF OAuth 실패")
        } catch (e: Exception) {
            log.warn("[suggestion-create] SF 호출 예외: {}", e.message)
            SfPushResult(success = false, apiResponse = null, errorSummary = e.message ?: e.javaClass.simpleName)
        }

    internal fun applySfResult(suggestion: Suggestion, result: SfPushResult) {
        suggestion.sfSendAttemptCount += 1
        if (result.success) {
            suggestion.sfSendStatus = SuggestionSfSendStatus.SENT
            suggestion.sfSentAt = LocalDateTime.now()
            suggestion.sfSendFailMessage = null
        } else {
            suggestion.sfSendStatus = SuggestionSfSendStatus.SEND_FAILED
            suggestion.sfSendFailMessage = (result.apiResponse?.resultMsg ?: result.errorSummary)?.take(1000)
        }
    }

    /**
     * 레거시 Heroku `FieldTalkController.suggestProc`(신규 등록 분기 hndSp="I", line 1726-1752) 가
     * SF `.../apexrest/mobile/ProposalRegist` 로 보내던 JSON payload 를 그대로 재현한다.
     *
     * 운영 검증된 key 셋만 전송한다 — 레거시 제안 등록은:
     *  - 거래처 key 는 **소문자 `accountCode`** 한 가지만 사용한다(대문자 `SAPAccountCode` 는
     *    클레임/현장점검 전용이며 제안 payload 엔 없음). 값은 동일하게 SAP 거래처코드.
     *  - `Type` 은 전송하지 않는다.
     * 미입력(빈) 값은 key 자체를 생략해 레거시 null 가드(`logclaimDate`/`accountCode`) 동작과 맞춘다.
     *
     * 이미지는 레거시가 S3 사전 업로드 후 식별 정보(UniqueKey/FileName/FileSize)만 전송하는 방식이라
     * (클레임의 Base64 buffer 와 상반), [Tx1] 에서 확보한 uniqueKey/파일명/크기를 1·2번 슬롯(최대 2장)에
     * 채운다. SF `IF_REST_MOBILE_ProposalRegist.cls` 가 동일 key 로 `UploadFile__c` insert.
     *
     * [pwrskey] — 레거시엔 없던 필드로, 이번에 SF 와 신규 협의해 추가했다. 해당 물류클레임(=제안)
     * 레코드의 PowerSales primary key(`suggestion_id`)를 전송해 SF 가 등록 SF 레코드와 PowerSales
     * row 를 역연결(back-link)하도록 한다.
     * 배포 의존성: SF Apex 는 `JSON.deserializeStrict` 로 파싱하므로 SF `Input` 클래스의
     * `pwrskey` 필드 반영과 동반 배포돼야 한다(SF 미반영 상태로 단독 배포 시 strict 파싱 실패 → 전 건 SEND_FAILED).
     */
    internal fun buildSfApiMap(
        pwrskey: Long,
        category: SuggestionCategory,
        request: SuggestionCreateRequest,
        employeeCode: String?,
        photoMetas: List<SfPhotoMeta>
    ): Map<String, Any?> = buildMap {
        put("pwrskey", pwrskey.toString())
        put("Category", category.displayName)
        put("ProductCode", request.productCode?.trim())
        put("Title", request.title?.trim())
        put("Description", request.content?.trim())
        put("CarNumber", request.carNumber?.trim()?.takeIf { it.isNotEmpty() })
        put("claimList", request.claimType?.trim())
        request.claimDate?.let { put("logclaimDate", it.format(SF_DATE_FMT)) }
        put("EmployeeCode", employeeCode)
        request.sapAccountCode?.trim()?.takeIf { it.isNotEmpty() }?.let { put("accountCode", it) }

        photoMetas.getOrNull(0)?.let {
            put("S3ImageUniqueKey1", it.uniqueKey)
            put("S3ImageFileName1", it.fileName)
            put("S3ImageFileSize1", it.fileSize.toString())
        }
        photoMetas.getOrNull(1)?.let {
            put("S3ImageUniqueKey2", it.uniqueKey)
            put("S3ImageFileName2", it.fileName)
            put("S3ImageFileSize2", it.fileSize.toString())
        }
    }

    /** SF 전송용 첨부 메타 — [Tx1] S3 업로드 결과(클레임의 Base64 와 달리 ProposalRegist 는 키 전송). */
    internal data class SfPhotoMeta(
        val uniqueKey: String,
        val fileSize: Long,
        val fileName: String?,
    )

    /** [Tx1] 결과 — Tx 밖으로 넘길 식별자·응답 첨부·SF 전송용 메타. */
    internal data class InsertResult(
        val id: Long,
        val proposalNumber: String,
        val attachments: List<SuggestionAttachment>,
        val photoMetas: List<SfPhotoMeta>,
    )

    /** SF push 결과 — 성공/실패 + 응답 또는 오류 요약 (클레임 등록 정합). */
    internal data class SfPushResult(
        val success: Boolean,
        val apiResponse: SfApiResponse?,
        val errorSummary: String?,
    )

    fun getDetail(suggestionId: Long, requesterEmployeeId: Long): SuggestionResponse {
        if (suggestionId <= 0) throw InvalidSuggestionIdException()
        val suggestion = suggestionRepository.findByIdAndIsDeletedFalse(suggestionId)
            ?: throw SuggestionNotFoundException()

        val requester = employeeRepository.findById(requesterEmployeeId).orElse(null)
            ?: throw SuggestionAccessDeniedException()

        // 접근 권한: 본인 OR (물류클레임 & 여사원 아님 & 같은 원가센터) — 목록 가시 범위와 일치(레거시 LogisticsClaimSearch 동등).
        // 정규 제안(그 외 분류)은 본인만 허용(기존 정책 유지).
        val isOwner = suggestion.employee?.id == requesterEmployeeId
        val requesterOrgCostCenter = resolveOrgCostCenterCode(requester.costCenterCode)
        val isSameCostCenterLogisticsClaim = suggestion.category == SuggestionCategory.LOGISTICS_CLAIM &&
            requester.role != AppAuthority.WOMAN &&
            requesterOrgCostCenter != null &&
            requesterOrgCostCenter == suggestion.orgCostCenterCode
        if (!isOwner && !isSameCostCenterLogisticsClaim) {
            throw SuggestionAccessDeniedException()
        }

        // '오뚜기 접수사원'(등록사원 이름/사번) 은 물류클레임 상세에서 조장에게만 노출(레거시 logisticsclaimview 동등).
        val showReceptionEmployee = suggestion.category == SuggestionCategory.LOGISTICS_CLAIM &&
            requester.role == AppAuthority.LEADER

        val attachments = uploadFileRepository
            .findByParentTypeAndParentIdAndIsDeletedFalse(UploadFileParentTypes.SUGGESTION, suggestion.id)
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

        return SuggestionResponse.from(suggestion, attachments, showReceptionEmployee)
    }

    /**
     * 본인/원가센터 제안·물류클레임 목록 — 레거시 `LogisticsClaimSearch` 동등.
     *
     * 권한 분기: 여사원=본인분 / 그 외(조장·지점장)=같은 원가센터 전체(물류클레임 한정, 정규 제안은 본인분 유지).
     * 선택 필터: [accountId] 거래처(레거시 SAPAccountCode), [startDate]~[endDate] 등록일 범위
     * (레거시는 `CreatedDate` 기준 + 종료일 익일 미만 경계이므로 `endDate+1일 00:00` 미만으로 조회).
     */
    fun listMine(
        employeeId: Long,
        page: Int,
        size: Int,
        category: SuggestionCategory? = null,
        accountId: Long? = null,
        startDate: LocalDate? = null,
        endDate: LocalDate? = null
    ): Page<SuggestionListItem> {
        val pageable: Pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))
        val requester = employeeRepository.findById(employeeId).orElse(null)
            ?: throw SuggestionAccessDeniedException()

        // 레거시 LogisticsClaimSearch 권한 분기: 여사원=본인분 / 그 외(조장·지점장)=같은 원가센터 전체.
        // 물류클레임 분류에 한해 원가센터 전체조회 적용(정규 제안은 본인분 유지).
        val requesterOrgCostCenter = resolveOrgCostCenterCode(requester.costCenterCode)
        val useCostCenterScope = category == SuggestionCategory.LOGISTICS_CLAIM &&
            requester.role != AppAuthority.WOMAN &&
            requesterOrgCostCenter != null
        val scopeOrgCostCenterCode = if (useCostCenterScope) requesterOrgCostCenter else null

        // 기간 필터는 CreatedDate(등록일) 기준 — 종료일 당일 전체 포함(익일 00:00 미만).
        val createdFrom = startDate?.atStartOfDay()
        val createdToExclusive = endDate?.plusDays(1)?.atStartOfDay()

        val result = suggestionRepository.searchMine(
            employeeId = employeeId,
            scopeOrgCostCenterCode = scopeOrgCostCenterCode,
            category = category,
            accountId = accountId,
            createdFrom = createdFrom,
            createdToExclusive = createdToExclusive,
            pageable = pageable
        )
        return result.map { SuggestionListItem.from(it) }
    }

    /** 사원 costCenterCode → 등록 시 저장된 `org_cost_center_code` 와 동일 정규화 (없으면 null). */
    private fun resolveOrgCostCenterCode(costCenterCode: String?): String? =
        costCenterCode
            ?.takeIf { it.isNotBlank() }
            ?.let { orgCostCenterMatchService.findMatchingCostCenterCode(it).orElse(null) }

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
     * 제안 첨부 사진 단건 삭제 (Spec #828, UC-06).
     *
     * 본인 row 검증 후 S3 객체 선행 삭제 → UploadFile 메타 soft delete (isDeleted=true).
     * 상태 무관 삭제 허용 (레거시 SF S3FileUpload 정합 — Q1). 레거시 UUID 형식 키는 S3 측 no-op.
     */
    @Transactional
    fun deletePhoto(employeeId: Long, suggestionId: Long, photoId: Long) {
        if (suggestionId <= 0) throw InvalidSuggestionIdException()
        if (photoId <= 0) throw InvalidSuggestionPhotoIdException()

        val suggestion = suggestionRepository.findByIdAndIsDeletedFalse(suggestionId)
            ?: throw SuggestionNotFoundException()
        if (suggestion.employee?.id != employeeId) {
            throw SuggestionAccessDeniedException()
        }

        val uploadFile = uploadFileRepository
            .findByIdAndParentTypeAndParentIdAndIsDeletedFalse(photoId, UploadFileParentTypes.SUGGESTION, suggestion.id)
            ?: throw SuggestionPhotoNotFoundException()

        uploadFile.uniqueKey?.takeIf { it.isNotBlank() }?.let {
            fileStorageService.deleteSuggestionPhoto(it)
        }

        uploadFile.isDeleted = true
        uploadFileRepository.save(uploadFile)
    }

    /**
     * `proposal_number` 채번 — Spec #664 Q12 채택 (DB Sequence + 패딩, race condition free).
     *
     * 형식: `S-YYYYMMDD-NNNNNN` (NNNNNN = `suggestion_proposal_number_seq` nextval, 6자리 zero-padded).
     * PostgreSQL `nextval()` 원자성 보장.
     *
     * Spec #830 P1-B §2.3 — `AdminSuggestionService` 가 동일 채번 정책 재사용.
     */
    internal fun generateProposalNumber(date: LocalDate): String {
        val seq = suggestionRepository.nextProposalNumberSeqValue()
        return "S-${date.format(PROPOSAL_NUMBER_DATE_FMT)}-${"%06d".format(seq)}"
    }

    /**
     * R17 WERK 분기 — Q3 옵션 B (레거시 동등 재현). `ProposalTriggerHandler.cls:89-95`.
     *
     * 레거시 분기 조건 문자열 `'냉동/냉장'` 은 SF picklist 실측 값 `실온` / `냉장` 과 매치 안 되어 실제 운영
     * row 는 거의 else 분기로 빠짐 (R17 bug 정황). 그러나 명시적 분기 코드는 레거시 동등하게 작성.
     *
     * Spec #830 P1-B §2.3 — `AdminSuggestionService` 가 동일 분기 로직 재사용.
     */
    internal fun computeWerkCenters(
        account: Account?,
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

    /**
     * 제안(물류 클레임) 첨부 이미지는 private/ 저장 → presigned URL 로만 조회 가능 (인증 기반 접근).
     * 제품 클레임(Claim)과 동일하게 실 객체 key = private/ + uniqueKey 로 합성된다.
     */
    internal fun composeS3Url(key: String): String =
        storageService.getPresignedUrl(key, StorageConstants.CLAIM_PRESIGN_TTL_SECONDS)

    internal fun formatFileSize(bytes: Long): String =
        when {
            bytes >= 1024 * 1024 -> "%.1fMB".format(bytes / (1024.0 * 1024.0))
            bytes >= 1024 -> "%.1fKB".format(bytes / 1024.0)
            else -> "${bytes}B"
        }
}
