package com.otoki.powersales.apppackage.controller

import com.otoki.powersales.apppackage.dto.AppPackageDownloadUrlDto
import com.otoki.powersales.apppackage.dto.AppVersionCheckDto
import com.otoki.powersales.apppackage.entity.AppPlatform
import com.otoki.powersales.apppackage.service.MobileAppPackageService
import com.otoki.powersales.common.dto.ApiResponse
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
    ): ResponseEntity<ApiResponse<AppVersionCheckDto>> {
        val result = mobileAppPackageService.check(platform, versionCode, currentBaseUrl())
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

    /**
     * iOS OTA 설치 안내 HTML 페이지. 공유 가능한 https URL — iPhone Safari 에서 열어
     * 페이지 내 "설치" 버튼으로 itms-services 를 호출한다.
     *
     * 한글이 포함되므로 charset=UTF-8 을 명시한다(누락 시 브라우저가 깨진 인코딩으로 표시).
     */
    @GetMapping("/ios/install", produces = ["text/html;charset=UTF-8"])
    fun iosInstallPage(@RequestParam id: Long): ResponseEntity<String> {
        val html = mobileAppPackageService.buildIosInstallPage(id, currentBaseUrl())
        return ResponseEntity.ok()
            .contentType(MediaType("text", "html", Charsets.UTF_8))
            .body(html)
    }

    /** 요청 기준 origin (scheme + host[:port]). X-Forwarded-* 가 반영된 절대 URL 의 base 를 추출. */
    private fun currentBaseUrl(): String =
        ServletUriComponentsBuilder.fromCurrentContextPath().build().toUriString()
}
