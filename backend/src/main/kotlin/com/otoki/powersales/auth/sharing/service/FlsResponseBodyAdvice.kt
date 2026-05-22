package com.otoki.powersales.auth.sharing.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.otoki.powersales.auth.sharing.annotation.FlsField
import com.otoki.powersales.auth.sharing.annotation.FlsFiltered
import org.slf4j.LoggerFactory
import org.springframework.core.MethodParameter
import org.springframework.http.MediaType
import org.springframework.http.converter.HttpMessageConverter
import org.springframework.http.server.ServerHttpRequest
import org.springframework.http.server.ServerHttpResponse
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice
import kotlin.reflect.KProperty1
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.memberProperties

/**
 * FLS 응답 mask hook (spec #795 Q1/Q2 옵션 1).
 *
 * `@FlsFiltered` 부착 endpoint 의 응답을 intercept — `@FlsField` 부착 field 중 사용자가 readable 권한
 * 보유하지 않은 항목을 응답 JSON 에서 omit (SF 동등).
 *
 * **점진 도입** (Q1 옵션 1) — 본 advice 는 annotation 명시 부착 endpoint 만 처리. 미부착 endpoint 는
 * 일반 Jackson 직렬화로 통과 — 모든 field 응답.
 *
 * **mask 방식** (Q2 옵션 1) — DTO 를 ObjectMapper 로 Map 변환 → readable=false key 제거 → 다시
 * Map 반환. Jackson 이 Map 직렬화 시 자동 omit.
 *
 * **점진 도입 단계** — 본 구현은 인프라만 도입. 실제 endpoint 적용은 후행 spec (sObject 별).
 */
@RestControllerAdvice
class FlsResponseBodyAdvice(
    private val flsService: FlsService,
    private val objectMapper: ObjectMapper,
) : ResponseBodyAdvice<Any> {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun supports(returnType: MethodParameter, converterType: Class<out HttpMessageConverter<*>>): Boolean {
        // @FlsFiltered 부착 endpoint 만 처리 (메서드 또는 클래스 단위)
        val methodAnnotation = returnType.getMethodAnnotation(FlsFiltered::class.java)
        val classAnnotation = returnType.containingClass.getAnnotation(FlsFiltered::class.java)
        return methodAnnotation != null || classAnnotation != null
    }

    override fun beforeBodyWrite(
        body: Any?,
        returnType: MethodParameter,
        selectedContentType: MediaType,
        selectedConverterType: Class<out HttpMessageConverter<*>>,
        request: ServerHttpRequest,
        response: ServerHttpResponse,
    ): Any? {
        if (body == null) return null

        val flsAnnotation = returnType.getMethodAnnotation(FlsFiltered::class.java)
            ?: returnType.containingClass.getAnnotation(FlsFiltered::class.java)
            ?: return body

        val sObjectName = flsAnnotation.sObject

        // TODO: SecurityContext 에서 현재 사용자 + profileId + permissionSetIds 추출
        // 본 인프라는 placeholder — 실제 사용자 컨텍스트 주입은 후행 spec (sObject 별 적용 시).
        // 현재는 mask 무조건 skip — annotation 부착 endpoint 도 응답 그대로 통과.
        log.debug("[fls] {} intercepted (no-op — 점진 도입 placeholder)", sObjectName)
        return body
    }

    /**
     * DTO 의 `@FlsField` 부착 property 인벤토리.
     *
     * 본 메서드는 P1-B 의 `GET /api/v1/admin/security/fls/dto-fields?sObject=` endpoint 의
     * 구현 보조 — DTO class 인벤토리 산출.
     */
    fun discoverFlsFields(dtoClass: Class<*>): Map<String, String> {
        val kClass = dtoClass.kotlin
        return kClass.memberProperties
            .mapNotNull { prop: KProperty1<out Any, *> ->
                val ann = prop.findAnnotation<FlsField>() ?: return@mapNotNull null
                prop.name to ann.sObjectField
            }
            .toMap()
    }
}
