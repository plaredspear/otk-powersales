package com.otoki.powersales.external.sap.inbound.toggle

import com.otoki.powersales.external.sap.auth.audit.SapInboundAuditService
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean
import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.config.annotation.InterceptorRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer
import tools.jackson.databind.ObjectMapper

/**
 * [SapInboundToggleInterceptor] 를 SAP 인바운드 적재 endpoint 에 등록한다.
 *
 * 토큰 발급(`/api/v1/sap/oauth/token`)은 적재 처리가 아니므로 토글 대상에서 제외한다.
 *
 * `@ConditionalOnBean(SapInboundToggleStore)` — 인바운드 토글 저장소 빈이 없는 슬라이스 테스트
 * (`@WebMvcTest` 등, 일반 `@Component` 미스캔)에서는 본 설정이 비활성화되어 인터셉터가 등록되지 않는다.
 * 그렇지 않으면 모든 MVC 슬라이스 테스트가 인터셉터 의존성을 요구하게 된다.
 *
 * 인터셉터는 `@Component` 가 아니라 여기서 직접 생성한다 — `HandlerInterceptor` 를 빈으로 두면
 * `@WebMvcTest` 가 이를 자동 스캔하기 때문이다.
 */
@Configuration
@ConditionalOnBean(SapInboundToggleStore::class)
class SapInboundToggleWebConfig(
    private val toggleStore: SapInboundToggleStore,
    private val auditService: SapInboundAuditService,
    private val objectMapper: ObjectMapper,
) : WebMvcConfigurer {

    override fun addInterceptors(registry: InterceptorRegistry) {
        val interceptor = SapInboundToggleInterceptor(toggleStore, auditService, objectMapper)
        registry.addInterceptor(interceptor)
            .addPathPatterns("/api/v1/sap/**")
            .excludePathPatterns("/api/v1/sap/oauth/token")
    }
}
