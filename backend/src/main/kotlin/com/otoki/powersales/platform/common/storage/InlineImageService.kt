package com.otoki.powersales.platform.common.storage

import com.otoki.powersales.platform.common.entity.UploadFile
import com.otoki.powersales.platform.common.repository.UploadFileRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile
import java.util.Base64

/**
 * 리치텍스트 본문 인라인 이미지의 업로드 / 저장 시 정규화 / 조회 시 rewrite / 고아 정리를 담당한다.
 * 공지([UploadFileParentTypes.NOTICE]) 와 교육([UploadFileParentTypes.EDUCATION_POST]) 이 공유한다.
 *
 * 도메인별로 다른 것은 [InlineImageDomain] 세 값(parentType / S3 domain / presigned TTL) 뿐이고 나머지
 * 규칙 — placeholder 형식, 정규화 3단계, backfill IDOR 차단, cleanup 보존 판정 — 은 완전히 동일하다.
 * 이 규칙들은 각각 실제 사고를 겪고 다듬어진 것이라(하단 각 메소드 KDoc 참조) 도메인마다 복제하면
 * 한쪽만 고쳐지는 형태로 재발한다. 그래서 로직을 여기 한 곳에 두고 호출부는 도메인 값만 넘긴다.
 *
 * ## 저장/조회 계약 (호출부가 지켜야 하는 순서)
 * - **저장**: 부모 row 를 먼저 저장해 id 를 확보한 뒤 [normalizeContent] → 그 결과를 본문에 반영 →
 *   [syncInlineImages]. 신규 등록도 마찬가지다 (업로드 시점엔 부모 id 가 없어 parent_id=null 로 떠 있다가
 *   backfill 로 연결되기 때문).
 * - **조회**: [rewriteInlineImages] 로 placeholder → presigned 변환. DB 에는 **절대** presigned 를 저장하지 않는다.
 *
 * 호출부는 자신의 `@Transactional` 경계 안에서 부른다 — 본 서비스의 메소드는 별도 tx 를 열지 않는다
 * ([uploadInlineImage] 만 단독 호출이라 자체 tx).
 */
@Service
class InlineImageService(
    private val uploadFileRepository: UploadFileRepository,
    private val storageService: StorageService,
    private val externalImageFetcher: ExternalImageFetcher,
) {

    companion object {
        private val log = LoggerFactory.getLogger(InlineImageService::class.java)

        /** 본문 인라인 이미지 식별자 (upload_file.upload_kbn). 첨부 목록과 구분하기 위함. */
        const val UPLOAD_KBN_INLINE = "INLINE"

        /** base64 / 외부 이미지 정규화 시 업로드 파일명(확장자 파생용). 실제 파일명 정보가 없어 고정 stem. */
        private const val INLINE_UPLOAD_STEM = "inline"
    }

    /**
     * 인라인 이미지 1건 업로드 (웹 에디터 툴바 / 드래그앤드롭 / 붙여넣기).
     *
     * 첨부 업로드와 달리 **parent_id 를 null 로 저장한다** — 신규 작성 화면에서는 부모(공지/교육) id 가
     * 아직 없기 때문이다. 본문 저장 시 [syncInlineImages] 의 backfill 이 채운다. (수정 화면은 부모 id 가
     * 있어도 일관성을 위해 동일 경로를 탄다.)
     *
     * refid = upload_file.id (Long). 마이그레이션분(refid=sfid)과 충돌하지 않으며 [rewriteInlineImages] 는
     * 두 키를 모두 매칭한다.
     */
    @Transactional
    fun uploadInlineImage(domain: InlineImageDomain, file: MultipartFile): InlineImageUploadResult {
        val contentType = file.contentType
            ?: throw IllegalArgumentException("파일 타입을 확인할 수 없습니다")
        val result = storageService.uploadPrivate(
            domain = domain.s3Domain,
            originalName = file.originalFilename ?: "unknown",
            bytes = file.bytes,
            contentType = contentType,
        )
        val saved = uploadFileRepository.save(
            UploadFile(
                name = file.originalFilename,
                uniqueKey = result.key,
                fileSize = formatFileSize(file.size),
                parentType = domain.parentType,
                parentId = null,
                uploadKbn = UPLOAD_KBN_INLINE,
                isDeleted = false,
            )
        )
        val refid = saved.id.toString()
        return InlineImageUploadResult(
            refid = refid,
            placeholder = InlineImagePlaceholder.build(refid, file.originalFilename ?: ""),
            previewUrl = storageService.getPresignedUrl(saved.uniqueKey!!, domain.presignTtlSeconds),
        )
    }

    /**
     * 저장 직전 본문 정규화 — 어떤 경로로 들어온 이미지든 본문에는 placeholder 만 남게 만든다.
     *
     * 클라이언트(웹 에디터)도 저장 전에 같은 변환을 시도하지만, 그 복원은 여러 이유로 실패할 수 있다
     * (에디터가 `data-refid` 를 파싱 단계에서 버림, 속성값 `&` → `&amp;` 이스케이프로 URL 매칭이 어긋남 등).
     * 클라이언트 복원에 의존하지 않도록 **서버가 저장 시점에 다시 정규화**한다 (이중 방어).
     *
     * 순서가 의미를 갖는다: base64 → presigned → 외부 URL. presigned 를 먼저 되돌려야 마지막 외부 URL
     * 단계가 우리 private 객체를 "외부 이미지" 로 오인해 다시 내려받지 않는다.
     */
    fun normalizeContent(domain: InlineImageDomain, parentId: Long, html: String?): String? {
        var result = normalizeInlineBase64Images(domain, parentId, html)
        result = normalizeInlinePresignedImages(domain, parentId, result)
        return normalizeInlineExternalImages(domain, parentId, result)
    }

    /**
     * 조회 시 placeholder(`<img data-refid="{refid}">`)의 src 를 presigned URL 로 교체한다.
     *
     * `data-refid` 는 보존한다 — 모바일이 cacheKey 로 쓰기 때문(presigned 는 매 조회 바뀌므로 URL 을
     * 캐시 키로 쓰면 캐시가 전혀 재사용되지 않는다).
     * 매칭되지 않는 refid(삭제분 등)는 placeholder 를 유지해 깨진 아이콘만 노출하고 본문을 오염시키지 않는다.
     *
     * [uploadFiles] 는 호출부가 부모 1건당 1회 조회해 넘긴다 (목록에서 N+1 을 만들지 않기 위함).
     */
    fun rewriteInlineImages(domain: InlineImageDomain, html: String, uploadFiles: List<UploadFile>): String {
        if (!html.contains("data-refid")) return html

        // refid → uniqueKey. 마이그레이션분은 refid=sfid, 신규 업로드분은 refid=id 라 두 키를 함께 등록한다
        // (sfid 는 18자 영숫자, id 는 숫자라 충돌 없음).
        val uniqueKeyByRefid: Map<String, String> = buildMap {
            uploadFiles.forEach { file ->
                val key = file.uniqueKey?.takeIf { it.isNotBlank() } ?: return@forEach
                file.sfid?.takeIf { it.isNotBlank() }?.let { put(it, key) }
                put(file.id.toString(), key)
            }
        }

        return InlineImagePlaceholder.PLACEHOLDER_IMG_REGEX.replace(html) { match ->
            val refid = match.groupValues[1]
            val uniqueKey = uniqueKeyByRefid[refid] ?: return@replace match.value
            val presigned = storageService.getPresignedUrl(uniqueKey, domain.presignTtlSeconds)
            // src 속성만 presigned 로 교체 (data-refid 보존). presigned URL 은 &, =, %, $ 등을 포함하므로
            // 람다 기반 replace 로 치환 문자열을 literal 로 넣어 그룹 참조($1 등) 오해석을 방지한다.
            InlineImagePlaceholder.SRC_ATTR_REGEX.replace(match.value) { "src=\"$presigned\"" }
        }
    }

    /**
     * 본문 저장 시 본문이 참조하는 인라인 이미지와 실제 upload_file 을 동기화한다.
     * (1) backfill: 본문 refid 중 parent_id=null 인 임시 INLINE 업로드분을 이 부모로 소속시킨다.
     * (2) cleanup: 본문에서 빠진(사용자가 삽입 후 삭제한) INLINE 이미지를 S3+soft-delete 로 정리한다.
     *
     * ## backfill 보안 (IDOR)
     * backfill 대상은 **아직 부모가 없는(parent_id=null) 임시 INLINE 업로드분**으로만 한정한다.
     * refid 는 클라이언트가 보낸 본문 HTML 에서 추출되므로, 이미 다른 글에 소속된 파일(parent_id != null)을
     * 무차별 재부모화하면 본문에 타인의 upload_file.id 를 심어 그 이미지를 자기 글로 탈취할 수 있다(IDOR).
     *
     * ## cleanup 대상 (동시 편집 간섭 차단)
     * 삭제 후보 = 최종 본문 refid 에 없는 INLINE 이미지 중 다음 하나:
     *  - [sessionUploadedRefids] 에 포함(이번 편집 세션에서 올렸다가 최종 본문에서 뺀 것) — 신규 작성 orphan 포함
     *  - parent_id=parentId (이 글에 이미 소속된 것 — 수정 시 기존 본문에서 뺀 것). 소유가 확실.
     * 세션 목록에도 없고 이 글 소속도 아닌 parent_id=null 파일은 **타 세션 미저장분일 수 있어 건드리지 않는다**.
     * (프론트가 sessionUploadedRefids 를 미전송하면 세션 기반 정리는 생략되고 parent_id 정리만 수행 — 하위호환.)
     */
    fun syncInlineImages(
        domain: InlineImageDomain,
        parentId: Long,
        content: String?,
        sessionUploadedRefids: List<String>?,
    ) {
        val html = content ?: ""
        val keptRefids = InlineImagePlaceholder.extractRefids(html)
            .mapNotNull { it.toLongOrNull() }.toSet()

        // 본문이 참조 중인 uniqueKey 집합. 수정 화면은 data-refid 를 잃은 presigned `<img>` 를 그대로 저장 본문에
        // 담아 보낼 수 있어(웹 에디터가 파싱 시 data-refid 소실), refid 만으로는 "본문에 살아있는 이미지" 를
        // 판별하지 못한다. presigned URL 에 내재된 불변 uniqueKey 로 소속 파일과 매칭해 보존 대상을 보강한다.
        val keptUniqueKeys = InlineImagePlaceholder.extractUniqueKeys(html).toSet()

        // (1) backfill — 본문에 남아있는 refid 중 미소속 임시 업로드분을 이 글로 연결.
        if (keptRefids.isNotEmpty()) {
            uploadFileRepository
                .findByIdInAndParentTypeAndIsDeletedFalse(keptRefids.toList(), domain.parentType)
                .filter { it.uploadKbn == UPLOAD_KBN_INLINE && it.parentId == null }
                .forEach { it.parentId = parentId }
        }

        // (2) cleanup — 정리 후보 수집 (세션 업로드분 ∪ 이 글 소속분) 후 본문에 없는 것만 삭제.
        val sessionIds = sessionUploadedRefids.orEmpty().mapNotNull { it.toLongOrNull() }.toSet()
        val candidates = buildList {
            if (sessionIds.isNotEmpty()) {
                addAll(uploadFileRepository.findByIdInAndParentTypeAndIsDeletedFalse(sessionIds.toList(), domain.parentType))
            }
            addAll(uploadFileRepository.findByParentTypeAndParentIdAndIsDeletedFalse(domain.parentType, parentId))
        }.distinctBy { it.id }

        // 보존 판정: 본문이 refid 로 참조 OR uniqueKey 로 참조하면 삭제하지 않는다.
        // (수정 시 이미지를 건드리지 않았는데 presigned src 로만 남은 기존 이미지가 오삭제되던 문제 방지.)
        candidates
            .filter { it.uploadKbn == UPLOAD_KBN_INLINE }
            .filter { it.id !in keptRefids && (it.uniqueKey.isNullOrBlank() || it.uniqueKey !in keptUniqueKeys) }
            .filter { it.parentId == parentId || it.id in sessionIds }
            .forEach { file ->
                file.uniqueKey?.takeIf { it.isNotBlank() }?.let { storageService.deletePrivate(it) }
                file.isDeleted = true
            }
    }

    /**
     * 본문에 base64 data URI 로 박혀 들어온 인라인 이미지(`<img src="data:image/...;base64,...">`)를
     * private S3 로 업로드하고 placeholder 로 치환한다.
     *
     * 웹 에디터에 이미지를 '붙여넣기' 하면 정상 업로드 경로([uploadInlineImage])를 타지 않고 base64 가 본문에
     * 그대로 삽입될 수 있다. 이 경우 (1) DB 본문이 비대해지고 (2) 모바일은 http 가 아닌 src 를 렌더하지 못해
     * 이미지가 깨진다. 저장 시점에 정규화하여 어떤 클라이언트/경로로 들어와도 본문에는 placeholder 만 남게 한다.
     *
     * 업로드분은 즉시 이 글 소속(parentId, upload_kbn=INLINE)으로 생성되므로 이어지는 [syncInlineImages] 의
     * cleanup 대상에서 자연히 보존된다(본문이 그 refid 를 참조).
     * 허용 외 content-type/디코드 실패분은 원본 태그를 보존한다(placeholder 미치환).
     */
    private fun normalizeInlineBase64Images(domain: InlineImageDomain, parentId: Long, html: String?): String? {
        if (html.isNullOrEmpty() || !html.contains("data:image", ignoreCase = true)) return html
        return InlineImagePlaceholder.DATA_URI_IMG_REGEX.replace(html) { match ->
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
                domain = domain.s3Domain,
                originalName = fileName,
                bytes = bytes,
                contentType = contentType,
            )
            val saved = uploadFileRepository.save(
                UploadFile(
                    name = fileName,
                    uniqueKey = result.key,
                    fileSize = formatFileSize(bytes.size.toLong()),
                    parentType = domain.parentType,
                    parentId = parentId,
                    uploadKbn = UPLOAD_KBN_INLINE,
                    isDeleted = false,
                )
            )
            InlineImagePlaceholder.build(saved.id.toString(), "")
        }
    }

    /**
     * 본문에 presigned URL 로 박혀 들어온 인라인 이미지(`<img src="https://.../private/{uniqueKey}?X-Amz-...">`)를
     * placeholder 로 되돌린다.
     *
     * presigned URL 은 TTL 뒤 만료되는 임시값이라 본문에 저장되면 그 시점부터 이미지가 영구히 깨진다
     * (공지 2393 실제 사례). 웹 에디터가 저장 직전 placeholder 로 복원하지만, 에디터가 `data-refid` 를 버리거나
     * URL 이스케이프(`&` → `&amp;`)로 치환이 어긋나면 그 복원이 실패한다.
     *
     * 역추적 키는 URL 에 내재된 불변 uniqueKey(= `upload_file.unique_key`). 아직 부모가 없는(parent_id=null)
     * 임시 업로드분은 이 글로 소속시킨다. 다른 글 소속 파일은 소속을 바꾸지 않으며(IDOR), placeholder 로만
     * 바뀌어 조회 시 rewrite 대상에서 빠진다(= 남의 이미지가 노출되지 않는다).
     * upload_file 에 없는 URL(외부 링크 등)은 원본 태그를 보존한다.
     */
    private fun normalizeInlinePresignedImages(domain: InlineImageDomain, parentId: Long, html: String?): String? {
        if (html.isNullOrEmpty()) return html
        val uniqueKeys = InlineImagePlaceholder.extractUniqueKeys(html).toSet()
        if (uniqueKeys.isEmpty()) return html

        val filesByKey = uploadFileRepository
            .findByUniqueKeyInAndParentTypeAndIsDeletedFalse(uniqueKeys.toList(), domain.parentType)
            .associateBy { it.uniqueKey }

        return InlineImagePlaceholder.rewriteImgsByUniqueKey(html) { key ->
            val file = filesByKey[key]
            if (file == null) {
                // 되돌릴 근거(upload_file)가 없으면 만료되는 URL 이 본문에 그대로 남는다 = 이후 깨짐 확정.
                // 조용히 저장되면 재발을 알 수 없으므로 경고로 남겨 관측 가능하게 한다.
                log.warn(
                    "{} {} 본문의 인라인 이미지 uniqueKey={} 에 대응하는 upload_file 이 없어 placeholder 로 되돌리지 못했다 " +
                        "— 만료 URL 이 본문에 남아 이미지가 깨질 수 있다",
                    domain.parentType, parentId, key,
                )
                return@rewriteImgsByUniqueKey null
            }
            // 아직 부모가 없는 임시 업로드분만 이 글로 소속시킨다 (uploadKbn 은 업로드 시점에 INLINE 으로 고정).
            if (file.parentId == null) file.parentId = parentId
            InlineImagePlaceholder.build(file.id.toString(), "")
        }
    }

    /**
     * 붙여넣기로 본문에 박힌 **외부 이미지 URL** 을 S3 로 이관하고 placeholder 로 치환한다.
     *
     * 웹 페이지에서 이미지를 복사해 붙여넣으면 그 사이트의 URL 이 그대로 저장된다. 그대로 두면 원본이
     * 사라지는 순간 깨지고, 사내망/로그인 필요 이미지는 모바일 앱에서 처음부터 보이지 않으며, 열람 때마다
     * 외부로 요청이 나간다. 저장 시점에 서버가 받아([ExternalImageFetcher]) 우리 S3 로 옮긴다.
     *
     * - 대상: `<img src="http(s)://...">` 중 우리 private 객체가 아닌 것. (우리 presigned 는 앞 단계
     *   [normalizeInlinePresignedImages] 가 이미 placeholder 로 되돌렸다.)
     * - 다운로드 실패(내부망/타입 거부/용량 초과/타임아웃)는 원본 태그를 보존하고 경고만 남긴다 —
     *   이미지 하나 때문에 저장이 실패하면 안 되므로. 그 이미지는 종전처럼 외부 URL 로 남는다.
     * - `file:`/`blob:` 등 서버가 받아올 수 없는 스킴은 경고만 남긴다(웹이 저장 전에 차단한다).
     */
    private fun normalizeInlineExternalImages(domain: InlineImageDomain, parentId: Long, html: String?): String? {
        if (html.isNullOrEmpty() || !html.contains("<img", ignoreCase = true)) return html

        return InlineImagePlaceholder.rewriteImgsBySrc(html) { rawSrc ->
            val src = InlineImagePlaceholder.unescapeAttr(rawSrc)
            when {
                // 우리 private 객체 / placeholder 는 대상 아님.
                src.startsWith(InlineImagePlaceholder.SCHEME) -> null
                src.contains(InlineImagePlaceholder.PRIVATE_PATH_SEGMENT) -> null
                src.startsWith("http://", ignoreCase = true) || src.startsWith("https://", ignoreCase = true) ->
                    adoptExternalImage(domain, parentId, src)

                else -> {
                    // data: 는 앞 단계에서 처리됐고, 남은 건 file:/blob: 처럼 서버가 받아올 수 없는 참조다.
                    log.warn(
                        "{} {} 본문에 가져올 수 없는 이미지 참조가 있다 — src prefix={}",
                        domain.parentType, parentId, src.take(12),
                    )
                    null
                }
            }
        }
    }

    /** 외부 이미지 1건을 내려받아 S3 + upload_file 로 적재하고 placeholder 태그를 돌려준다. 실패 시 null. */
    private fun adoptExternalImage(domain: InlineImageDomain, parentId: Long, src: String): String? {
        val fetched = externalImageFetcher.fetch(src) ?: return null

        val fileName = "$INLINE_UPLOAD_STEM.${extensionForContentType(fetched.contentType)}"
        val result = storageService.uploadPrivate(
            domain = domain.s3Domain,
            originalName = fileName,
            bytes = fetched.bytes,
            contentType = fetched.contentType,
        )
        val saved = uploadFileRepository.save(
            UploadFile(
                name = fetched.fileName,
                uniqueKey = result.key,
                fileSize = formatFileSize(fetched.bytes.size.toLong()),
                parentType = domain.parentType,
                parentId = parentId,
                uploadKbn = UPLOAD_KBN_INLINE,
                isDeleted = false,
            )
        )
        log.info("{} {} 외부 이미지 S3 이관 — {} → {}", domain.parentType, parentId, src.take(120), result.key)
        return InlineImagePlaceholder.build(saved.id.toString(), "")
    }

    private fun extensionForContentType(contentType: String): String = when (contentType) {
        "image/png" -> "png"
        "image/jpeg", "image/jpg" -> "jpg"
        "image/gif" -> "gif"
        "image/webp" -> "webp"
        "image/heic" -> "heic"
        else -> "img"
    }

    private fun formatFileSize(bytes: Long): String = when {
        bytes < 1024L -> "$bytes B"
        bytes < 1024L * 1024L -> "${bytes / 1024L} KB"
        bytes < 1024L * 1024L * 1024L -> "${bytes / (1024L * 1024L)} MB"
        else -> "${bytes / (1024L * 1024L * 1024L)} GB"
    }
}

/**
 * 인라인 이미지를 쓰는 도메인이 [InlineImageService] 에 넘기는 값. 도메인 간 차이는 이 세 값이 전부다.
 *
 * [parentType] 이 도메인 격리 축이다 — 조회/backfill/cleanup 이 모두 이 값으로 필터하므로, 교육 본문에
 * 공지 이미지의 refid 를 심어도 조회 맵에 잡히지 않아 노출되지 않는다.
 */
enum class InlineImageDomain(
    /** [UploadFileParentTypes] 값. upload_file.parent_type 에 저장된다. */
    val parentType: String,
    /** S3 private key 의 도메인 세그먼트 — 실제 key = `private/uploads/{s3Domain}/yyyy/mm/dd/uuid.ext`. */
    val s3Domain: String,
    /** 조회 시 발급하는 presigned URL 의 만료 시간(초). */
    val presignTtlSeconds: Int,
) {
    NOTICE(UploadFileParentTypes.NOTICE, "notice", StorageConstants.NOTICE_PRESIGN_TTL_SECONDS),
    EDUCATION(UploadFileParentTypes.EDUCATION_POST, "education-inline", StorageConstants.EDUCATION_PRESIGN_TTL_SECONDS),
}

/** 인라인 이미지 업로드 결과. 클라이언트는 본문엔 [placeholder] 를, 에디터엔 [previewUrl] 을 쓴다. */
data class InlineImageUploadResult(
    /** upload_file.id 문자열. 본문 `data-refid` 이자 세션 정리 목록의 키. */
    val refid: String,
    /** 본문에 저장될 `<img src="notice-image://{refid}" data-refid="{refid}">` 태그. */
    val placeholder: String,
    /** 에디터 즉시 미리보기용 presigned URL. **본문에 저장 금지** (만료됨). */
    val previewUrl: String,
)
