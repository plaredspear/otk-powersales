package com.otoki.powersales.domain.activity.order.repository

import com.otoki.powersales.domain.activity.order.entity.ErpOrder
import com.otoki.powersales.domain.activity.order.entity.ErpOrderProduct
import com.otoki.powersales.platform.common.config.QueryDslConfig
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles
import java.time.LocalDate

/**
 * ErpOrderProductRepository QueryDSL 전환 검증 — `deleteByErpOrderOrderDateBefore`.
 *
 * 부모(`erp_order.order_date`) 기준 서브쿼리로 자식 라인을 hard delete 한다. 라인 자체에 order_date 가
 * 없으므로 부모 헤더의 order_date < cutoff 인 라인만 삭제되고, 부모 order_date 가 null/cutoff 이후인
 * 라인은 보존되는지(서브쿼리 IN 동작) 실 DB 로 검증한다.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@ActiveProfiles("test")
@Import(QueryDslConfig::class)
class ErpOrderProductRepositoryTest {

    @Autowired
    private lateinit var erpOrderProductRepository: ErpOrderProductRepository

    @Autowired
    private lateinit var em: TestEntityManager

    @Test
    @DisplayName("deleteByErpOrderOrderDateBefore - 부모 order_date < cutoff 인 라인만 삭제, null/이후 부모 라인은 보존")
    fun deletesLinesOfOldParentsOnly() {
        // 오래된 부모(삭제 대상) — 라인 2개
        val oldOrder = persistOrder("0300000001", LocalDate.of(2026, 1, 1))
        persistLine(oldOrder, "L1")
        persistLine(oldOrder, "L2")
        // cutoff 이후 부모(보존) — 라인 1개
        val recentOrder = persistOrder("0300000002", LocalDate.of(2026, 5, 1))
        persistLine(recentOrder, "L3")
        // order_date null 부모(보존) — 라인 1개
        val nullDateOrder = persistOrder("0300000003", null)
        persistLine(nullDateOrder, "L4")
        em.clear()

        val deleted = erpOrderProductRepository.deleteByErpOrderOrderDateBefore(LocalDate.of(2026, 3, 1))
        em.clear()

        assertThat(deleted).isEqualTo(2)
        val remaining = erpOrderProductRepository.findAll().map { it.name }
        assertThat(remaining).containsExactlyInAnyOrder("L3", "L4")
    }

    private fun persistOrder(sapOrderNumber: String, orderDate: LocalDate?): ErpOrder {
        val order = ErpOrder(sapOrderNumber = sapOrderNumber, orderDate = orderDate)
        return em.persistAndFlush(order)
    }

    private fun persistLine(order: ErpOrder, name: String) {
        val line = ErpOrderProduct(name = name).apply { erpOrder = order }
        em.persistAndFlush(line)
    }
}
