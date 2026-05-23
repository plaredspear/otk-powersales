package com.otoki.powersales.auth.permission

import com.otoki.powersales.common.salesforce.SFObject
import io.mockk.every
import io.mockk.mockk
import jakarta.persistence.EntityManager
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.metamodel.EntityType
import jakarta.persistence.metamodel.Metamodel
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.context.ApplicationContext

@DisplayName("EntitySfNameRegistry")
class EntitySfNameRegistryTest {

    @Test
    @DisplayName("JPA entity scan — @SFObject 부착 + 미부착 모두 자원 카탈로그 등록 (spec #808)")
    fun jpaEntityScanCoversAll() {
        val registry = registryOf(
            jpaEntities = listOf(SfEntity::class.java, NonSfEntity::class.java),
            permissionResources = emptyMap(),
        )
        registry.afterPropertiesSet()

        assertThat(registry.allResources()).containsExactlyInAnyOrder("sf_entity", "non_sf_entity")
        // SF lookup 은 @SFObject 부착 entity 만
        assertThat(registry.toSfApiName("sf_entity")).isEqualTo("SfEntity__c")
        assertThat(registry.toSfApiName("non_sf_entity")).isNull()
    }

    @Test
    @DisplayName("@PermissionResource 가상 자원 — 카탈로그에 합집합 등록")
    fun permissionResourceScanned() {
        val virtualBean = VirtualDashboard()
        val registry = registryOf(
            jpaEntities = listOf(SfEntity::class.java),
            permissionResources = mapOf("dashboardBean" to virtualBean),
        )
        registry.afterPropertiesSet()

        assertThat(registry.allResources()).containsExactlyInAnyOrder("sf_entity", "dashboard")
        assertThat(registry.contains("dashboard")).isTrue()
    }

    @Test
    @DisplayName("snapshot() 는 @SFObject 부착 entity 만 노출 (spec #801 호환)")
    fun snapshotOnlyShowsSfEntities() {
        val registry = registryOf(
            jpaEntities = listOf(SfEntity::class.java, NonSfEntity::class.java),
            permissionResources = mapOf("dashboardBean" to VirtualDashboard()),
        )
        registry.afterPropertiesSet()

        assertThat(registry.snapshot().keys).containsExactly("sf_entity")
    }

    private fun registryOf(
        jpaEntities: List<Class<*>>,
        permissionResources: Map<String, Any>,
    ): EntitySfNameRegistry {
        val em = mockk<EntityManager>()
        val metamodel = mockk<Metamodel>()
        every { em.metamodel } returns metamodel
        val entityTypes = jpaEntities.map { javaType ->
            val et = mockk<EntityType<*>>()
            every { et.javaType } returns javaType
            et
        }.toSet()
        every { metamodel.entities } returns entityTypes

        val ctx = mockk<ApplicationContext>()
        every { ctx.getBeansWithAnnotation(PermissionResource::class.java) } returns permissionResources

        return EntitySfNameRegistry(em, ctx)
    }

    @Entity
    @Table(name = "sf_entity")
    @SFObject("SfEntity__c")
    class SfEntity {
        @Id
        var id: Long = 0
    }

    @Entity
    @Table(name = "non_sf_entity")
    class NonSfEntity {
        @Id
        var id: Long = 0
    }

    @PermissionResource("dashboard")
    class VirtualDashboard
}
