package com.otoki.powersales.platform.apppackage.controller

import com.otoki.powersales.platform.apppackage.dto.AppPackageDownloadUrlDto
import com.otoki.powersales.platform.apppackage.dto.AppVersionCheckDto
import com.otoki.powersales.platform.apppackage.entity.AppPlatform
import com.otoki.powersales.platform.common.dto.ApiResponse
import com.otoki.powersales.platform.apppackage.service.MobileAppPackageService
import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.servlet.support.ServletUriComponentsBuilder

/**
 * 모바일 앱 패키지 다운로드/버전 체크.
 *
 * 강제 업데이트는 로그인 전(미인증) 게이트여야 하므로 app-package 경로는 SecurityConfig 에서 permitAll.
 */
@RestController
@RequestMapping("/api/v1/mobile/app-package")
class MobileAppPackageController(
    private val mobileAppPackageService: MobileAppPackageService,
) {

    @GetMapping("/check")
    fun check(
        @RequestParam platform: AppPlatform,
        @RequestParam versionCode: Long,
        request: HttpServletRequest,
    ): ResponseEntity<ApiResponse<AppVersionCheckDto>> {
        val result = mobileAppPackageService.check(platform, versionCode, currentBaseUrl(request))
        return ResponseEntity.ok(ApiResponse.success(result))
    }

    @GetMapping("/{id}/download-url")
    fun downloadUrl(@PathVariable id: Long): ResponseEntity<ApiResponse<AppPackageDownloadUrlDto>> {
        return ResponseEntity.ok(ApiResponse.success(mobileAppPackageService.issueDownloadUrl(id)))
    }

    /** iOS itms-services manifest.plist 동적 생성. Content-Type application/xml. */
    @GetMapping("/ios/manifest.plist", produces = [MediaType.APPLICATION_XML_VALUE])
    fun iosManifest(@RequestParam id: Long): ResponseEntity<String> {
        val xml = mobileAppPackageService.buildIosManifest(id)
        return ResponseEntity.ok()
            .contentType(MediaType.APPLICATION_XML)
            .body(xml)
    }

    /** iOS manifest.plist (항상 최신 isLatest 버전). 고정 설치 페이지가 fetch 한다. */
    @GetMapping("/ios/manifest.plist/latest", produces = [MediaType.APPLICATION_XML_VALUE])
    fun iosLatestManifest(): ResponseEntity<String> {
        val xml = mobileAppPackageService.buildLatestIosManifest()
        return ResponseEntity.ok()
            .contentType(MediaType.APPLICATION_XML)
            .body(xml)
    }

    /**
     * iOS OTA 설치 안내 HTML 페이지(특정 버전). 공유 가능한 https URL — iPhone Safari 에서 열어
     * 페이지 내 "설치" 버튼으로 itms-services 를 호출한다.
     *
     * 한글이 포함되므로 charset=UTF-8 을 명시한다(누락 시 브라우저가 깨진 인코딩으로 표시).
     */
    @GetMapping("/ios/install", produces = ["text/html;charset=UTF-8"])
    fun iosInstallPage(@RequestParam id: Long, request: HttpServletRequest): ResponseEntity<String> {
        val html = mobileAppPackageService.buildIosInstallPage(id, currentBaseUrl(request))
        return htmlResponse(html)
    }

    /**
     * iOS OTA 설치 안내 페이지(항상 최신 isLatest 버전). **대규모 배포용 고정 링크** —
     * 새 버전 업로드 후 "최신 지정"만 하면 동일 URL 이 신버전을 가리킨다. 사번 전체에 1회 공지.
     */
    @GetMapping("/ios/install/latest", produces = ["text/html;charset=UTF-8"])
    fun iosLatestInstallPage(request: HttpServletRequest): ResponseEntity<String> {
        val html = mobileAppPackageService.buildLatestIosInstallPage(currentBaseUrl(request))
        return htmlResponse(html)
    }

    /**
     * Android 최신 APK 다운로드 (대규모 배포용 고정 링크). 최신 isLatest APK 의 presigned URL 로
     * 302 redirect 한다 — 영구 고정 링크 뒤에서 만료되는 presigned URL 을 매 요청 새로 발급.
     * 기기는 redirect 를 따라가 APK 를 직접 받아 설치한다(iOS 와 달리 안내 페이지 불요).
     */
    @GetMapping("/android/download/latest")
    fun androidLatestDownload(): ResponseEntity<Void> {
        val presignedUrl = mobileAppPackageService.issueLatestAndroidDownloadUrl()
        return ResponseEntity.status(HttpStatus.FOUND)
            .header(HttpHeaders.LOCATION, presignedUrl)
            .build()
    }

    private fun htmlResponse(html: String): ResponseEntity<String> =
        ResponseEntity.ok()
            .contentType(MediaType("text", "html", Charsets.UTF_8))
            .body(html)

    /**
     * 요청 기준 origin (scheme + host[:port]).
     *
     * TLS 가 ALB/nginx 에서 종료되어 Tomcat 은 http 로 받으므로 X-Forwarded-Proto 로 scheme 을
     * 보정한다 — 보정 없이는 itms-services manifest URL 이 http 로 합성되어 iOS 가 OTA 설치를
     * "인증서가 유효하지 않음" 오류로 거부한다. 전역 server.forward-headers-strategy 는
     * ForwardedHeaderFilter 가 X-Forwarded-For 를 제거해 SAP/SF 인바운드 IP allowlist
     * (ClientIpResolver) 를 깨뜨리므로 쓰지 않고, 본 컨트롤러 한정으로 보정한다.
     */
    private fun currentBaseUrl(request: HttpServletRequest): String {
        val builder = ServletUriComponentsBuilder.fromCurrentContextPath()
        request.getHeader("X-Forwarded-Proto")
            ?.split(",")?.first()?.trim()
            ?.takeIf { it.isNotEmpty() }
            ?.let { builder.scheme(it) }
        return builder.build().toUriString()
    }
}
