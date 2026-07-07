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
import com.otoki.powersales.domain.support.notice.dto.response.NoticePushInfo
import com.otoki.powersales.domain.support.notice.dto.response.NoticePushResultResponse
import com.otoki.powersales.domain.support.notice.dto.response.NoticePostListResponse
import com.otoki.powersales.domain.support.notice.dto.response.NoticePostSummaryResponse
import com.otoki.powersales.domain.support.notice.dto.response.ScopeOption
import com.otoki.powersales.domain.support.notice.entity.Notice
import com.otoki.powersales.domain.support.notice.entity.NoticePushLog
import com.otoki.powersales.domain.support.notice.enums.NoticeCategory
import com.otoki.powersales.domain.support.notice.enums.NoticeScope
import com.otoki.powersales.domain.support.notice.enums.NoticeStatus
import com.otoki.powersales.domain.support.notice.exception.BranchNoticeOnlyException
import com.otoki.powersales.domain.support.notice.exception.BranchRequiredException
import com.otoki.powersales.domain.support.notice.exception.InvalidImageIdException
import com.otoki.powersales.domain.support.notice.exception.InvalidNoticeCategoryException
import com.otoki.powersales.domain.support.notice.exception.InvalidNoticeIdException
import com.otoki.powersales.domain.support.notice.exception.InvalidNoticeScopeException
import com.otoki.powersales.domain.support.notice.exception.NoticeNotPublishedException
import com.otoki.powersales.domain.support.notice.exception.NoticePostNotFoundException
import com.otoki.powersales.domain.support.notice.exception.NoticeScopeNotPushableException
import com.otoki.powersales.domain.support.notice.exception.NoticeCategoryNotPushableException
import com.otoki.powersales.domain.support.notice.exception.NoticeVersionConflictException
import com.otoki.powersales.domain.support.notice.repository.NoticePushLogRepository
import com.otoki.powersales.domain.support.notice.repository.NoticeRepository
import com.otoki.powersales.platform.push.sender.FcmSender
import com.otoki.powersales.platform.push.sender.FcmSendResult
import com.otoki.powersales.domain.org.employee.entity.Employee
import com.otoki.powersales.domain.org.employee.repository.EmployeeRepository
import com.otoki.powersales.domain.activity.schedule.service.WomenScheduleBranchResolver
import com.otoki.powersales.platform.auth.web.WebUserPrincipal
import com.otoki.powersales.domain.support.notice.exception.BranchNotAllowedException
import org.slf4j.LoggerFactory
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile
import java.util.Base64

@Service
@Transactional(readOnly = true)
class NoticeService(
    private val noticeRepository: NoticeRepository,
    private val noticePushLogRepository: NoticePushLogRepository,
    private val uploadFileRepository: UploadFileRepository,
    private val employeeRepository: EmployeeRepository,
    private val fileStorageService: FileStorageService,
    private val storageService: StorageService,
    private val fcmSender: FcmSender,
    private val womenScheduleBranchResolver: WomenScheduleBranchResolver
) {

    companion object {
        private val log = LoggerFactory.getLogger(NoticeService::class.java)

        // placeholder <img> 의 생성/파싱 형식은 NoticeImagePlaceholder (SoT) 가 소유한다.
        // 조회측 rewrite 는 그 정규식 2개를 그대로 참조 — 생성측(마이그레이션 치환)과 형식 정합 보장.
        private val INLINE_IMG_REGEX = NoticeImagePlaceholder.PLACEHOLDER_IMG_REGEX
        private val SRC_ATTR_REGEX = NoticeImagePlaceholder.SRC_ATTR_REGEX

        // 본문 인라인 이미지 식별자 (upload_file.upload_kbn). 첨부 목록과 구분하기 위함.
        private const val UPLOAD_KBN_INLINE = "INLINE"

        // base64 data URI 정규화 시 업로드 파일명(확장자 파생용). 실제 파일명 정보가 없어 고정 stem 사용.
        private const val INLINE_UPLOAD_STEM = "inline"

        // 공지 push notification 제목 (본문은 공지 제목). 모바일 data payload 의 딥링크 타입.
        private const val PUSH_TITLE = "공지사항"
        private const val PUSH_TYPE_NOTICE = "notice"
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
    fun getNoticeDetail(noticeId: Long, publishedOnly: Boolean): NoticePostDetailResponse {
        val notice = noticeRepository.findById(noticeId)
            .filter { it.isDeleted != true }
            // 모바일/사용자용 조회(publishedOnly=true)는 임시저장(DRAFT) 상세를 URL 직접 접근으로도 볼 수 없다.
            .filter { !publishedOnly || it.status == NoticeStatus.PUBLISHED }
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

        // push 발송 이력 (admin 상세에서 중복 발송 경고/결과 표시용). publishedOnly(모바일)에는 노출 불필요하나
        // 조회 1회 비용이라 공통 노출 — 모바일 응답은 필드를 사용하지 않는다.
        val pushSentCount = noticePushLogRepository.countByNoticeId(notice.id)
        val lastPush = noticePushLogRepository.findFirstByNoticeIdOrderByCreatedAtDesc(notice.id)?.let {
            NoticePushInfo(
                sentAt = it.createdAt,
                targetCount = it.targetCount,
                successCount = it.successCount,
                failureCount = it.failureCount
            )
        }

        return NoticePostDetailResponse(
            id = notice.id,
            scope = notice.scope?.displayName,
            category = notice.category?.apiCode ?: "",
            categoryName = notice.category?.displayName ?: "",
            status = (notice.status ?: NoticeStatus.PUBLISHED).apiCode,
            statusName = (notice.status ?: NoticeStatus.PUBLISHED).displayName,
            title = notice.name ?: "",
            content = content,
            branch = notice.branch,
            branchCode = notice.branchCode,
            createdAt = notice.createdAt,
            version = notice.version,
            images = images,
            pushSentCount = pushSentCount,
            lastPush = lastPush
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

    /**
     * 본문에 base64 data URI 로 박혀 들어온 인라인 이미지(`<img src="data:image/...;base64,...">`)를
     * private S3 로 업로드하고 placeholder(`<img src="notice-image://{id}" data-refid="{id}">`)로 치환한다.
     *
     * 웹 Quill 에디터에 이미지를 '붙여넣기' 하면 정상 업로드 경로([uploadNoticeInlineImage])를 타지 않고
     * base64 가 본문에 그대로 삽입될 수 있다. 이 경우 (1) DB contents 가 비대해지고 (2) 모바일은 http 가 아닌
     * src 를 렌더하지 못해 이미지가 깨진다. 저장 시점에 여기서 정규화하여 어떤 클라이언트/경로로 들어와도
     * 본문에는 placeholder 만 남고 조회 시 presigned 로 rewrite 되게 한다(설계 SoT: [NoticeImagePlaceholder]).
     *
     * 업로드분은 즉시 이 공지 소속(parentId=noticeId, upload_kbn=INLINE)으로 생성되므로 이어지는
     * [syncInlineImages] 의 backfill/cleanup 대상에서 자연히 보존된다(본문이 그 refid 를 참조).
     * 허용 외 content-type/디코드 실패분은 원본 태그를 보존한다(placeholder 미치환). content-type/크기 검증은
     * 정상 업로드([uploadPrivate])와 동일 규칙 — 상한 초과 시 예외가 전파되어 저장이 거부된다.
     */
    private fun normalizeInlineBase64Images(noticeId: Long, html: String?): String? {
        if (html.isNullOrEmpty() || !html.contains("data:image", ignoreCase = true)) return html
        return NoticeImagePlaceholder.DATA_URI_IMG_REGEX.replace(html) { match ->
            val contentType = match.groupValues[1].lowercase()
            if (contentType !in StorageConstants.ALLOWED_CONTENT_TYPES) return@replace match.value
            val bytes = try {
                Base64.getMimeDecoder().decode(match.groupValues[2])
            } catch (_: IllegalArgumentException) {
                return@replace match.value
            }
            if (bytes.isEmpty()) return@replace match.value

            val fileName = "$INLINE_UPLOAD_STEM.${extensionForContentType(contentType)}"
            val result = storageService.uploadPrivate(
                domain = "notice",
                originalName = fileName,
                bytes = bytes,
                contentType = contentType
            )
            val saved = uploadFileRepository.save(
                UploadFile(
                    name = fileName,
                    uniqueKey = result.key,
                    fileSize = formatFileSize(bytes.size.toLong()),
                    parentType = UploadFileParentTypes.NOTICE,
                    parentId = noticeId,
                    uploadKbn = UPLOAD_KBN_INLINE,
                    isDeleted = false
                )
            )
            NoticeImagePlaceholder.build(saved.id.toString(), "")
        }
    }

    private fun extensionForContentType(contentType: String): String = when (contentType) {
        "image/png" -> "png"
        "image/jpeg", "image/jpg" -> "jpg"
        "image/gif" -> "gif"
        "image/webp" -> "webp"
        "image/heic" -> "heic"
        else -> "img"
    }

    /**
     * 기존 공지 본문에 base64 로 저장된 인라인 이미지를 일괄 정규화(S3 업로드 + placeholder 치환)한다.
     * 저장 시점 정규화([normalizeInlineBase64Images]) 도입 이전에 저장된 공지를 소급 복구하기 위한 일회성 관리자 작업.
     * 공지별 실패(허용 외 타입/상한 초과 등)는 로그만 남기고 건너뛰어 나머지 공지 정규화를 계속한다.
     *
     * @return 실제로 본문이 정규화(변경)된 공지 수
     */
    @Transactional
    fun migrateInlineBase64Images(): Int {
        val notices = noticeRepository.findByContentsContaining("data:image").filter { it.isDeleted != true }
        var migrated = 0
        for (notice in notices) {
            try {
                val normalized = normalizeInlineBase64Images(notice.id, notice.contents)
                if (normalized != notice.contents) {
                    notice.contents = normalized
                    migrated++
                }
            } catch (ex: Exception) {
                log.warn("공지 {} 의 base64 인라인 이미지 정규화 실패 — 건너뜀", notice.id, ex)
            }
        }
        log.info("base64 인라인 이미지 정규화 완료 — 대상 {}건 중 {}건 변경", notices.size, migrated)
        return migrated
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
            status = (status ?: NoticeStatus.PUBLISHED).apiCode,
            statusName = (status ?: NoticeStatus.PUBLISHED).displayName,
            scope = scope?.displayName,
            title = name ?: "",
            branch = branch,
            department = employee?.orgName,
            authorName = ownerUser?.lastName,
            createdAt = createdAt
        )

    @Transactional
    fun createNotice(request: NoticeCreateRequest, principal: WebUserPrincipal): NoticeMutationResponse {
        val cat = parseCategory(request.category)
        requireBranchNoticeForLeader(principal.role, cat)
        val noticeScope = parseScope(request.scope)

        val creator = employeeRepository.findById(principal.requireEmployeeId())
            .orElseThrow { EmployeeNotFoundException() }
        // 지점공지(BRANCH) 의 지점/지점코드는 작성자가 권한 스코프 안에서 고른 값을 저장한다
        // (행사마스터/여사원일정과 동일한 WomenScheduleBranchResolver 화이트리스트). 스코프 밖 코드는 거부(IDOR).
        val (branch, branchCode) = resolveSelectedBranch(cat, principal, request.branchCode)

        val notice = Notice(
            name = request.title,
            scope = noticeScope,
            category = cat,
            contents = request.content,
            branch = branch,
            branchCode = branchCode,
            employee = creator,
            // 발행 버튼=PUBLISHED, 임시저장 버튼=DRAFT.
            status = if (request.publish) NoticeStatus.PUBLISHED else NoticeStatus.DRAFT
        )
        val saved = noticeRepository.save(notice)
        // 붙여넣기 등으로 본문에 base64 로 박혀 들어온 이미지를 S3 업로드 + placeholder 로 정규화한다
        // (parentId=noticeId 로 즉시 소속 → 이어지는 syncInlineImages 가 본문 참조로 자연히 보존).
        saved.contents = normalizeInlineBase64Images(saved.id, saved.contents)
        syncInlineImages(saved.id, saved.contents, request.sessionUploadedRefids)
        return NoticeMutationResponse.Companion.from(saved)
    }

    @Transactional
    fun updateNotice(noticeId: Long, request: NoticeUpdateRequest, principal: WebUserPrincipal): NoticeMutationResponse {
        if (noticeId <= 0) throw InvalidNoticeIdException()
        val notice = findActiveNotice(noticeId)

        // 낙관적 락 — 수정 화면 진입 시 받은 version 과 현재 DB version 이 다르면 그 사이 다른 사용자가 저장한 것.
        // 나중 저장자를 409 로 거부해 lost update + 인라인 이미지 교차 오삭제를 차단한다(cleanup 실행 전에 중단).
        // request.version 이 null(구 클라이언트/외부 호출)이면 이 선제 검사는 생략 — 단 JPA @Version 이 flush 시점에
        // "로드~flush 사이 변경" 은 여전히 잡는다(ObjectOptimisticLockingFailureException).
        if (request.version != null && request.version != notice.version) {
            throw NoticeVersionConflictException()
        }

        val cat = parseCategory(request.category)
        requireBranchNoticeForLeader(principal.role, cat)
        val noticeScope = parseScope(request.scope)

        notice.name = request.title
        notice.scope = noticeScope
        notice.category = cat
        // 붙여넣기 등으로 본문에 base64 로 박혀 들어온 이미지를 S3 업로드 + placeholder 로 정규화한다.
        notice.contents = normalizeInlineBase64Images(notice.id, request.content)
        // 발행 버튼=PUBLISHED, 임시저장 버튼=DRAFT(발행취소 효과). "임시저장=무조건 DRAFT" 정책.
        notice.status = if (request.publish) NoticeStatus.PUBLISHED else NoticeStatus.DRAFT

        // 지점공지의 지점/지점코드는 작성자가 권한 스코프 안에서 고른 값으로 갱신한다(등록과 동일 규칙).
        // 요청 branchCode 가 없으면(구 클라이언트/미변경) 기존 값을 보존한다. 스코프 밖 코드는 거부(IDOR).
        if (cat == NoticeCategory.BRANCH) {
            if (request.branchCode.isNullOrBlank()) {
                if (notice.branchCode.isNullOrBlank()) throw BranchRequiredException()
                // 기존 지점값 보존
            } else {
                val (branch, branchCode) = resolveSelectedBranch(cat, principal, request.branchCode)
                notice.branch = branch
                notice.branchCode = branchCode
            }
        } else {
            notice.branch = null
            notice.branchCode = null
        }

        // 영속 entity 변경은 dirty checking 으로 flush — 명시 save 불필요. sync 는 같은 tx 내 auto-flush 로 정합.
        syncInlineImages(notice.id, notice.contents, request.sessionUploadedRefids)
        return NoticeMutationResponse.Companion.from(notice)
    }

    @Transactional
    fun deleteNotice(noticeId: Long) {
        if (noticeId <= 0) throw InvalidNoticeIdException()
        val notice = findActiveNotice(noticeId)
        notice.isDeleted = true
        noticeRepository.save(notice)
    }

    /** 공지 발행 — status 를 PUBLISHED 로 전환(모바일 노출). 상세화면 발행 버튼용. */
    @Transactional
    fun publishNotice(noticeId: Long): NoticeMutationResponse {
        if (noticeId <= 0) throw InvalidNoticeIdException()
        val notice = findActiveNotice(noticeId)
        notice.status = NoticeStatus.PUBLISHED
        return NoticeMutationResponse.Companion.from(notice)
    }

    /** 공지 발행취소 — status 를 DRAFT 로 전환(모바일 미노출). 상세화면 발행취소 버튼용. */
    @Transactional
    fun unpublishNotice(noticeId: Long): NoticeMutationResponse {
        if (noticeId <= 0) throw InvalidNoticeIdException()
        val notice = findActiveNotice(noticeId)
        notice.status = NoticeStatus.DRAFT
        return NoticeMutationResponse.Companion.from(notice)
    }

    /**
     * 공지 FCM push 즉시 발송 — web 관리자 상세화면 '푸시 발송' 버튼용.
     *
     * 발송 대상은 그 공지가 앱 목록에 노출되는 사용자와 동일하게 선별한다 (조회 노출 규칙 정합):
     * - 회사공지(COMPANY): 앱 로그인 활성(재직) + FCM 토큰 보유 전 사용자
     * - 지점공지(BRANCH): 앱 로그인 활성 + costCenterCode 가 공지 branchCode 와 일치하는 사용자만
     * - 교육(EDUCATION): 모바일 목록 미노출(별도 교육 메뉴 소관) → 발송 대상 아님(차단).
     * scope=영업사원 공지는 앱에 노출되지 않으므로 발송 대상이 아니다 (레거시 Heroku 조회 정합).
     *
     * 알림 탭 시 딥링크(해당 공지 상세 이동)를 위해 data payload({"type":"notice","noticeId":...})를 함께 실는다.
     * 발송 결과는 notice_push_log 에 기록한다 (재발송/결과 확인 근거). 중복 발송 자체는 허용 —
     * 재발송 여부 판단은 이력을 내려받은 클라이언트(경고 모달)가 담당한다.
     */
    @Transactional
    fun sendPush(noticeId: Long, senderId: Long): NoticePushResultResponse {
        if (noticeId <= 0) throw InvalidNoticeIdException()
        val notice = findActiveNotice(noticeId)

        // 발행된 공지만 발송 가능 (임시저장은 모바일 미노출).
        if (notice.status != NoticeStatus.PUBLISHED) throw NoticeNotPublishedException()
        // 영업사원 scope 공지는 모바일 미노출 → 발송 대상 아님.
        if (notice.scope == NoticeScope.SALES_EMPLOYEE) throw NoticeScopeNotPushableException()

        val category = notice.category ?: throw InvalidNoticeCategoryException()
        // 교육 공지는 모바일 목록 미노출 → push 도 대상 아님 (조회 노출 규칙 정합, 오발송 차단).
        if (category == NoticeCategory.EDUCATION) throw NoticeCategoryNotPushableException()
        val tokens = noticeRepository.findPushTargetTokens(category, notice.branchCode)

        val result = if (tokens.isEmpty()) {
            FcmSendResult.EMPTY
        } else {
            fcmSender.sendNotificationToTokens(
                tokens = tokens,
                title = PUSH_TITLE,
                body = notice.name ?: "",
                data = mapOf("type" to PUSH_TYPE_NOTICE, "noticeId" to notice.id.toString())
            )
        }

        val sender = employeeRepository.findById(senderId).orElse(null)
        noticePushLogRepository.save(
            NoticePushLog(
                noticeId = notice.id,
                sentBy = sender,
                targetScope = notice.scope?.displayName,
                targetCount = tokens.size,
                successCount = result.successCount,
                failureCount = result.failureCount
            )
        )

        return NoticePushResultResponse(
            targetCount = tokens.size,
            successCount = result.successCount,
            failureCount = result.failureCount
        )
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
     * 공지 저장 시 본문이 참조하는 인라인 이미지와 실제 upload_file 을 동기화한다.
     * (1) backfill: 본문 refid 중 parent_id=null 인 임시 INLINE 업로드분을 이 공지로 소속시킨다.
     * (2) cleanup: 본문에서 빠진(사용자가 삽입 후 삭제한) INLINE 이미지를 S3+soft-delete 로 정리한다.
     * create/update 양쪽 끝에서 호출.
     *
     * ## backfill 보안 (IDOR)
     * backfill 대상은 **아직 부모가 없는(parent_id=null) 임시 INLINE 업로드분**으로만 한정한다.
     * refid 는 클라이언트가 보낸 본문 HTML 에서 추출되므로, 이미 다른 공지에 소속된 파일(parent_id != null)을 무차별
     * 재부모화하면 본문에 타 공지의 upload_file.id 를 심어 그 이미지를 자기 공지로 탈취할 수 있다(IDOR). 이를 차단한다.
     *
     * ## cleanup 대상 (동시 편집 간섭 차단)
     * 삭제 후보 = 최종 본문 refid 에 없는 INLINE 이미지 중 다음 하나:
     *  - sessionUploadedRefids 에 포함(이번 편집 세션에서 올렸다가 최종 본문에서 뺀 것) — 신규 작성 orphan 포함
     *  - parent_id=noticeId (이 공지에 이미 소속된 것 — 수정 시 기존 본문에서 뺀 것). 소유가 확실.
     * 세션 목록에도 없고 이 공지 소속도 아닌 parent_id=null 파일은 **타 세션 미저장분일 수 있어 건드리지 않는다**.
     * (프론트가 sessionUploadedRefids 를 미전송하면 세션 기반 정리는 생략되고 parent_id=noticeId 정리만 수행 — 하위호환.)
     */
    private fun syncInlineImages(noticeId: Long, content: String?, sessionUploadedRefids: List<String>?) {
        val html = content ?: ""
        val keptRefids = NoticeImagePlaceholder.extractRefids(html)
            .mapNotNull { it.toLongOrNull() }.toSet()

        // 본문이 참조 중인 uniqueKey 집합. 수정 화면은 data-refid 를 잃은 presigned `<img>` 를 그대로 저장 본문에
        // 담아 보낼 수 있어(웹 에디터가 파싱 시 data-refid 소실), refid 만으로는 "본문에 살아있는 이미지" 를
        // 판별하지 못한다. presigned URL 에 내재된 불변 uniqueKey 로 이 공지 소속 파일과 매칭해 보존 대상을 보강한다.
        // (presigned URL 자체는 저장/식별자로 쓰지 않고, URL 에서 뽑은 uniqueKey 만 매칭에 사용.)
        val keptUniqueKeys = NoticeImagePlaceholder.extractUniqueKeys(html).toSet()

        // (1) backfill — 본문에 남아있는 refid 중 미소속 임시 업로드분을 이 공지로 연결.
        if (keptRefids.isNotEmpty()) {
            uploadFileRepository.findByIdInAndParentTypeAndIsDeletedFalse(keptRefids.toList(), UploadFileParentTypes.NOTICE)
                .filter { it.uploadKbn == UPLOAD_KBN_INLINE && it.parentId == null }
                .forEach { it.parentId = noticeId }
        }

        // (2) cleanup — 정리 후보 수집 (세션 업로드분 ∪ 이 공지 소속분) 후 본문에 없는 것만 삭제.
        val sessionIds = sessionUploadedRefids.orEmpty().mapNotNull { it.toLongOrNull() }.toSet()
        val candidates = buildList {
            if (sessionIds.isNotEmpty()) {
                addAll(uploadFileRepository.findByIdInAndParentTypeAndIsDeletedFalse(sessionIds.toList(), UploadFileParentTypes.NOTICE))
            }
            addAll(uploadFileRepository.findByParentTypeAndParentIdAndIsDeletedFalse(UploadFileParentTypes.NOTICE, noticeId))
        }.distinctBy { it.id }

        // 보존 판정: 본문이 refid 로 참조 OR uniqueKey 로 참조하면 삭제하지 않는다.
        // (수정 시 이미지를 건드리지 않았는데 presigned src 로만 남은 기존 이미지가 오삭제되던 문제 방지.)
        candidates
            .filter { it.uploadKbn == UPLOAD_KBN_INLINE }
            .filter { it.id !in keptRefids && (it.uniqueKey.isNullOrBlank() || it.uniqueKey !in keptUniqueKeys) }
            .filter { it.parentId == noticeId || it.id in sessionIds }
            .forEach { file ->
                file.uniqueKey?.takeIf { it.isNotBlank() }?.let { storageService.deletePrivate(it) }
                file.isDeleted = true
            }
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

    fun getNoticeFormMeta(principal: WebUserPrincipal): NoticeFormMetaResponse {
        val scopes = NoticeScope.entries.map {
            ScopeOption(code = it.displayName, name = it.displayName)
        }

        // 조장/지점장은 지점공지만 작성 가능 → 카테고리 옵션도 지점공지(BRANCH)만 노출한다.
        // 프론트는 내려온 옵션을 그대로 렌더링하므로 UI 제한이 서버 권위로 통일된다.
        // 교육(EDUCATION)은 별도 '교육' 메뉴에서 관리하므로 공지사항 작성 카테고리에서는 제외한다.
        val visibleCategories = if (isBranchNoticeOnlyRole(principal.role)) {
            listOf(NoticeCategory.BRANCH)
        } else {
            NoticeCategory.entries.filter { it != NoticeCategory.EDUCATION }
        }
        val categories = visibleCategories.map {
            CategoryOption(code = it.apiCode, name = it.displayName)
        }

        // 지점공지 선택 지점 옵션 — 행사마스터/여사원일정과 동일한 WomenScheduleBranchResolver 권한별 화이트리스트.
        // 전사 권한자는 다중 지점, 조장/지점장은 본인 지점 단일(프론트가 단일 시 고정 표시). costCenterCode(=OrgCode) 차원.
        val branches = womenScheduleBranchResolver.resolveBranches(principal)
            .map { BranchOption(branchCode = it.branchCode, branchName = it.branchName) }

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
     * 지점공지(BRANCH) 저장 시 (지점명, 지점코드) 를 작성자가 고른 지점코드에서 해석한다.
     * - 회사공지/교육: (null, null)
     * - 지점공지: 요청 branchCode 필수(미보유 시 BranchRequiredException). 그 코드가 작성자 권한 스코프
     *   (WomenScheduleBranchResolver 화이트리스트) 안에 있어야 하며(아니면 BranchNotAllowedException/IDOR 차단),
     *   지점명은 화이트리스트에서 코드로 매칭한다.
     */
    private fun resolveSelectedBranch(
        category: NoticeCategory,
        principal: WebUserPrincipal,
        requestedBranchCode: String?
    ): Pair<String?, String?> {
        if (category != NoticeCategory.BRANCH) return null to null

        val branchCode = requestedBranchCode?.takeIf { it.isNotBlank() } ?: throw BranchRequiredException()

        val allowed = womenScheduleBranchResolver.resolveBranches(principal)
        val match = allowed.firstOrNull { it.branchCode == branchCode } ?: throw BranchNotAllowedException()
        return match.branchName to match.branchCode
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
