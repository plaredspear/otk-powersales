package com.otoki.powersales.platform.auth.permission

import com.otoki.powersales.platform.common.salesforce.SFObject
import jakarta.persistence.EntityManager
import jakarta.persistence.Table
import org.slf4j.LoggerFactory
import org.springframework.aop.support.AopUtils
import org.springframework.beans.factory.InitializingBean
import org.springframework.context.ApplicationContext
import org.springframework.stereotype.Component
import java.util.concurrent.ConcurrentHashMap
import kotlin.collections.iterator

/**
 * 권한 자원 카탈로그 + SF API name 양방향 lookup (spec #801 + spec #808).
 *
 * ## 책임
 *
 * 1. **SF API name lookup** (spec #801 도입): 신규 entity `@Table(name)` ↔ SF API name (`@SFObject(value)`)
 *    양방향 매핑. PermissionSet.object_permissions JSON 파싱 시 SF API name → entity table name 변환.
 * 2. **권한 자원 카탈로그** (spec #808 확장): `@RequiresSfPermission(entity = ...)` 가드의 식별자 합집합.
 *    - 모든 JPA entity 의 `@Table(name)` (SF 부착 무관)
 *    - `@PermissionResource("name")` 부착 클래스의 name (JPA entity 없는 가상 자원)
 *
 * ## 카탈로그 구축 시점
 *
 * 부팅 시 1회 — JPA EntityManager metamodel scan + Spring `ApplicationContext` 의 `@PermissionResource`
 * bean scan 으로 인덱싱. 매 권한 판정마다 reflection 호출 없음.
 *
 * @see RequiresSfPermission §식별자 정책
 * @see PermissionResource
 */
@Component
class EntitySfNameRegistry(
    private val entityManager: EntityManager,
    private val applicationContext: ApplicationContext,
) : InitializingBean {

    private val log = LoggerFactory.getLogger(javaClass)

    /** SF API name lookup — `@SFObject` 부착 entity 만 등록. spec #801 호환. */
    private val entityToSfApiName: MutableMap<String, String> = ConcurrentHashMap()
    private val sfApiNameToEntity: MutableMap<String, String> = ConcurrentHashMap()

    /** 권한 자원 카탈로그 — `@RequiresSfPermission(entity = ...)` 의 합법 식별자 합집합. spec #808. */
    private val resourceCatalog: MutableSet<String> = ConcurrentHashMap.newKeySet()

    override fun afterPropertiesSet() {
        indexJpaEntities()
        indexPermissionResources()
        log.info(
            "[EntitySfNameRegistry] 카탈로그 인덱싱 완료 — JPA entity {} 종 (SF 매핑 {} 종 포함) + custom resource {} 종 = total {} 자원",
            countJpaEntities(),
            entityToSfApiName.size,
            resourceCatalog.size - countJpaEntities(),
            resourceCatalog.size,
        )
    }

    private fun indexJpaEntities() {
        val metamodel = entityManager.metamodel
        for (entityType in metamodel.entities) {
            val javaType = entityType.javaType ?: continue
            val tableAnnotation = javaType.getAnnotation(Table::class.java)
            val tableName = tableAnnotation?.name
                ?.trim('"')
                ?.takeIf { it.isNotBlank() }
                ?: javaType.simpleName.lowercase()

            // (spec #808) 모든 JPA entity 를 자원 카탈로그에 등록 — @SFObject 부착 무관.
            resourceCatalog.add(tableName)

            // SF API name lookup 은 @SFObject 부착 entity 만 (spec #801 호환).
            val sfAnnotation = javaType.getAnnotation(SFObject::class.java) ?: continue
            val sfApiName = sfAnnotation.value
            entityToSfApiName[tableName] = sfApiName
            sfApiNameToEntity[sfApiName] = tableName
        }
    }

    private fun indexPermissionResources() {
        val beans = applicationContext.getBeansWithAnnotation(PermissionResource::class.java)
        for ((beanName, bean) in beans) {
            // Spring AOP 프록시 우회 — userClass 가 원본 클래스
            val targetClass = AopUtils.getTargetClass(bean)
            val annotation = targetClass.getAnnotation(PermissionResource::class.java)
                ?: bean.javaClass.getAnnotation(PermissionResource::class.java)
                ?: continue
            val resourceName = annotation.name.takeIf { it.isNotBlank() }
                ?: error("[EntitySfNameRegistry] @PermissionResource name 누락 — bean=$beanName class=${targetClass.name}")
            if (resourceCatalog.contains(resourceName) && entityToSfApiName.containsKey(resourceName).not()) {
                // 다른 가상 자원과의 이름 충돌만 차단. JPA entity 와 동일 이름은 의도된 별칭 가능성 있어 경고로만.
                log.warn(
                    "[EntitySfNameRegistry] @PermissionResource('{}') 가 다른 자원과 이름 충돌 — bean={}",
                    resourceName, beanName,
                )
            }
            resourceCatalog.add(resourceName)
        }
    }

    private fun countJpaEntities(): Int = entityManager.metamodel.entities.size

    /**
     * 신규 entity table name → SF API name 변환. SF 부재 entity 는 null. (spec #801)
     */
    fun toSfApiName(entityTableName: String): String? = entityToSfApiName[entityTableName]

    /**
     * SF API name → 신규 entity table name 변환. 매핑 부재 entity (SF 메타에만 존재) 는 null. (spec #801)
     */
    fun toEntityTableName(sfApiName: String): String? = sfApiNameToEntity[sfApiName]

    /**
     * 등록된 모든 (entity → SF API name) 매핑 스냅샷. `@SFObject` 부착 entity 만 포함. (spec #801)
     *
     * 호출처 — entity × Profile matrix UI 등 SF entity 단독 조회 케이스.
     * 권한 펼침 (VIEW_ALL_DATA / MODIFY_ALL_DATA) 용도는 [allResources] 사용 (spec #808).
     */
    fun snapshot(): Map<String, String> = entityToSfApiName.toMap()

    /**
     * 모든 권한 자원 식별자 (entity table name + custom resource name 합집합) 스냅샷. (spec #808)
     *
     * `@RequiresSfPermission(entity = X)` 의 X 가 합법인지 검증하는 lint + system 권한 비트의
     * 모든 자원 펼침 (`SfPermissionResolver.expandAllDataBits`) 에 사용.
     */
    fun allResources(): Set<String> = resourceCatalog.toSet()

    /**
     * 자원 식별자가 카탈로그에 등록되어 있는지 확인. (spec #808)
     */
    fun contains(resourceName: String): Boolean = resourceCatalog.contains(resourceName)
}
