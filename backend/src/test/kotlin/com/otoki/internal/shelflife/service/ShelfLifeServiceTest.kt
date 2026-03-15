package com.otoki.internal.shelflife.service

import com.otoki.internal.auth.exception.UserNotFoundException
import com.otoki.internal.sap.entity.User
import com.otoki.internal.sap.repository.UserRepository
import com.otoki.internal.shelflife.dto.request.ShelfLifeBatchDeleteRequest
import com.otoki.internal.shelflife.dto.request.ShelfLifeCreateRequest
import com.otoki.internal.shelflife.dto.request.ShelfLifeUpdateRequest
import com.otoki.internal.shelflife.entity.ShelfLife
import com.otoki.internal.shelflife.exception.InvalidAlertDateException
import com.otoki.internal.shelflife.exception.InvalidShelfLifeDateRangeException
import com.otoki.internal.shelflife.exception.ShelfLifeForbiddenException
import com.otoki.internal.shelflife.exception.ShelfLifeNotFoundException
import com.otoki.internal.shelflife.repository.ShelfLifeRepository
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.whenever
import java.time.LocalDate
import java.util.*

@ExtendWith(MockitoExtension::class)
@DisplayName("ShelfLifeService 테스트")
class ShelfLifeServiceTest {

    @Mock
    private lateinit var shelfLifeRepository: ShelfLifeRepository

    @Mock
    private lateinit var userRepository: UserRepository

    @InjectMocks
    private lateinit var shelfLifeService: ShelfLifeService

    private val userId = 1L
    private val employeeIdVal = "20030117"

    private fun createUser(id: Long = userId, sfid: String? = "EMP_SFID_001"): User {
        return User(id = id, sfid = sfid, employeeId = employeeIdVal, name = "테스트사원")
    }

    private fun createShelfLife(
        seq: Int = 1,
        employeeId: String = employeeIdVal,
        accountCode: String = "1025172",
        accountId: String = "(유)경산식품",
        productCode: String = "30310009",
        productId: String = "고등어김치&무조림(캔)280G",
        expirationDate: LocalDate = LocalDate.of(2026, 3, 10),
        alarmDate: LocalDate = LocalDate.of(2026, 3, 9),
        description: String? = null
    ): ShelfLife {
        return ShelfLife(
            seq = seq,
            employeeId = employeeId,
            accountCode = accountCode,
            accountId = accountId,
            productCode = productCode,
            productId = productId,
            expirationDate = expirationDate,
            alarmDate = alarmDate,
            description = description
        )
    }

    private fun stubUser(id: Long = userId) {
        whenever(userRepository.findById(id)).thenReturn(Optional.of(createUser(id = id)))
    }

    @Nested
    @DisplayName("getShelfLifeList - 유통기한 목록 조회")
    inner class GetShelfLifeListTests {

        @Test
        @DisplayName("전체 거래처 조회 - accountCode 미지정 -> 전체 목록 반환")
        fun getList_allAccounts_success() {
            stubUser()
            val items = listOf(createShelfLife(seq = 1), createShelfLife(seq = 2))
            whenever(
                shelfLifeRepository.findByEmployeeIdAndExpirationDateBetweenOrderByExpirationDateAsc(
                    eq(employeeIdVal), any(), any()
                )
            ).thenReturn(items)

            val result = shelfLifeService.getShelfLifeList(userId, null, "2026-03-01", "2026-03-31")

            assertThat(result).hasSize(2)
        }

        @Test
        @DisplayName("특정 거래처 조회 - accountCode 지정 -> 필터링된 목록 반환")
        fun getList_specificAccount_success() {
            stubUser()
            val items = listOf(createShelfLife(seq = 1))
            whenever(
                shelfLifeRepository.findByEmployeeIdAndAccountCodeAndExpirationDateBetweenOrderByExpirationDateAsc(
                    eq(employeeIdVal), eq("1025172"), any(), any()
                )
            ).thenReturn(items)

            val result = shelfLifeService.getShelfLifeList(userId, "1025172", "2026-03-01", "2026-03-31")

            assertThat(result).hasSize(1)
            assertThat(result[0].accountCode).isEqualTo("1025172")
        }

        @Test
        @DisplayName("빈 결과 - 데이터 없음 -> 빈 리스트 반환")
        fun getList_empty() {
            stubUser()
            whenever(
                shelfLifeRepository.findByEmployeeIdAndExpirationDateBetweenOrderByExpirationDateAsc(
                    eq(employeeIdVal), any(), any()
                )
            ).thenReturn(emptyList())

            val result = shelfLifeService.getShelfLifeList(userId, null, "2026-03-01", "2026-03-31")

            assertThat(result).isEmpty()
        }

        @Test
        @DisplayName("데이터 없음 - employeeId로 조회 결과 없음 -> 빈 리스트 반환")
        fun getList_noData_returnsEmpty() {
            stubUser()
            whenever(
                shelfLifeRepository.findByEmployeeIdAndExpirationDateBetweenOrderByExpirationDateAsc(
                    eq(employeeIdVal), any(), any()
                )
            ).thenReturn(emptyList())

            val result = shelfLifeService.getShelfLifeList(userId, null, "2026-03-01", "2026-03-31")

            assertThat(result).isEmpty()
        }

        @Test
        @DisplayName("날짜 범위 초과 - 180일 초과 -> InvalidShelfLifeDateRangeException")
        fun getList_dateRangeExceeded() {
            assertThatThrownBy {
                shelfLifeService.getShelfLifeList(userId, null, "2026-01-01", "2026-12-31")
            }.isInstanceOf(InvalidShelfLifeDateRangeException::class.java)
        }

        @Test
        @DisplayName("종료일 < 시작일 - 역전된 날짜 -> InvalidShelfLifeDateRangeException")
        fun getList_invalidDateOrder() {
            assertThatThrownBy {
                shelfLifeService.getShelfLifeList(userId, null, "2026-03-31", "2026-03-01")
            }.isInstanceOf(InvalidShelfLifeDateRangeException::class.java)
        }

        @Test
        @DisplayName("잘못된 날짜 형식 -> InvalidShelfLifeDateRangeException")
        fun getList_invalidDateFormat() {
            assertThatThrownBy {
                shelfLifeService.getShelfLifeList(userId, null, "invalid", "2026-03-31")
            }.isInstanceOf(InvalidShelfLifeDateRangeException::class.java)
        }
    }

    @Nested
    @DisplayName("createShelfLife - 유통기한 등록")
    inner class CreateShelfLifeTests {

        @Test
        @DisplayName("정상 등록 - 유효한 요청 -> ShelfLife 생성 반환")
        fun create_success() {
            stubUser()
            val request = ShelfLifeCreateRequest(
                accountCode = "1025172",
                accountName = "(유)경산식품",
                productCode = "30310009",
                productName = "고등어김치&무조림(캔)280G",
                expirationDate = "2026-03-10",
                alarmDate = "2026-03-09",
                description = "테스트"
            )
            whenever(shelfLifeRepository.save(any<ShelfLife>())).thenAnswer { it.getArgument<ShelfLife>(0) }

            val result = shelfLifeService.createShelfLife(userId, request)

            assertThat(result.productCode).isEqualTo("30310009")
            assertThat(result.accountCode).isEqualTo("1025172")
        }

        @Test
        @DisplayName("알림일 오류 - alarm_date >= expiration_date -> InvalidAlertDateException")
        fun create_invalidAlertDate() {
            stubUser()
            val request = ShelfLifeCreateRequest(
                accountCode = "1025172",
                accountName = "(유)경산식품",
                productCode = "30310009",
                productName = "제품명",
                expirationDate = "2026-03-10",
                alarmDate = "2026-03-10"
            )

            assertThatThrownBy {
                shelfLifeService.createShelfLife(userId, request)
            }.isInstanceOf(InvalidAlertDateException::class.java)
        }

        @Test
        @DisplayName("사용자 없음 - 존재하지 않는 userId -> UserNotFoundException")
        fun create_userNotFound() {
            whenever(userRepository.findById(999L)).thenReturn(Optional.empty())

            val request = ShelfLifeCreateRequest(
                accountCode = "1025172",
                accountName = "테스트",
                productCode = "30310009",
                productName = "제품명",
                expirationDate = "2026-03-10",
                alarmDate = "2026-03-09"
            )

            assertThatThrownBy {
                shelfLifeService.createShelfLife(999L, request)
            }.isInstanceOf(UserNotFoundException::class.java)
        }
    }

    @Nested
    @DisplayName("updateShelfLife - 유통기한 수정")
    inner class UpdateShelfLifeTests {

        @Test
        @DisplayName("정상 수정 - 유효한 요청 -> 수정된 항목 반환")
        fun update_success() {
            stubUser()
            val entity = createShelfLife(seq = 1)
            whenever(shelfLifeRepository.findById(1)).thenReturn(Optional.of(entity))

            val request = ShelfLifeUpdateRequest(
                expirationDate = "2026-04-10",
                alarmDate = "2026-04-09",
                description = "수정된 설명"
            )

            val result = shelfLifeService.updateShelfLife(userId, 1, request)

            assertThat(result.expirationDate).isEqualTo("2026-04-10")
            assertThat(result.description).isEqualTo("수정된 설명")
        }

        @Test
        @DisplayName("타인 데이터 수정 - employeeId 불일치 -> ShelfLifeForbiddenException")
        fun update_forbidden() {
            stubUser()
            val entity = createShelfLife(seq = 1, employeeId = "OTHER_EMP")
            whenever(shelfLifeRepository.findById(1)).thenReturn(Optional.of(entity))

            val request = ShelfLifeUpdateRequest(
                expirationDate = "2026-04-10",
                alarmDate = "2026-04-09"
            )

            assertThatThrownBy {
                shelfLifeService.updateShelfLife(userId, 1, request)
            }.isInstanceOf(ShelfLifeForbiddenException::class.java)
        }

        @Test
        @DisplayName("존재하지 않는 seq - 없는 seq -> ShelfLifeNotFoundException")
        fun update_notFound() {
            stubUser()
            whenever(shelfLifeRepository.findById(999)).thenReturn(Optional.empty())

            val request = ShelfLifeUpdateRequest(
                expirationDate = "2026-04-10",
                alarmDate = "2026-04-09"
            )

            assertThatThrownBy {
                shelfLifeService.updateShelfLife(userId, 999, request)
            }.isInstanceOf(ShelfLifeNotFoundException::class.java)
        }
    }

    @Nested
    @DisplayName("deleteShelfLife - 유통기한 단건 삭제")
    inner class DeleteShelfLifeTests {

        @Test
        @DisplayName("정상 삭제 - 본인 데이터 -> 성공")
        fun delete_success() {
            stubUser()
            val entity = createShelfLife(seq = 1)
            whenever(shelfLifeRepository.findById(1)).thenReturn(Optional.of(entity))

            shelfLifeService.deleteShelfLife(userId, 1)
        }

        @Test
        @DisplayName("존재하지 않는 seq -> ShelfLifeNotFoundException")
        fun delete_notFound() {
            stubUser()
            whenever(shelfLifeRepository.findById(999)).thenReturn(Optional.empty())

            assertThatThrownBy {
                shelfLifeService.deleteShelfLife(userId, 999)
            }.isInstanceOf(ShelfLifeNotFoundException::class.java)
        }

        @Test
        @DisplayName("타인 데이터 삭제 - employeeId 불일치 -> ShelfLifeForbiddenException")
        fun delete_forbidden() {
            stubUser()
            val entity = createShelfLife(seq = 1, employeeId = "OTHER_EMP")
            whenever(shelfLifeRepository.findById(1)).thenReturn(Optional.of(entity))

            assertThatThrownBy {
                shelfLifeService.deleteShelfLife(userId, 1)
            }.isInstanceOf(ShelfLifeForbiddenException::class.java)
        }
    }

    @Nested
    @DisplayName("deleteShelfLifeBatch - 유통기한 일괄 삭제")
    inner class DeleteShelfLifeBatchTests {

        @Test
        @DisplayName("정상 일괄 삭제 - 본인 데이터 3건 -> deletedCount: 3")
        fun batchDelete_success() {
            stubUser()
            val items = listOf(
                createShelfLife(seq = 1),
                createShelfLife(seq = 2),
                createShelfLife(seq = 3)
            )
            whenever(shelfLifeRepository.findBySeqInAndEmployeeId(eq(listOf(1, 2, 3)), eq(employeeIdVal)))
                .thenReturn(items)

            val request = ShelfLifeBatchDeleteRequest(ids = listOf(1, 2, 3))
            val result = shelfLifeService.deleteShelfLifeBatch(userId, request)

            assertThat(result.deletedCount).isEqualTo(3)
        }

        @Test
        @DisplayName("타인 데이터 포함 - 1건이 타인 소유 -> ShelfLifeForbiddenException")
        fun batchDelete_forbidden() {
            stubUser()
            val myItem = createShelfLife(seq = 1)
            whenever(shelfLifeRepository.findBySeqInAndEmployeeId(eq(listOf(1, 2)), eq(employeeIdVal)))
                .thenReturn(listOf(myItem))
            whenever(shelfLifeRepository.findAllById(listOf(2)))
                .thenReturn(listOf(createShelfLife(seq = 2, employeeId = "OTHER_EMP")))

            val request = ShelfLifeBatchDeleteRequest(ids = listOf(1, 2))

            assertThatThrownBy {
                shelfLifeService.deleteShelfLifeBatch(userId, request)
            }.isInstanceOf(ShelfLifeForbiddenException::class.java)
        }
    }
}
