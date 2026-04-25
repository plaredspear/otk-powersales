package com.otoki.powersales.common.config

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.PropertyNamingStrategies
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.http.converter.HttpMessageConverter
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver
import org.springframework.web.method.support.HandlerMethodArgumentResolver
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer
import java.util.TimeZone

// Spring Boot 4 의 기본 ObjectMapper 는 Jackson 3 (`tools.jackson.*`).
// 본 프로젝트 메인 코드는 아직 Jackson 2 (`com.fasterxml.jackson.*`) 를 사용하므로,
// 컨트롤러/서비스가 주입받는 ObjectMapper 와 Spring MVC HTTP 메시지 컨버터가 Jackson 2 타입이 되도록 명시 등록한다.
// WebMvcConfigurer 를 implement 해 @WebMvcTest 테스트 슬라이스에서도 자동 로드되도록 한다.
// #539-P1 에서 Jackson 3 정식 전환 시 본 클래스는 제거 또는 단순화될 예정.
@Configuration
class JacksonConfig : WebMvcConfigurer {

    @Bean
    @Primary
    fun objectMapper(): ObjectMapper {
        return ObjectMapper().apply {
            registerModule(KotlinModule.Builder().build())
            registerModule(JavaTimeModule())
            disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            propertyNamingStrategy = PropertyNamingStrategies.SNAKE_CASE
            setTimeZone(TimeZone.getTimeZone("Asia/Seoul"))
        }
    }

    override fun extendMessageConverters(converters: MutableList<HttpMessageConverter<*>>) {
        converters.add(0, MappingJackson2HttpMessageConverter(objectMapper()))
    }

    // Spring Security 7 + Boot 4 의 @WebMvcTest(addFilters=false) 환경에서는 SecurityFilterChain 이
    // 비활성화되어 @AuthenticationPrincipal argument resolver 가 자동 등록되지 않는다.
    // 명시적으로 등록해 컨트롤러 테스트가 SecurityContextHolder 의 principal 을 주입받을 수 있게 한다.
    override fun addArgumentResolvers(resolvers: MutableList<HandlerMethodArgumentResolver>) {
        resolvers.add(AuthenticationPrincipalArgumentResolver())
    }
}
