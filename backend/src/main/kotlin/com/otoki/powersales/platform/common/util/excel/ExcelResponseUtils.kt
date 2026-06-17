package com.otoki.powersales.platform.common.util.excel

import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

/**
 * 엑셀 다운로드 응답 빌더 — Content-Type + UTF-8 파일명(Content-Disposition) 헤더를 통일한다.
 *
 * 파일명은 RFC 5987 `filename*=UTF-8''` 형식으로 인코딩 (한글 파일명 깨짐 방지).
 * 기존 컨트롤러들이 각자 반복하던 URLEncoder boilerplate 를 한 곳으로 통합.
 */
object ExcelResponseUtils {

    fun build(result: ExcelResult): ResponseEntity<ByteArray> =
        build(result.bytes, result.filename)

    fun build(bytes: ByteArray, filename: String): ResponseEntity<ByteArray> {
        val headers = HttpHeaders()
        headers.contentType = MediaType.parseMediaType(XLSX_CONTENT_TYPE)
        val encoded = URLEncoder.encode(filename, StandardCharsets.UTF_8.toString()).replace("+", "%20")
        headers.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''$encoded")
        return ResponseEntity.ok().headers(headers).body(bytes)
    }
}
