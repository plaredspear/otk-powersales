package com.otoki.internal.repository

import com.otoki.internal.entity.ApprovalStatus
import com.otoki.internal.entity.Order
import com.otoki.internal.entity.Store
import com.otoki.internal.entity.User
import com.otoki.internal.entity.UserRole
import com.otoki.internal.entity.WorkerType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.test.context.ActiveProfiles
import java.time.LocalDate

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@ActiveProfiles("test")
@DisplayName("OrderRepository 테스트")
class OrderRepositoryTest {

    @Autowired
    private lateinit var orderRepository: OrderRepository

    @Autowired
    private lateinit var testEntityManager: TestEntityManager

    private lateinit var testUser1: User
    private lateinit var testUser2: User
    private lateinit var testStore1: Store
    private lateinit var testStore2: Store

    @BeforeEach
    fun setUp() {
        orderRepository.deleteAll()
        testEntityManager.clear()

        // 테스트 사용자 생성
        testUser1 = testEntityManager.persistAndFlush(User(
            employeeId = "10000001",
            password = "encoded",
            name = "홍길동",
            department = "영업부",
            branchName = "서울지점",
            role = UserRole.USER,
            workerType = WorkerType.PATROL
        ))

        testUser2 = testEntityManager.persistAndFlush(User(
            employeeId = "10000002",
            password = "encoded",
            name = "김영희",
            department = "영업부",
            branchName = "부산지점",
            role = UserRole.USER,
            workerType = WorkerType.PATROL
        ))

        // 테스트 거래처 생성
        testStore1 = testEntityManager.persistAndFlush(Store(
            storeCode = "ST001",
            storeName = "천사푸드"
        ))

        testStore2 = testEntityManager.persistAndFlush(Store(
            storeCode = "ST002",
            storeName = "행복식품"
        ))

        // 테스트 주문 생성
        val orders = listOf(
            // 사용자1의 주문 (천사푸드, 승인완료, 2/1주문, 2/4납기)
            createOrder("OP00000001", testUser1, testStore1, ApprovalStatus.APPROVED,
                LocalDate.of(2026, 2, 1), LocalDate.of(2026, 2, 4), 500000L),
            // 사용자1의 주문 (천사푸드, 승인대기, 2/3주문, 2/6납기)
            createOrder("OP00000002", testUser1, testStore1, ApprovalStatus.PENDING,
                LocalDate.of(2026, 2, 3), LocalDate.of(2026, 2, 6), 300000L),
            // 사용자1의 주문 (행복식품, 전송실패, 2/5주문, 2/8납기)
            createOrder("OP00000003", testUser1, testStore2, ApprovalStatus.SEND_FAILED,
                LocalDate.of(2026, 2, 5), LocalDate.of(2026, 2, 8), 800000L),
            // 사용자1의 주문 (행복식품, 승인완료, 1/20주문, 1/25납기 - 1월)
            createOrder("OP00000004", testUser1, testStore2, ApprovalStatus.APPROVED,
                LocalDate.of(2026, 1, 20), LocalDate.of(2026, 1, 25), 200000L),
            // 사용자2의 주문 (다른 사용자)
            createOrder("OP00000005", testUser2, testStore1, ApprovalStatus.APPROVED,
                LocalDate.of(2026, 2, 1), LocalDate.of(2026, 2, 4), 100000L)
        )
        orders.forEach { testEntityManager.persistAndFlush(it) }
        testEntityManager.clear()
    }

    // ========== 사용자별 조회 ==========

    @Nested
    @DisplayName("사용자별 조회")
    inner class UserFilter {

        @Test
        @DisplayName("사용자1의 주문만 조회 - 4건 반환")
        fun findByUserId_user1_returns4Orders() {
            // Given
            val pageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "orderDate"))

            // When
            val result = orderRepository.findByUserIdWithFilters(
                testUser1.id, null, null, null, null, pageable
            )

            // Then
            assertThat(result.content).hasSize(4)
            assertThat(result.totalElements).isEqualTo(4)
        }

        @Test
        @DisplayName("사용자2의 주문만 조회 - 1건 반환")
        fun findByUserId_user2_returns1Order() {
            // Given
            val pageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "orderDate"))

            // When
            val result = orderRepository.findByUserIdWithFilters(
                testUser2.id, null, null, null, null, pageable
            )

            // Then
            assertThat(result.content).hasSize(1)
            assertThat(result.content[0].orderRequestNumber).isEqualTo("OP00000005")
        }
    }

    // ========== 거래처 필터 ==========

    @Nested
    @DisplayName("거래처 필터")
    inner class StoreFilter {

        @Test
        @DisplayName("사용자1의 천사푸드(store1) 주문만 조회 - 2건")
        fun findByStoreId_store1_returns2Orders() {
            // Given
            val pageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "orderDate"))

            // When
            val result = orderRepository.findByUserIdWithFilters(
                testUser1.id, testStore1.id, null, null, null, pageable
            )

            // Then
            assertThat(result.content).hasSize(2)
            assertThat(result.content).allSatisfy { order ->
                assertThat(order.store.id).isEqualTo(testStore1.id)
            }
        }

        @Test
        @DisplayName("사용자1의 행복식품(store2) 주문만 조회 - 2건")
        fun findByStoreId_store2_returns2Orders() {
            // Given
            val pageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "orderDate"))

            // When
            val result = orderRepository.findByUserIdWithFilters(
                testUser1.id, testStore2.id, null, null, null, pageable
            )

            // Then
            assertThat(result.content).hasSize(2)
            assertThat(result.content).allSatisfy { order ->
                assertThat(order.store.id).isEqualTo(testStore2.id)
            }
        }
    }

    // ========== 상태 필터 ==========

    @Nested
    @DisplayName("상태 필터")
    inner class StatusFilter {

        @Test
        @DisplayName("사용자1의 APPROVED 주문만 조회 - 2건")
        fun findByStatus_approved_returns2Orders() {
            // Given
            val pageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "orderDate"))

            // When
            val result = orderRepository.findByUserIdWithFilters(
                testUser1.id, null, ApprovalStatus.APPROVED, null, null, pageable
            )

            // Then
            assertThat(result.content).hasSize(2)
            assertThat(result.content).allSatisfy { order ->
                assertThat(order.approvalStatus).isEqualTo(ApprovalStatus.APPROVED)
            }
        }

        @Test
        @DisplayName("사용자1의 SEND_FAILED 주문만 조회 - 1건")
        fun findByStatus_sendFailed_returns1Order() {
            // Given
            val pageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "orderDate"))

            // When
            val result = orderRepository.findByUserIdWithFilters(
                testUser1.id, null, ApprovalStatus.SEND_FAILED, null, null, pageable
            )

            // Then
            assertThat(result.content).hasSize(1)
            assertThat(result.content[0].approvalStatus).isEqualTo(ApprovalStatus.SEND_FAILED)
        }

        @Test
        @DisplayName("사용자1의 RESEND 주문 조회 - 0건")
        fun findByStatus_resend_returnsEmpty() {
            // Given
            val pageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "orderDate"))

            // When
            val result = orderRepository.findByUserIdWithFilters(
                testUser1.id, null, ApprovalStatus.RESEND, null, null, pageable
            )

            // Then
            assertThat(result.content).isEmpty()
        }
    }

    // ========== 납기일 범위 필터 ==========

    @Nested
    @DisplayName("납기일 범위 필터")
    inner class DateRangeFilter {

        @Test
        @DisplayName("2월 납기 주문만 조회 - 3건")
        fun findByDateRange_february_returns3Orders() {
            // Given
            val pageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "orderDate"))
            val from = LocalDate.of(2026, 2, 1)
            val to = LocalDate.of(2026, 2, 28)

            // When
            val result = orderRepository.findByUserIdWithFilters(
                testUser1.id, null, null, from, to, pageable
            )

            // Then
            assertThat(result.content).hasSize(3)
            assertThat(result.content).allSatisfy { order ->
                assertThat(order.deliveryDate).isAfterOrEqualTo(from)
                assertThat(order.deliveryDate).isBeforeOrEqualTo(to)
            }
        }

        @Test
        @DisplayName("1월 납기 주문만 조회 - 1건")
        fun findByDateRange_january_returns1Order() {
            // Given
            val pageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "orderDate"))
            val from = LocalDate.of(2026, 1, 1)
            val to = LocalDate.of(2026, 1, 31)

            // When
            val result = orderRepository.findByUserIdWithFilters(
                testUser1.id, null, null, from, to, pageable
            )

            // Then
            assertThat(result.content).hasSize(1)
            assertThat(result.content[0].orderRequestNumber).isEqualTo("OP00000004")
        }

        @Test
        @DisplayName("deliveryDateFrom만 지정 - 해당 날짜 이후 주문")
        fun findByDateRange_fromOnly_returnsAfterDate() {
            // Given
            val pageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "orderDate"))
            val from = LocalDate.of(2026, 2, 5)

            // When
            val result = orderRepository.findByUserIdWithFilters(
                testUser1.id, null, null, from, null, pageable
            )

            // Then
            assertThat(result.content).hasSize(2) // 2/6, 2/8 납기
            assertThat(result.content).allSatisfy { order ->
                assertThat(order.deliveryDate).isAfterOrEqualTo(from)
            }
        }

        @Test
        @DisplayName("deliveryDateTo만 지정 - 해당 날짜 이전 주문")
        fun findByDateRange_toOnly_returnsBeforeDate() {
            // Given
            val pageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "orderDate"))
            val to = LocalDate.of(2026, 2, 5)

            // When
            val result = orderRepository.findByUserIdWithFilters(
                testUser1.id, null, null, null, to, pageable
            )

            // Then
            assertThat(result.content).hasSize(2) // 1/25, 2/4 납기
            assertThat(result.content).allSatisfy { order ->
                assertThat(order.deliveryDate).isBeforeOrEqualTo(to)
            }
        }
    }

    // ========== 복합 필터 ==========

    @Nested
    @DisplayName("복합 필터")
    inner class CombinedFilter {

        @Test
        @DisplayName("거래처 + 상태 필터 - store1 AND APPROVED")
        fun findByStoreAndStatus_returns1Order() {
            // Given
            val pageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "orderDate"))

            // When
            val result = orderRepository.findByUserIdWithFilters(
                testUser1.id, testStore1.id, ApprovalStatus.APPROVED, null, null, pageable
            )

            // Then
            assertThat(result.content).hasSize(1)
            assertThat(result.content[0].store.id).isEqualTo(testStore1.id)
            assertThat(result.content[0].approvalStatus).isEqualTo(ApprovalStatus.APPROVED)
        }

        @Test
        @DisplayName("모든 필터 적용 - store1 AND APPROVED AND 2월")
        fun findByAllFilters_returns1Order() {
            // Given
            val pageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "orderDate"))
            val from = LocalDate.of(2026, 2, 1)
            val to = LocalDate.of(2026, 2, 28)

            // When
            val result = orderRepository.findByUserIdWithFilters(
                testUser1.id, testStore1.id, ApprovalStatus.APPROVED, from, to, pageable
            )

            // Then
            assertThat(result.content).hasSize(1)
            assertThat(result.content[0].orderRequestNumber).isEqualTo("OP00000001")
        }
    }

    // ========== 정렬 ==========

    @Nested
    @DisplayName("정렬")
    inner class Sorting {

        @Test
        @DisplayName("주문일 내림차순 정렬 (기본)")
        fun findAll_sortByOrderDateDesc_orderedCorrectly() {
            // Given
            val pageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "orderDate"))

            // When
            val result = orderRepository.findByUserIdWithFilters(
                testUser1.id, null, null, null, null, pageable
            )

            // Then
            val orderDates = result.content.map { it.orderDate }
            assertThat(orderDates).isSortedAccordingTo(Comparator.reverseOrder())
        }

        @Test
        @DisplayName("금액 내림차순 정렬")
        fun findAll_sortByTotalAmountDesc_orderedCorrectly() {
            // Given
            val pageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "totalAmount"))

            // When
            val result = orderRepository.findByUserIdWithFilters(
                testUser1.id, null, null, null, null, pageable
            )

            // Then
            val amounts = result.content.map { it.totalAmount }
            assertThat(amounts).isSortedAccordingTo(Comparator.reverseOrder())
        }

        @Test
        @DisplayName("납기일 오름차순 정렬")
        fun findAll_sortByDeliveryDateAsc_orderedCorrectly() {
            // Given
            val pageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.ASC, "deliveryDate"))

            // When
            val result = orderRepository.findByUserIdWithFilters(
                testUser1.id, null, null, null, null, pageable
            )

            // Then
            val deliveryDates = result.content.map { it.deliveryDate }
            assertThat(deliveryDates).isSorted()
        }
    }

    // ========== 페이지네이션 ==========

    @Nested
    @DisplayName("페이지네이션")
    inner class Pagination {

        @Test
        @DisplayName("페이지 크기 2로 조회 - 정확한 페이지 정보 반환")
        fun findAll_withPagination_returnsCorrectPage() {
            // Given
            val pageable = PageRequest.of(0, 2, Sort.by(Sort.Direction.DESC, "orderDate"))

            // When
            val result = orderRepository.findByUserIdWithFilters(
                testUser1.id, null, null, null, null, pageable
            )

            // Then
            assertThat(result.content).hasSize(2)
            assertThat(result.totalElements).isEqualTo(4)
            assertThat(result.totalPages).isEqualTo(2)
            assertThat(result.isFirst).isTrue()
            assertThat(result.isLast).isFalse()
        }

        @Test
        @DisplayName("마지막 페이지 조회 - last=true")
        fun findAll_lastPage_isLastTrue() {
            // Given
            val pageable = PageRequest.of(1, 2, Sort.by(Sort.Direction.DESC, "orderDate"))

            // When
            val result = orderRepository.findByUserIdWithFilters(
                testUser1.id, null, null, null, null, pageable
            )

            // Then
            assertThat(result.content).hasSize(2)
            assertThat(result.isLast).isTrue()
        }
    }

    // ========== 헬퍼 메서드 ==========

    private fun createOrder(
        orderRequestNumber: String,
        user: User,
        store: Store,
        approvalStatus: ApprovalStatus,
        orderDate: LocalDate,
        deliveryDate: LocalDate,
        totalAmount: Long
    ): Order {
        return Order(
            orderRequestNumber = orderRequestNumber,
            user = user,
            store = store,
            orderDate = orderDate,
            deliveryDate = deliveryDate,
            totalAmount = totalAmount,
            approvalStatus = approvalStatus,
            isClosed = false
        )
    }
}
