package com.otoki.powersales.notice.service

import com.otoki.powersales.auth.exception.EmployeeNotFoundException
import com.otoki.powersales.common.entity.UploadFile
import com.otoki.powersales.common.repository.UploadFileRepository
import com.otoki.powersales.common.service.FileStorageService
import com.otoki.powersales.common.storage.StorageConstants
import com.otoki.powersales.common.storage.StorageService
import com.otoki.powersales.common.storage.UploadFileParentTypes
import com.otoki.powersales.employee.repository.EmployeeRepository
import com.otoki.powersales.notice.dto.request.NoticeCreateRequest
import com.otoki.powersales.notice.dto.request.NoticeUpdateRequest
import com.otoki.powersales.notice.dto.response.NoticeImageResponse
import com.otoki.powersales.notice.dto.response.BranchOption
import com.otoki.powersales.notice.dto.response.CategoryOption
import com.otoki.powersales.notice.dto.response.ScopeOption
import com.otoki.powersales.notice.dto.response.NoticeFormMetaResponse
import com.otoki.powersales.notice.dto.response.NoticeMutationResponse
import com.otoki.powersales.notice.dto.response.NoticePostDetailResponse
import com.otoki.powersales.notice.dto.response.NoticePostListResponse
import com.otoki.powersales.notice.dto.response.NoticePostSummaryResponse
import com.otoki.powersales.notice.entity.Notice
import com.otoki.powersales.notice.enums.NoticeCategory
import com.otoki.powersales.notice.enums.NoticeScope
import com.otoki.powersales.notice.exception.BranchRequiredException
import com.otoki.powersales.notice.exception.InvalidImageIdException
import com.otoki.powersales.notice.exception.InvalidNoticeCategoryException
import com.otoki.powersales.notice.exception.InvalidNoticeIdException
import com.otoki.powersales.notice.exception.InvalidNoticeScopeException
import com.otoki.powersales.notice.exception.NoticePostNotFoundException
import com.otoki.powersales.notice.repository.NoticeRepository
import com.otoki.powersales.organization.repository.OrganizationRepository
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile

@Service
@Transactional(readOnly = true)
class NoticeService(
    private val noticeRepository: NoticeRepository,
    private val uploadFileRepository: UploadFileRepository,
    private val employeeRepository: EmployeeRepository,
    private val organizationRepository: OrganizationRepository,
    private val fileStorageService: FileStorageService,
    private val storageService: StorageService
) {

    companion object {
        // data-refid 속성을 가진 placeholder <img> 태그 전체. group(1) = refid.
        private val INLINE_IMG_REGEX =
            Regex("""<img\b[^>]*\bdata-refid\s*=\s*"([^"]+)"[^>]*>""", RegexOption.IGNORE_CASE)
        // <img> 태그 안의 src 속성 (presigned 로 교체 대상).
        private val SRC_ATTR_REGEX =
            Regex("""\bsrc\s*=\s*"[^"]*"""", RegexOption.IGNORE_CASE)
    }

    /**
     * 공지 상세 조회 (web admin + mobile 공통).
     *
     * 공지 이미지는 본문 인라인(rich text rtaImage 마이그레이션분)과 첨부(images[]) 두 종류 모두 private S3 에
     * 저장되어 presigned URL 로만 조회 가능하다 (Spec #854 재설계). upload_file 을 1회 조회해 두 용도로 재사용한다:
     * 1. 본문 content 의 placeholder `<img data-refid="{refid}">` 를 presigned URL 로 rewrite ([rewriteInlineImages]).
     *    매핑 키는 refid = upload_file.sfid (Stage1 이 build CSV 의 Id=refid 를 sfid 로 적재).
     * 2. 첨부 images[] 의 url 을 presigned 로 발급.
     * presigned 는 만료(NOTICE_PRESIGN_TTL_SECONDS)되므로 본문 DB 에는 placeholder 만 영구 저장하고 조회 시점에 발급한다.
     */
    fun getNoticeDetail(noticeId: Long): NoticePostDetailResponse {
        val notice = noticeRepository.findById(noticeId)
            .filter { it.isDeleted != true }
            .orElseThrow { NoticePostNotFoundException() }

        val uploadFiles = uploadFileRepository
            .findByParentTypeAndParentIdAndIsDeletedFalse(UploadFileParentTypes.NOTICE, notice.id)
            .filter { !it.uniqueKey.isNullOrBlank() }

        // 본문 rewrite 용 refid(=sfid) → uniqueKey 맵 (1회 조회 재사용, N+1 없음).
        val uniqueKeyByRefid: Map<String, String> = uploadFiles
            .filter { !it.sfid.isNullOrBlank() }
            .associate { it.sfid!! to it.uniqueKey!! }

        val content = rewriteInlineImages(notice.contents ?: "", uniqueKeyByRefid)

        val images = uploadFiles
            .sortedBy { it.createdAt }
            .mapIndexed { index, file ->
                NoticeImageResponse(
                    id = file.id,
                    url = storageService.getPresignedUrl(file.uniqueKey!!, StorageConstants.NOTICE_PRESIGN_TTL_SECONDS),
                    sortOrder = index
                )
            }

        return NoticePostDetailResponse(
            id = notice.id,
            scope = notice.scope?.displayName,
            category = notice.category?.apiCode ?: "",
            categoryName = notice.category?.displayName ?: "",
            title = notice.name ?: "",
            content = content,
            branch = notice.branch,
            branchCode = notice.branchCode,
            createdAt = notice.createdAt,
            images = images
        )
    }

    /**
     * 공지 본문 HTML 의 placeholder 인라인 이미지(`<img ... data-refid="{refid}" ...>`)의 src 를 presigned URL 로 치환.
     *
     * 마이그레이션이 본문에 `<img src="notice-image://{refid}" data-refid="{refid}">` 형태로 영구 저장하고,
     * 조회 시점에 본 함수가 refid → uniqueKey → presigned 로 src 만 교체한다 (data-refid 는 mobile cacheKey 용으로 보존).
     * 미적재 refid(다운로드 실패분 등)는 placeholder 유지 → 클라이언트에서 깨진 이미지로 노출되되 본문 오염 없음.
     * data-refid 미포함 본문(이미지 없는 공지)은 즉시 반환.
     */
    private fun rewriteInlineImages(html: String, uniqueKeyByRefid: Map<String, String>): String {
        if (!html.contains("data-refid")) return html
        return INLINE_IMG_REGEX.replace(html) { match ->
            val refid = match.groupValues[1]
            val uniqueKey = uniqueKeyByRefid[refid] ?: return@replace match.value
            val presigned = storageService.getPresignedUrl(uniqueKey, StorageConstants.NOTICE_PRESIGN_TTL_SECONDS)
            // src 속성만 presigned 로 교체 (data-refid 보존). presigned URL 은 &, =, %, $ 등을 포함하므로
            // 람다 기반 replace 로 치환 문자열을 literal 로 넣어 그룹 참조($1 등) 오해석을 방지한다.
            SRC_ATTR_REGEX.replace(match.value) { "src=\"$presigned\"" }
        }
    }

    fun getPosts(userId: Long, category: String?, search: String?, page: Int, size: Int): NoticePostListResponse {
        val employee = employeeRepository.findById(userId)
            .orElseThrow { EmployeeNotFoundException() }
        // 레거시는 지점공지를 지점코드(costcentercode__c)로 필터한다. 지점명/조직명 포맷 불일치로
        // 지점공지가 누락되던 문제를 코드 기반 매칭으로 해소. (communityMapper.xml#selectNotice 참조)
        val branchCode = employee.costCenterCode ?: ""

        val parsedCategory = if (category != null) parseCategory(category) else null

        val truncatedSearch = search?.take(100)?.ifBlank { null }
        val pageable = PageRequest.of(page - 1, size)
        val noticePage = noticeRepository.findNotices(parsedCategory, truncatedSearch, branchCode, pageable)

        val content = noticePage.content.map { it.toSummaryResponse() }

        return NoticePostListResponse(
            content = content,
            totalCount = noticePage.totalElements,
            totalPages = noticePage.totalPages,
            currentPage = page,
            size = size
        )
    }

    fun getPostsForAdmin(category: String?, search: String?, page: Int, size: Int): NoticePostListResponse {
        val parsedCategory = if (category != null) parseCategory(category) else null

        val truncatedSearch = search?.take(100)?.ifBlank { null }
        val pageable = PageRequest.of(page - 1, size)
        val noticePage = noticeRepository.findAllNotices(parsedCategory, truncatedSearch, pageable)

        val content = noticePage.content.map { it.toSummaryResponse() }

        return NoticePostListResponse(
            content = content,
            totalCount = noticePage.totalElements,
            totalPages = noticePage.totalPages,
            currentPage = page,
            size = size
        )
    }

    /**
     * Notice 엔티티 → 목록 요약 Response.
     * 컬럼 매핑은 SF `DKRetail__Notice__c` 의 `DKRetail__All` ListView 기준.
     * - department ← employee.orgName (= SF Department__c formula)
     * - authorName ← ownerUser.lastName (= SF OwnerName__c = Owner:User.LastName)
     */
    private fun Notice.toSummaryResponse(): NoticePostSummaryResponse =
        NoticePostSummaryResponse(
            id = id,
            category = category?.apiCode ?: "",
            categoryName = category?.displayName ?: "",
            scope = scope?.displayName,
            title = name ?: "",
            branch = branch,
            department = employee?.orgName,
            authorName = ownerUser?.lastName,
            createdAt = createdAt
        )

    @Transactional
    fun createNotice(request: NoticeCreateRequest, creatorId: Long): NoticeMutationResponse {
        val cat = parseCategory(request.category)
        val noticeScope = parseScope(request.scope)
        validateBranch(cat, request.branch, request.branchCode)

        val notice = Notice(
            name = request.title,
            scope = noticeScope,
            category = cat,
            contents = request.content,
            branch = if (cat == NoticeCategory.BRANCH) request.branch else null,
            branchCode = if (cat == NoticeCategory.BRANCH) request.branchCode else null,
            employee = employeeRepository.getReferenceById(creatorId)
        )
        val saved = noticeRepository.save(notice)
        return NoticeMutationResponse.from(saved)
    }

    @Transactional
    fun updateNotice(noticeId: Long, request: NoticeUpdateRequest): NoticeMutationResponse {
        if (noticeId <= 0) throw InvalidNoticeIdException()
        val notice = findActiveNotice(noticeId)

        val cat = parseCategory(request.category)
        val noticeScope = parseScope(request.scope)
        validateBranch(cat, request.branch, request.branchCode)

        notice.name = request.title
        notice.scope = noticeScope
        notice.category = cat
        notice.contents = request.content
        notice.branch = if (cat == NoticeCategory.BRANCH) request.branch else null
        notice.branchCode = if (cat == NoticeCategory.BRANCH) request.branchCode else null

        val saved = noticeRepository.save(notice)
        return NoticeMutationResponse.from(saved)
    }

    @Transactional
    fun deleteNotice(noticeId: Long) {
        if (noticeId <= 0) throw InvalidNoticeIdException()
        val notice = findActiveNotice(noticeId)
        notice.isDeleted = true
        noticeRepository.save(notice)
    }

    /**
     * 공지사항 첨부 이미지 업로드 — multipart 파일 → S3 PUT + UploadFile 행 INSERT.
     *
     * ## 레거시 매핑
     * - SF Apex: `S3_FileUploadController.cls#sizeCheck` + `#putImg` + `#insertUploadFileRecord`
     * - SF Aura: `S3FileUpload.cmp` + `S3FileUploadHelper.js#putAction`
     * - flow-legacy: `flow-legacy-notice-attachment.yaml` step 6~9
     * - origin spec: #656
     *
     * ## 레거시 동작 요약
     * 1. 입력: parent fileId(ContentDocumentId) + parent recId + parent obj + fileName
     * 2. sizeCheck — ContentVersion.ContentSize > 5,300,000 byte (5.3MB) 시 ContentDocument 즉시 DELETE + return false → toast
     * 3. putImg — ContentVersion.VersionData read → AWS_S3 callout PUT (key=timestamp+한글 인코딩 파일명, acl=public-read, Content-type 분기 PNG/JPG/빈 문자열)
     * 4. insertUploadFileRecord — UploadFile__c{Name, Size__c (B/KB/MB 분기), UniqueKey__c, RecordId__c (텍스트), Object__c, FileId__c=ContentDocumentId} 적재 (Url__c/UploadKbn__c=NULL)
     * 5. @future deleteContentVersionRecord — ContentDocument cascade delete (R8 race 위험)
     *
     * ## 신규 차이
     * - MAX_FILE_BYTES: 5.3MB → 20MB. 참조: legacy-deviation.md §6 외부 연동
     * - S3 키 형식: timestamp+한글 파일명 → `uploads/notice/<YYYY>/<MM>/<DD>/<UUID>.<ext>`. 참조: legacy-deviation.md §6 외부 연동
     * - parentId Long lookup: RecordId__c String → parent_id Long FK. 참조: Spec #616
     * - ContentDocument/ContentVersion 자체 부재 — Tech stack 차이로 자연 소멸 (R8 race 위험 없음)
     * - Content-type whitelist 명시적 (StorageConstants.ALLOWED_CONTENT_TYPES) — 레거시 빈 문자열 fallthrough(R15) 제거
     * - 응답 sortOrder=0 고정 (단일 업로드 응답). 다건 read 시 createdAt 기준 재계산 — `getNoticeDetail` 동일 정책
     */
    @Transactional
    fun uploadNoticeImage(noticeId: Long, file: MultipartFile): NoticeImageResponse {
        if (noticeId <= 0) throw InvalidNoticeIdException()
        findActiveNotice(noticeId)

        val key = fileStorageService.uploadNoticeImage(file, noticeId)

        val uploadFile = UploadFile(
            name = file.originalFilename,
            uniqueKey = key,
            fileSize = formatFileSize(file.size),
            parentType = UploadFileParentTypes.NOTICE,
            parentId = noticeId,
            isDeleted = false
        )
        val saved = uploadFileRepository.save(uploadFile)

        return NoticeImageResponse(
            id = saved.id,
            url = storageService.getPresignedUrl(saved.uniqueKey!!, StorageConstants.NOTICE_PRESIGN_TTL_SECONDS),
            sortOrder = 0
        )
    }

    /**
     * 공지사항 첨부 이미지 삭제 — S3 DELETE + UploadFile soft-delete.
     *
     * ## 레거시 매핑
     * - SF Apex: `S3_FileUploadController.cls#deleteImg` + `#deleteUploadFileRecord`
     * - flow-legacy: `flow-legacy-notice-attachment.yaml` step 10
     * - origin spec: #656
     *
     * ## 레거시 동작 요약
     * 1. 입력: fileName + fileDummy(timestamp+finalName) + fileId(ContentDocumentId)
     * 2. AWS_S3 callout DELETE — 204 응답 시 다음 단계, 비-204 응답은 silent (R14)
     * 3. deleteUploadFileRecord — UploadFile__c WHERE UniqueKey__c=:dummy AND Name=:name (LIMIT 1 부재 — R6) → DML hard delete
     *
     * ## 신규 차이
     * - hard-delete → soft-delete (`is_deleted=true`). 이유: R14 silent failure 자동 보강 + audit 추적성 확보
     * - S3 DELETE idempotent 보장 (NoSuchKeyException swallow) — 비-204 silent 잔재 제거
     * - parent 정합 가드 추가 (`parent_type='NOTICE' AND parent_id=noticeId`) — R6 LIMIT 1 부재 위험 회피
     */
    @Transactional
    fun deleteNoticeImage(noticeId: Long, imageId: Long) {
        if (noticeId <= 0) throw InvalidNoticeIdException()
        if (imageId <= 0) throw InvalidImageIdException()

        val uploadFile = uploadFileRepository
            .findByIdAndParentTypeAndParentIdAndIsDeletedFalse(imageId, UploadFileParentTypes.NOTICE, noticeId)
            ?: throw InvalidImageIdException()

        uploadFile.uniqueKey?.takeIf { it.isNotBlank() }?.let { storageService.deletePrivate(it) }
        uploadFile.isDeleted = true
    }

    fun getNoticeFormMeta(): NoticeFormMetaResponse {
        val scopes = NoticeScope.entries.map {
            ScopeOption(code = it.displayName, name = it.displayName)
        }

        val categories = NoticeCategory.entries.map {
            CategoryOption(code = it.apiCode, name = it.displayName)
        }

        val branches = organizationRepository.findAll()
            .filter { !it.orgNameLevel3.isNullOrBlank() && !it.orgNameLevel4.isNullOrBlank() }
            .map { org ->
                val branchName = if (org.orgNameLevel5.isNullOrBlank()) {
                    "[${org.orgNameLevel3}] ${org.orgNameLevel4}"
                } else {
                    "[${org.orgNameLevel3}] ${org.orgNameLevel4}-${org.orgNameLevel5}"
                }
                val branchCode = if (!org.costCenterLevel5.isNullOrBlank()) {
                    org.costCenterLevel5!!
                } else {
                    org.costCenterLevel4 ?: ""
                }
                BranchOption(branchCode = branchCode, branchName = branchName)
            }
            .distinctBy { it.branchCode }
            .sortedBy { it.branchName }

        return NoticeFormMetaResponse(scopes = scopes, categories = categories, branches = branches)
    }

    private fun findActiveNotice(noticeId: Long): Notice {
        val notice = noticeRepository.findById(noticeId)
            .orElseThrow { NoticePostNotFoundException() }
        if (notice.isDeleted == true) throw NoticePostNotFoundException()
        return notice
    }

    private fun parseCategory(category: String): NoticeCategory {
        return try {
            NoticeCategory.fromApiCode(category)
        } catch (_: IllegalArgumentException) {
            throw InvalidNoticeCategoryException()
        }
    }

    private fun parseScope(scope: String): NoticeScope {
        return NoticeScope.fromDisplayNameOrNull(scope) ?: throw InvalidNoticeScopeException()
    }

    private fun validateBranch(category: NoticeCategory, branch: String?, branchCode: String?) {
        if (category == NoticeCategory.BRANCH) {
            if (branch.isNullOrBlank() || branchCode.isNullOrBlank()) {
                throw BranchRequiredException()
            }
        }
    }

    private fun formatFileSize(bytes: Long): String {
        return when {
            bytes < 1024L -> "$bytes B"
            bytes < 1024L * 1024L -> "${bytes / 1024L} KB"
            bytes < 1024L * 1024L * 1024L -> "${bytes / (1024L * 1024L)} MB"
            else -> "${bytes / (1024L * 1024L * 1024L)} GB"
        }
    }
}
