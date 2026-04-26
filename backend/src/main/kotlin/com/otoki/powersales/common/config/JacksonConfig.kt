package com.otoki.powersales.common.config

import org.springframework.context.annotation.Configuration
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver
import org.springframework.web.method.support.HandlerMethodArgumentResolver
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

// Spring Boot 4 의 기본 ObjectMapper 가 Jackson 3 (`tools.jackson.*`) 이며 Jackson 3 의 KotlinModule
// 및 JavaTime 지원이 ServiceLoader 로 자동 등록되므로 별도 ObjectMapper 빈 구성은 필요 없다.
// `spring.jackson.property-naming-strategy: SNAKE_CASE` 는 application.yml 에서 그대로 유효.
//
// 본 설정은 Spring Security 7 + @WebMvcTest(addFilters=false) 환경에서
// SecurityFilterChain 비활성으로 자동 등록되지 않는 @AuthenticationPrincipal argument resolver 를
// 명시 등록하는 역할만 수행한다.
@Configuration
class JacksonConfig : WebMvcConfigurer {

    override fun addArgumentResolvers(resolvers: MutableList<HandlerMethodArgumentResolver>) {
        resolvers.add(AuthenticationPrincipalArgumentResolver())
    }
}
