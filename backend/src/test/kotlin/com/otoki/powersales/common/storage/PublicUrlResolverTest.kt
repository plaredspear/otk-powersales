package com.otoki.powersales.common.storage

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("PublicUrlResolver 테스트")
class PublicUrlResolverTest {

    @Test
    @DisplayName("prefix 와 uniqueKey 가 모두 있으면 완전 URL 반환")
    fun resolve_normal() {
        val resolver = PublicUrlResolver(prefix = "https://bucket.s3.ap-northeast-2.amazonaws.com/public")

        val result = resolver.resolve("26may2026claim_001jpg")

        assertThat(result).isEqualTo("https://bucket.s3.ap-northeast-2.amazonaws.com/public/26may2026claim_001jpg")
    }

    @Test
    @DisplayName("prefix 가 / 로 끝나도 중복되지 않음")
    fun resolve_trailingSlash() {
        val resolver = PublicUrlResolver(prefix = "https://bucket.s3.ap-northeast-2.amazonaws.com/public/")

        val result = resolver.resolve("key.jpg")

        assertThat(result).isEqualTo("https://bucket.s3.ap-northeast-2.amazonaws.com/public/key.jpg")
    }

    @Test
    @DisplayName("uniqueKey 가 null 이면 null 반환")
    fun resolve_nullKey() {
        val resolver = PublicUrlResolver(prefix = "https://bucket.s3.ap-northeast-2.amazonaws.com/public")

        assertThat(resolver.resolve(null)).isNull()
    }

    @Test
    @DisplayName("uniqueKey 가 빈 문자열이면 null 반환")
    fun resolve_blankKey() {
        val resolver = PublicUrlResolver(prefix = "https://bucket.s3.ap-northeast-2.amazonaws.com/public")

        assertThat(resolver.resolve("")).isNull()
        assertThat(resolver.resolve("   ")).isNull()
    }

    @Test
    @DisplayName("prefix 가 비어 있으면 uniqueKey 를 그대로 반환 (local 환경)")
    fun resolve_blankPrefix() {
        val resolver = PublicUrlResolver(prefix = "")

        val result = resolver.resolve("key.jpg")

        assertThat(result).isEqualTo("key.jpg")
    }
}
