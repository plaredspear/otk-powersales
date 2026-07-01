package com.otoki.powersales.domain.support.notice.service

import com.otoki.powersales.platform.auth.entity.AppAuthority
import com.otoki.powersales.platform.auth.exception.EmployeeNotFoundException
import com.otoki.powersales.platform.common.entity.UploadFile
import com.otoki.powersales.platform.common.repository.UploadFileRepository
import com.otoki.powersales.platform.common.service.FileStorageService
import com.otoki.powersales.platform.common.storage.StorageConstants
import com.otoki.powersales.platform.common.storage.StorageService
import com.otoki.powersales.platform.common.storage.UploadFileParentTypes
import com.otoki.powersales.domain.support.notice.dto.request.NoticeCreateRequest
import com.otoki.powersales.domain.support.notice.dto.request.NoticeUpdateRequest
import com.otoki.powersales.domain.support.notice.dto.response.BranchOption
import com.otoki.powersales.domain.support.notice.dto.response.CategoryOption
import com.otoki.powersales.domain.support.notice.dto.response.NoticeFormMetaResponse
import com.otoki.powersales.domain.support.notice.dto.response.NoticeImageResponse
import com.otoki.powersales.domain.support.notice.dto.response.NoticeInlineImageResponse
import com.otoki.powersales.domain.support.notice.dto.response.NoticeMutationResponse
import com.otoki.powersales.domain.support.notice.dto.response.NoticePostDetailResponse
import com.otoki.powersales.domain.support.notice.dto.response.NoticePostListResponse
import com.otoki.powersales.domain.support.notice.dto.response.NoticePostSummaryResponse
import com.otoki.powersales.domain.support.notice.dto.response.ScopeOption
import com.otoki.powersales.domain.support.notice.entity.Notice
import com.otoki.powersales.domain.support.notice.enums.NoticeCategory
import com.otoki.powersales.domain.support.notice.enums.NoticeScope
import com.otoki.powersales.domain.support.notice.exception.BranchNoticeOnlyException
import com.otoki.powersales.domain.support.notice.exception.BranchRequiredException
import com.otoki.powersales.domain.support.notice.exception.InvalidImageIdException
import com.otoki.powersales.domain.support.notice.exception.InvalidNoticeCategoryException
import com.otoki.powersales.domain.support.notice.exception.InvalidNoticeIdException
import com.otoki.powersales.domain.support.notice.exception.InvalidNoticeScopeException
import com.otoki.powersales.domain.support.notice.exception.NoticePostNotFoundException
import com.otoki.powersales.domain.support.notice.repository.NoticeRepository
import com.otoki.powersales.domain.org.employee.entity.Employee
import com.otoki.powersales.domain.org.employee.repository.EmployeeRepository
import com.otoki.powersales.domain.org.organization.repository.OrganizationRepository
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
        // placeholder <img> 의 생성/파싱 형식은 NoticeImagePlaceholder (SoT) 가 소유한다.
        // 조회측 rewrite 는 그 정규식 2개를 그대로 참조 — 생성측(마이그레이션 치환)과 형식 정합 보장.
        private val INLINE_IMG_REGEX = NoticeImagePlaceholder.PLACEHOLDER_IMG_REGEX
        private val SRC_ATTR_REGEX = NoticeImagePlaceholder.SRC_ATTR_REGEX

        // 본문 인라인 이미지 식별자 (upload_file.upload_kbn). 첨부 목록과 구분하기 위함.
        private const val UPLOAD_KBN_INLINE = "INLINE"
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

        // 본문 rewrite 용 refid → uniqueKey 맵 (1회 조회 재사용, N+1 없음).
        // 마이그레이션분: refid = upload_file.sfid. 신규 업로드분: refid = upload_file.id (Long 문자열).
        // 둘을 하나의 맵에 합쳐 rewrite 가 출처 무관하게 매칭하도록 한다 (id/sfid 충돌 가능성 없음 — sfid 는 18자 영숫자).
        val uniqueKeyByRefid: Map<String, String> = buildMap {
            uploadFiles.forEach { file ->
                val key = file.uniqueKey ?: return@forEach
                file.sfid?.takeIf { it.isNotBlank() }?.let { put(it, key) }
                put(file.id.toString(), key)
            }
        }

        val content = rewriteInlineImages(notice.contents ?: "", uniqueKeyByRefid)

        // 본문 인라인 이미지(upload_kbn=INLINE)는 하단 첨부 목록에서 제외 — 본문에 이미 렌더링되므로 중복 노출 방지.
        val images = uploadFiles
            .filter { it.uploadKbn != UPLOAD_KBN_INLINE }
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
    fun createNotice(request: NoticeCreateRequest, creatorId: Long, role: String?): NoticeMutationResponse {
        val cat = parseCategory(request.category)
        requireBranchNoticeForLeader(role, cat)
        val noticeScope = parseScope(request.scope)

        // 지점공지(BRANCH) 의 지점/지점코드는 요청 값이 아니라 등록자(조장/지점장) 소속 지점을 권위로 사용한다.
        // 이로써 누가 등록하든 본인 지점 공지로만 등록되며, 프론트 우회로 타 지점 공지를 만들 수 없다.
        val creator = employeeRepository.findById(creatorId)
            .orElseThrow { EmployeeNotFoundException() }
        val (branch, branchCode) = resolveCreatorBranch(cat, creator)

        val notice = Notice(
            name = request.title,
            scope = noticeScope,
            category = cat,
            contents = request.content,
            branch = branch,
            branchCode = branchCode,
            employee = creator
        )
        val saved = noticeRepository.save(notice)
        backfillInlineImages(saved.id, saved.contents)
        return NoticeMutationResponse.Companion.from(saved)
    }

    @Transactional
    fun updateNotice(noticeId: Long, request: NoticeUpdateRequest, role: String?): NoticeMutationResponse {
        if (noticeId <= 0) throw InvalidNoticeIdException()
        val notice = findActiveNotice(noticeId)

        val cat = parseCategory(request.category)
        requireBranchNoticeForLeader(role, cat)
        val noticeScope = parseScope(request.scope)

        notice.name = request.title
        notice.scope = noticeScope
        notice.category = cat
        notice.contents = request.content

        // 등록(createNotice) 과 동일하게, 지점공지의 지점/지점코드는 요청 값이 아니라 공지 소유자(등록자)
        // 소속 지점을 권위로 강제한다. 이로써 등록 후 수정으로 타 지점 코드로 바꾸는 우회를 차단한다.
        // 소유자(employee) 정보가 없는 레거시 데이터는 기존 지점값을 보존한다.
        if (cat == NoticeCategory.BRANCH) {
            val ownerId = notice.employee?.id
            if (ownerId != null) {
                val owner = employeeRepository.findById(ownerId)
                    .orElseThrow { EmployeeNotFoundException() }
                val (branch, branchCode) = resolveCreatorBranch(cat, owner)
                notice.branch = branch
                notice.branchCode = branchCode
            } else if (notice.branchCode.isNullOrBlank()) {
                throw BranchRequiredException()
            }
            // ownerId == null 이고 기존 지점값이 있으면 보존 (변경하지 않음)
        } else {
            notice.branch = null
            notice.branchCode = null
        }

        // 영속 entity 변경은 dirty checking 으로 flush — 명시 save 불필요. backfill 은 같은 tx 내 auto-flush 로 정합.
        backfillInlineImages(notice.id, notice.contents)
        return NoticeMutationResponse.Companion.from(notice)
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
     * 공지 본문 인라인 이미지 업로드 (신규 작성/수정 화면 Quill 드래그앤드롭).
     *
     * 첨부 업로드([uploadNoticeImage])와 달리:
     * - parent_id 는 아직 미정(신규 작성 시 noticeId 부재) → null 로 저장하고 공지 저장 시 [backfillInlineImages] 가 채운다.
     *   (수정 화면은 noticeId 가 있어도 일관성을 위해 동일하게 backfill 경로를 탄다.)
     * - upload_kbn=INLINE 으로 표기 → 조회 시 하단 첨부 목록에서 제외(본문에만 렌더링).
     * - 응답으로 placeholder(`<img data-refid="{id}">`)와 미리보기 presigned URL 을 함께 반환 → 클라이언트가 본문엔
     *   placeholder 를, 에디터엔 presigned 로 보여준다.
     *
     * refid = upload_file.id (Long). 마이그레이션분(refid=sfid)과 충돌하지 않으며 조회측 rewrite 는 두 키를 모두 매칭한다.
     */
    @Transactional
    fun uploadNoticeInlineImage(file: MultipartFile): NoticeInlineImageResponse {
        val key = fileStorageService.uploadNoticeImage(file, 0L)

        val uploadFile = UploadFile(
            name = file.originalFilename,
            uniqueKey = key,
            fileSize = formatFileSize(file.size),
            parentType = UploadFileParentTypes.NOTICE,
            parentId = null,
            uploadKbn = UPLOAD_KBN_INLINE,
            isDeleted = false
        )
        val saved = uploadFileRepository.save(uploadFile)

        val refid = saved.id.toString()
        return NoticeInlineImageResponse(
            refid = refid,
            placeholder = NoticeImagePlaceholder.build(refid, file.originalFilename ?: ""),
            previewUrl = storageService.getPresignedUrl(saved.uniqueKey!!, StorageConstants.NOTICE_PRESIGN_TTL_SECONDS)
        )
    }

    /**
     * 본문 content 가 참조하는 인라인 이미지(placeholder refid = upload_file.id)의 parent_id 를 noticeId 로 backfill 한다.
     * - 신규 업로드분(INLINE)만 대상 — 마이그레이션 refid(sfid, 비숫자)는 toLongOrNull 에서 자연 제외.
     * - 본문에서 빠진(사용자가 삭제한) 이미지는 backfill 하지 않아 parent_id=null 고아로 남는다 (S3 정리는 별도 배치 영역, 본 스펙 범위 외).
     * create/update 양쪽 끝에서 호출.
     *
     * 보안: backfill 대상은 **아직 부모가 없는(parent_id=null) 임시 INLINE 업로드분**으로만 한정한다.
     * refid 는 클라이언트가 보낸 본문 HTML 에서 추출되므로, 이미 다른 공지에 소속된 파일(parent_id != null)을 무차별
     * 재부모화하면 본문에 타 공지의 upload_file.id 를 심어 그 이미지를 자기 공지로 탈취할 수 있다(IDOR). 이를 차단한다.
     */
    private fun backfillInlineImages(noticeId: Long, content: String?) {
        val ids = NoticeImagePlaceholder.extractRefids(content ?: "")
            .mapNotNull { it.toLongOrNull() }.distinct()
        if (ids.isEmpty()) return

        uploadFileRepository.findByIdInAndParentTypeAndIsDeletedFalse(ids, UploadFileParentTypes.NOTICE)
            .filter { it.uploadKbn == UPLOAD_KBN_INLINE && it.parentId == null }
            .forEach { it.parentId = noticeId }
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

    fun getNoticeFormMeta(role: String?): NoticeFormMetaResponse {
        val scopes = NoticeScope.entries.map {
            ScopeOption(code = it.displayName, name = it.displayName)
        }

        // 조장/지점장은 지점공지만 작성 가능 → 카테고리 옵션도 지점공지(BRANCH)만 노출한다.
        // 프론트는 내려온 옵션을 그대로 렌더링하므로 UI 제한이 서버 권위로 통일된다.
        val visibleCategories = if (isBranchNoticeOnlyRole(role)) {
            listOf(NoticeCategory.BRANCH)
        } else {
            NoticeCategory.entries
        }
        val categories = visibleCategories.map {
            CategoryOption(code = it.apiCode, name = it.displayName)
        }

        val branches = loadBranchOptions()

        return NoticeFormMetaResponse(scopes = scopes, categories = categories, branches = branches)
    }

    /**
     * 조장/지점장 여부 판별. Employee.role(= SF DKRetail__AppAuthority__c) picklist value 기준.
     * 이 두 권한은 지점공지(BRANCH)만 작성 가능하다.
     */
    private fun isBranchNoticeOnlyRole(role: String?): Boolean =
        role == AppAuthority.LEADER || role == AppAuthority.BRANCH_MANAGER

    /**
     * 조장/지점장이 지점공지(BRANCH) 외 카테고리로 등록/수정을 시도하면 차단한다.
     */
    private fun requireBranchNoticeForLeader(role: String?, category: NoticeCategory) {
        if (isBranchNoticeOnlyRole(role) && category != NoticeCategory.BRANCH) {
            throw BranchNoticeOnlyException()
        }
    }

    /**
     * 조직 마스터(Organization) 에서 지점 옵션 목록을 생성한다.
     * 지점코드는 5레벨(costCenterLevel5) 우선, 없으면 4레벨(costCenterLevel4).
     */
    private fun loadBranchOptions(): List<BranchOption> =
        organizationRepository.findAll()
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

    /**
     * 지점공지(BRANCH) 등록 시 저장할 (지점명, 지점코드) 를 등록자 소속 지점에서 해석한다.
     * - 회사공지/교육: (null, null)
     * - 지점공지: 등록자 costCenterCode 가 권위. 미보유 시 등록 차단(BranchRequiredException).
     *   지점명은 조직 마스터의 지점 옵션에서 코드로 매칭, 없으면 등록자 orgName 으로 폴백.
     */
    private fun resolveCreatorBranch(category: NoticeCategory, creator: Employee): Pair<String?, String?> {
        if (category != NoticeCategory.BRANCH) return null to null

        val branchCode = creator.costCenterCode
        if (branchCode.isNullOrBlank()) throw BranchRequiredException()

        val branchName = loadBranchOptions().firstOrNull { it.branchCode == branchCode }?.branchName
            ?: creator.orgName
        return branchName to branchCode
    }

    private fun findActiveNotice(noticeId: Long): Notice {
        val notice = noticeRepository.findById(noticeId)
            .orElseThrow { NoticePostNotFoundException() }
        if (notice.isDeleted == true) throw NoticePostNotFoundException()
        return notice
    }

    private fun parseCategory(category: String): NoticeCategory {
        return try {
            NoticeCategory.Companion.fromApiCode(category)
        } catch (_: IllegalArgumentException) {
            throw InvalidNoticeCategoryException()
        }
    }

    private fun parseScope(scope: String): NoticeScope {
        return NoticeScope.Companion.fromDisplayNameOrNull(scope) ?: throw InvalidNoticeScopeException()
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
