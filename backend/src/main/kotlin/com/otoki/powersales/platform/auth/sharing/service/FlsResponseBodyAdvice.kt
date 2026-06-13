package com.otoki.powersales.platform.auth.sharing.service

import com.otoki.powersales.platform.auth.sharing.annotation.FlsField
import com.otoki.powersales.platform.auth.sharing.annotation.FlsFiltered
import com.otoki.powersales.platform.auth.web.WebUserPrincipal
import com.otoki.powersales.user.repository.UserRepository
import org.slf4j.LoggerFactory
import org.springframework.core.MethodParameter
import org.springframework.http.MediaType
import org.springframework.http.converter.HttpMessageConverter
import org.springframework.http.server.ServerHttpRequest
import org.springframework.http.server.ServerHttpResponse
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice
import tools.jackson.core.type.TypeReference
import tools.jackson.databind.ObjectMapper
import kotlin.reflect.KProperty1
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.memberProperties

/**
 * FLS 응답 mask hook (spec #797 — #795 P1-B 인프라의 실제 mask 구현).
 *
 * `@FlsFiltered` 부착 endpoint 의 응답을 intercept — `@FlsField` 부착 field 중 사용자가 readable 권한
 * 보유하지 않은 항목을 응답 JSON 에서 omit (SF 동등).
 *
 * ## 결정 사항 정합 (spec #797 Q1~Q6 옵션 1)
 * - Q1: SecurityContextHolder 에서 WebUserPrincipal 직접 추출
 * - Q2: Jackson ObjectMapper convertValue 로 Map 변환 후 readable=false key 제거
 * - Q3: `@FlsField` 미부착 field 는 평가 자체 skip (항상 통과 — audit field 면제)
 * - Q4: root level DTO 만 mask. nested DTO / List 는 후행 spec
 * - Q5: 미인증 / WebUserPrincipal 부재 시 body 그대로 통과 (debug log)
 * - Q6: 본 spec 은 인프라 활성화만 — endpoint/DTO 별 annotation 부착은 후행 spec
 *
 * ## Profile 측 평가 (PermissionSet 위주)
 * 운영 인벤토리 §2.8 — Profile.fieldPermissions 0건 / PermissionSet 26건 활용. `FlsService.readableFields`
 * 에 profileId 전달 시 추가 DB 조회 발생 — 본 advice 는 매 응답마다 호출되는 핫패스라 profileId 전달은
 * UserRepository 조회 1회 추가. `@Cacheable` 은 FlsService 측에서 흡수 (cache name `field-permission:v1`).
 */
@RestControllerAdvice
class FlsResponseBodyAdvice(
    private val flsService: FlsService,
    private val permissionSetEvaluator: PermissionSetEvaluator,
    private val userRepository: UserRepository,
    private val objectMapper: ObjectMapper,
) : ResponseBodyAdvice<Any> {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun supports(returnType: MethodParameter, converterType: Class<out HttpMessageConverter<*>>): Boolean {
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

        // Q1 + Q5 — SecurityContext 에서 WebUserPrincipal 추출. 미인증/타 principal 시 body 그대로
        val auth = SecurityContextHolder.getContext()?.authentication
        val principal = auth?.principal as? WebUserPrincipal
        if (principal == null) {
            log.debug("[fls] {} no WebUserPrincipal — body 그대로 통과", sObjectName)
            return body
        }

        // Q3 — DTO class 의 `@FlsField` 부착 property 일람. 0건이면 mask 자체 skip
        val flsFieldMap = discoverFlsFields(body::class.java)
        if (flsFieldMap.isEmpty()) {
            log.debug("[fls] {} body class {} has no @FlsField — skip", sObjectName, body::class.java.simpleName)
            return body
        }

        // PermissionSet snapshot — permissionSetIds 활용 (#796)
        val snapshot = permissionSetEvaluator.getPermissionSetSnapshot(principal.userId)
        // profileId 는 운영 0건이라 추가 조회 비용 최소화 — null 전달 시 FlsService 가 Profile 측 skip
        val profileId = userRepository.findById(principal.userId).orElse(null)?.profileId

        val readableFields = flsService.readableFields(
            userId = principal.userId,
            sObjectName = sObjectName,
            profileId = profileId,
            permissionSetIds = snapshot.permissionSetIds,
        )

        // Q2 — ObjectMapper 로 Map 변환 후 readable=false key omit (Q4 — root level 만)
        val bodyMap: MutableMap<String, Any?> = try {
            @Suppress("UNCHECKED_CAST")
            objectMapper.convertValue(body, object : TypeReference<MutableMap<String, Any?>>() {})
        } catch (e: Exception) {
            log.warn("[fls] {} body class {} Map 변환 실패 — body 그대로 통과: {}",
                sObjectName, body::class.java.simpleName, e.message)
            return body
        }

        flsFieldMap.forEach { (propertyName, sObjectField) ->
            // sObjectField = "SObject.FieldApiName" — split 후 fieldName 추출
            val fieldName = sObjectField.substringAfter(".")
            if (fieldName !in readableFields) {
                bodyMap.remove(propertyName)
            }
        }

        return bodyMap
    }

    /**
     * DTO 의 `@FlsField` 부착 property 인벤토리 — property name → sObjectField.
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
