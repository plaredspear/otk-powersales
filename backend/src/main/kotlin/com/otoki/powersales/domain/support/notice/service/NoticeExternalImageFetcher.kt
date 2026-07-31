package com.otoki.powersales.domain.support.notice.service

import com.otoki.powersales.platform.common.storage.StorageConstants
import org.slf4j.LoggerFactory
import org.springframework.http.client.SimpleClientHttpRequestFactory
import org.springframework.stereotype.Component
import java.net.InetAddress
import java.net.URI
import java.net.UnknownHostException
import java.time.Duration

/** 외부 이미지 1건을 내려받은 결과. */
data class FetchedImage(
    val bytes: ByteArray,
    val contentType: String,
    /** 원본 URL 의 파일명(확장자 판별/표시용). 없으면 "external". */
    val fileName: String,
)

/**
 * 공지 본문에 붙여넣기로 들어온 **외부 이미지 URL** 을 내려받는다 (저장 시 S3 이관용).
 *
 * 웹 페이지에서 이미지를 복사해 붙여넣으면 에디터에는 그 사이트의 URL 이 그대로 박힌다. 그대로 저장하면
 * (1) 원본 사이트가 링크를 바꾸거나 내리면 깨지고 (2) 사내망/로그인 필요 이미지는 모바일 앱에서 아예
 * 안 보이며 (3) 열람 때마다 외부로 요청이 나간다. 그래서 저장 시점에 서버가 받아 S3 로 옮긴다
 * ([NoticeService.normalizeInlineExternalImages]).
 *
 * ## 안전장치 (서버가 임의 URL 을 호출하므로 SSRF 방어가 필수)
 * - **스킴**: http/https 만. `file:`/`blob:`/그 외는 거부 (서버 로컬 파일 접근 차단).
 * - **대상 IP**: DNS 해석 결과가 loopback/사설망/링크로컬(169.254.x = 클라우드 메타데이터)/멀티캐스트/
 *   와일드카드면 거부 — 내부망·인스턴스 메타데이터 탈취 경로를 막는다.
 * - **크기**: [StorageConstants.MAX_FILE_BYTES] 초과 시 중단 (스트림을 상한+1 까지만 읽어 메모리 폭주 방지).
 * - **타입**: 응답 Content-Type 이 이미지 허용 목록에 없으면 거부 (HTML 오류 페이지 등 혼입 차단).
 * - **시간**: 연결 3초 / 읽기 5초 — 저장 요청이 외부 지연에 묶이지 않게 한다.
 *
 * 실패는 예외를 던지지 않고 `null` 을 반환한다 — 이미지 하나 때문에 공지 저장 자체가 실패하면 안 되므로,
 * 호출부가 원본 태그를 보존하고 경고 로그만 남긴다.
 */
@Component
class NoticeExternalImageFetcher {

    companion object {
        private val log = LoggerFactory.getLogger(NoticeExternalImageFetcher::class.java)

        private const val CONNECT_TIMEOUT_MS = 3_000L
        private const val READ_TIMEOUT_MS = 5_000L

        /** 본문 인라인 이미지로 허용할 content-type (공용 허용 목록 ∩ 이미지). */
        private val IMAGE_CONTENT_TYPES: Set<String> =
            StorageConstants.ALLOWED_CONTENT_TYPES.filter { it.startsWith("image/") }.toSet()

        private const val DEFAULT_FILE_NAME = "external"
    }

    private val requestFactory = SimpleClientHttpRequestFactory().apply {
        setConnectTimeout(Duration.ofMillis(CONNECT_TIMEOUT_MS))
        setReadTimeout(Duration.ofMillis(READ_TIMEOUT_MS))
    }

    /**
     * 외부 이미지 URL 을 내려받는다. 위 안전장치 중 하나라도 걸리면 `null`.
     *
     * RestClient 대신 요청 팩토리를 직접 쓰는 이유: 응답 바이트를 상한까지만 읽어 중단해야 하는데
     * (Content-Length 를 신뢰할 수 없다) RestClient 의 body 변환은 전량 로딩이 기본이기 때문이다.
     */
    fun fetch(url: String): FetchedImage? {
        val uri = parseSafeUri(url) ?: return null

        return try {
            val request = requestFactory.createRequest(uri, org.springframework.http.HttpMethod.GET)
            request.execute().use { response ->
                if (!response.statusCode.is2xxSuccessful) {
                    log.warn("외부 이미지 응답 실패 — url={} status={}", url, response.statusCode)
                    return null
                }
                val contentType = response.headers.contentType?.let { "${it.type}/${it.subtype}" }?.lowercase()
                if (contentType == null || contentType !in IMAGE_CONTENT_TYPES) {
                    log.warn("외부 이미지 content-type 거부 — url={} contentType={}", url, contentType)
                    return null
                }
                val bytes = readAtMost(response.body, StorageConstants.MAX_FILE_BYTES) ?: run {
                    log.warn("외부 이미지 용량 초과({}B 초과) — url={}", StorageConstants.MAX_FILE_BYTES, url)
                    return null
                }
                if (bytes.isEmpty()) {
                    log.warn("외부 이미지 본문이 비어 있음 — url={}", url)
                    return null
                }
                FetchedImage(bytes = bytes, contentType = contentType, fileName = fileNameOf(uri))
            }
        } catch (ex: Exception) {
            // 타임아웃/DNS 실패/TLS 오류 등 — 공지 저장을 막지 않는다.
            log.warn("외부 이미지 다운로드 실패 — url={} ({})", url, ex.javaClass.simpleName)
            null
        }
    }

    /** http/https + 공인 IP 대상만 통과. 그 외(사설망/메타데이터/로컬 스킴)는 null. */
    private fun parseSafeUri(url: String): URI? {
        val uri = try {
            URI(url)
        } catch (_: Exception) {
            return null
        }
        val scheme = uri.scheme?.lowercase()
        if (scheme != "http" && scheme != "https") return null
        val host = uri.host ?: return null

        val addresses = try {
            InetAddress.getAllByName(host)
        } catch (_: UnknownHostException) {
            log.warn("외부 이미지 호스트 해석 실패 — host={}", host)
            return null
        }
        // 하나라도 내부 대역이면 거부 (DNS rebinding 여지를 줄이기 위해 전부 검사).
        if (addresses.any { it.isLoopbackAddress || it.isSiteLocalAddress || it.isLinkLocalAddress || it.isAnyLocalAddress || it.isMulticastAddress }) {
            log.warn("외부 이미지 대상이 내부 주소라 거부 — host={}", host)
            return null
        }
        return uri
    }

    /** 상한을 넘으면 즉시 중단하고 null. (Content-Length 헤더를 신뢰하지 않는다.) */
    private fun readAtMost(input: java.io.InputStream, limit: Long): ByteArray? {
        val buffer = java.io.ByteArrayOutputStream()
        val chunk = ByteArray(8 * 1024)
        var total = 0L
        while (true) {
            val read = input.read(chunk)
            if (read < 0) break
            total += read
            if (total > limit) return null
            buffer.write(chunk, 0, read)
        }
        return buffer.toByteArray()
    }

    private fun fileNameOf(uri: URI): String =
        uri.path?.substringAfterLast('/')?.takeIf { it.isNotBlank() } ?: DEFAULT_FILE_NAME
}
