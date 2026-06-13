package com.otoki.powersales.platform.apppackage.service

import org.springframework.stereotype.Component

/**
 * iOS itms-services OTA 설치용 manifest.plist XML 빌더.
 *
 * S3 에 저장하지 않고 설치 요청 시점에 IPA presigned URL 을 주입해 즉석 생성한다.
 * presigned URL 의 `&` 등 특수문자는 반드시 XML 이스케이프해야 iOS 가 파싱한다(누락 시 설치 실패).
 */
@Component
class ManifestPlistBuilder {

    /**
     * @param ipaUrl IPA presigned 다운로드 URL (XML 이스케이프 전 raw)
     * @param bundleIdentifier 서명된 IPA 의 실제 bundle id 와 일치해야 함
     * @param bundleVersion 표시용 버전 (version_name)
     * @param title 설치 중 표시 제목
     */
    fun build(ipaUrl: String, bundleIdentifier: String, bundleVersion: String, title: String): String {
        return """<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
<plist version="1.0">
<dict>
  <key>items</key>
  <array>
    <dict>
      <key>assets</key>
      <array>
        <dict>
          <key>kind</key><string>software-package</string>
          <key>url</key><string>${escapeXml(ipaUrl)}</string>
        </dict>
      </array>
      <key>metadata</key>
      <dict>
        <key>bundle-identifier</key><string>${escapeXml(bundleIdentifier)}</string>
        <key>bundle-version</key><string>${escapeXml(bundleVersion)}</string>
        <key>kind</key><string>software</string>
        <key>title</key><string>${escapeXml(title)}</string>
      </dict>
    </dict>
  </array>
</dict>
</plist>
"""
    }

    private fun escapeXml(raw: String): String = raw
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
        .replace("'", "&apos;")
}
