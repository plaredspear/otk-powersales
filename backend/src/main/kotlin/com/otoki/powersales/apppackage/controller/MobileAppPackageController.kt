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

    /** 요청 기준 origin (scheme + host[:port]). X-Forwarded-* 가 반영된 절대 URL 의 base 를 추출. */
    private fun currentBaseUrl(): String =
        ServletUriComponentsBuilder.fromCurrentContextPath().build().toUriString()
}
