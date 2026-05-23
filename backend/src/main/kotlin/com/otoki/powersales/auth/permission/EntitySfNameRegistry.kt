package com.otoki.powersales.auth.permission

import com.otoki.powersales.common.salesforce.SFObject
import jakarta.persistence.EntityManager
import jakarta.persistence.Table
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.InitializingBean
import org.springframework.stereotype.Component
import java.util.concurrent.ConcurrentHashMap

/**
 * 신규 entity `@Table(name)` ↔ SF API name (`@SFObject(value)`) 양방향 lookup (spec #801).
 *
 * 부팅 시 JPA EntityManager metamodel scan 으로 모든 `@SFObject` 부착 entity 를 1회 인덱싱한 후
 * 정적 ConcurrentHashMap 으로 캐싱. 매 권한 판정마다 reflection 호출 없음.
 *
 * SF `@SFObject` 부재인 신규 자체 entity 는 toSfApiName 이 null 반환 — SF 권한 매칭 대상 아님.
 *
 * @see RequiresSfPermission §식별자 정책
 */
@Component
class EntitySfNameRegistry(
    private val entityManager: EntityManager,
) : InitializingBean {

    private val log = LoggerFactory.getLogger(javaClass)
    private val entityToSfApiName: MutableMap<String, String> = ConcurrentHashMap()
    private val sfApiNameToEntity: MutableMap<String, String> = ConcurrentHashMap()

    override fun afterPropertiesSet() {
        val metamodel = entityManager.metamodel
        for (entityType in metamodel.entities) {
            val javaType = entityType.javaType ?: continue
            val sfAnnotation = javaType.getAnnotation(SFObject::class.java) ?: continue
            val tableAnnotation = javaType.getAnnotation(Table::class.java)
            val tableName = tableAnnotation?.name
                ?.trim('"')
                ?.takeIf { it.isNotBlank() }
                ?: javaType.simpleName.lowercase()
            val sfApiName = sfAnnotation.value
            entityToSfApiName[tableName] = sfApiName
            sfApiNameToEntity[sfApiName] = tableName
        }
        log.info("[EntitySfNameRegistry] {} entity ↔ SF API name 매핑 인덱싱 완료", entityToSfApiName.size)
    }

    /**
     * 신규 entity table name → SF API name 변환. SF 부재 entity 는 null.
     */
    fun toSfApiName(entityTableName: String): String? = entityToSfApiName[entityTableName]

    /**
     * SF API name → 신규 entity table name 변환. 매핑 부재 entity (SF 메타에만 존재) 는 null.
     */
    fun toEntityTableName(sfApiName: String): String? = sfApiNameToEntity[sfApiName]

    /**
     * 등록된 모든 (entity → SF API name) 매핑 스냅샷.
     */
    fun snapshot(): Map<String, String> = entityToSfApiName.toMap()
}
