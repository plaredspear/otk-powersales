package com.otoki.powersales.domain.activity.order.repository

import com.otoki.powersales.domain.activity.order.entity.OrderRequest
import com.otoki.powersales.domain.activity.order.entity.OrderRequestProduct
import com.otoki.powersales.domain.foundation.product.entity.Product
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
import java.math.BigDecimal

/**
 * OrderRequestProductRepository QueryDSL 전환 검증 — `findByOrderRequest_IdOrderByLineNumberAsc`.
 *
 * 지정 주문요청의 라인만 라인번호 오름차순으로 조회하고 product 를 fetch join 하는지 실 DB 로 검증한다.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@ActiveProfiles("test")
@Import(QueryDslConfig::class)
class OrderRequestProductRepositoryTest {

    @Autowired
    private lateinit var orderRequestProductRepository: OrderRequestProductRepository

    @Autowired
    private lateinit var em: TestEntityManager

    @Test
    @DisplayName("findByOrderRequest_IdOrderByLineNumberAsc - 해당 주문요청 라인만 라인번호 오름차순 반환")
    fun returnsLinesOrderedByLineNumber() {
        val target = persistOrderRequest("OR-0001")
        val other = persistOrderRequest("OR-0002")
        val product = em.persistAndFlush(Product())

        // 일부러 역순으로 저장 — 정렬 검증
        persistLine(target, product, BigDecimal(2), "B")
        persistLine(target, product, BigDecimal(1), "A")
        persistLine(target, product, BigDecimal(3), "C")
        // 다른 주문요청 라인(제외 대상)
        persistLine(other, product, BigDecimal(1), "X")
        em.clear()

        val result = orderRequestProductRepository.findByOrderRequest_IdOrderByLineNumberAsc(target.id)

        assertThat(result).hasSize(3)
        assertThat(result.map { it.name }).containsExactly("A", "B", "C")
        // fetch join 된 product 가 즉시 사용 가능
        assertThat(result.first().product?.id).isEqualTo(product.id)
    }

    private fun persistOrderRequest(number: String): OrderRequest =
        em.persistAndFlush(OrderRequest(orderRequestNumber = number))

    private fun persistLine(
        orderRequest: OrderRequest,
        product: Product,
        lineNumber: BigDecimal,
        name: String,
    ) {
        val line = OrderRequestProduct(
            orderRequest = orderRequest,
            lineNumber = lineNumber,
            name = name,
        ).apply {
            this.product = product
        }
        em.persistAndFlush(line)
    }
}
