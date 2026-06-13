package com.otoki.powersales.platform.apppackage.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("IosInstallPageBuilder 테스트")
class IosInstallPageBuilderTest {

    private val builder = IosInstallPageBuilder()

    @Test
    @DisplayName("itms-services href 에 manifest URL 이 인코딩되어 포함된다")
    fun installHrefContainsEncodedManifestUrl() {
        val manifestUrl = "https://app.example.com/api/v1/mobile/app-package/ios/manifest.plist?id=7"

        val html = builder.build(manifestUrl, "오뚜기 파워세일즈", "1.2.0")

        // itms-services 스킴 + download-manifest action + url 파라미터(인코딩) 포함
        assertThat(html).contains("itms-services://?action=download-manifest&url=")
        // manifest URL 의 ?/= 등이 퍼센트 인코딩되어 들어가야 한다 (raw 노출 금지)
        assertThat(html).contains("manifest.plist%3Fid%3D7")
        assertThat(html).doesNotContain("manifest.plist?id=7\"") // raw 형태로 href 에 노출되면 안 됨
    }

    @Test
    @DisplayName("title/version 이 HTML 본문에 표시되고 특수문자는 이스케이프된다")
    fun escapesTitleAndVersion() {
        val html = builder.build("https://h/m.plist?id=1", "A <b> & \"C\"", "1.0")

        assertThat(html).contains("A &lt;b&gt; &amp; &quot;C&quot;")
        assertThat(html).contains("버전 1.0")
        // 원본 raw 특수문자가 그대로 노출되면 안 됨
        assertThat(html).doesNotContain("A <b> & \"C\"")
    }

    @Test
    @DisplayName("Content 는 완결된 HTML 문서다")
    fun producesHtmlDocument() {
        val html = builder.build("https://h/m.plist?id=1", "앱", "1.0")

        assertThat(html).startsWith("<!DOCTYPE html>")
        assertThat(html).contains("<html")
        assertThat(html).contains("</html>")
    }
}
