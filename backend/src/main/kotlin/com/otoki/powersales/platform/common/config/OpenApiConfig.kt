package com.otoki.powersales.platform.common.config

import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.security.SecurityRequirement
import io.swagger.v3.oas.models.security.SecurityScheme
import org.springdoc.core.models.GroupedOpenApi
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class OpenApiConfig {

    @Bean
    fun openAPI(): OpenAPI {
        val securitySchemeName = "Bearer Authentication"

        return OpenAPI()
            .info(
                Info()
                    .title("오뚜기 파워세일즈 API")
                    .description("B2B 영업사원 실적 조회 및 목표 관리 시스템 API")
                    .version("v1.0.0")
            )
            .addSecurityItem(SecurityRequirement().addList(securitySchemeName))
            .components(
                Components()
                    .addSecuritySchemes(
                        securitySchemeName,
                        SecurityScheme()
                            .name(securitySchemeName)
                            .type(SecurityScheme.Type.HTTP)
                            .scheme("bearer")
                            .bearerFormat("JWT")
                    )
            )
    }

    // 그룹별 API 문서 분리. Swagger UI 우상단 select 박스 + `/v3/api-docs/<group>` JSON URL 노출.
    // path 패턴은 `/api/*/<group>/**` 와일드카드로 v1/v2 등 향후 API 버전 변경에도 영향 없음.

    @Bean
    fun adminApi(): GroupedOpenApi = GroupedOpenApi.builder()
        .group("admin")
        .displayName("Admin API")
        .pathsToMatch("/api/*/admin/**")
        .build()

    @Bean
    fun sapApi(): GroupedOpenApi = GroupedOpenApi.builder()
        .group("sap")
        .displayName("SAP Inbound API")
        .pathsToMatch("/api/*/sap/**")
        .build()

    @Bean
    fun mobileApi(): GroupedOpenApi = GroupedOpenApi.builder()
        .group("mobile")
        .displayName("Mobile API")
        .pathsToMatch("/api/*/mobile/**")
        .build()
}
