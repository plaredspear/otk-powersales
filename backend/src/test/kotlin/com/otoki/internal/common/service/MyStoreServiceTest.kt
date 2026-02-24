package com.otoki.internal.common.service

import com.otoki.internal.entity.Account
import com.otoki.internal.common.entity.StoreSchedule
import com.otoki.internal.common.entity.User
import com.otoki.internal.exception.UserNotFoundException
import com.otoki.internal.repository.AccountRepository
import com.otoki.internal.common.repository.StoreScheduleRepository
import com.otoki.internal.common.repository.UserRepository
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
import org.mockito.kotlin.whenever
import java.time.LocalDate
import java.time.YearMonth
import java.util.Optional

@ExtendWith(MockitoExtension::class)
@DisplayName("MyStoreService 테스트")
class MyStoreServiceTest {

    @Mock
    private lateinit var userRepository: UserRepository

    @Mock
    private lateinit var storeScheduleRepository: StoreScheduleRepository

    @Mock
    private lateinit var accountRepository: AccountRepository

    @InjectMocks
    private lateinit var myStoreService: MyStoreService

    private val testUserSfid = "a0B000000012345"

    // ========== getMyStores Tests ==========

    @Nested
    @DisplayName("getMyStores - 내 거래처 목록 조회")
    inner class GetMyStoresTests {

        @Test
        @DisplayName("한 달 거래처 조회 - Account 마스터가 있으면 Account 정보 반환")
        fun getMyStores_withAccountMaster() {
            // Given
            val userId = 1L
            val mockUser = createUser(id = userId, sfid = testUserSfid)
            val now = LocalDate.now()
            val yearMonth = YearMonth.from(now)
            val startDate = yearMonth.atDay(1)
            val endDate = yearMonth.atEndOfMonth()

            val distinctAccountSfids = listOf("SF_ACC001", "SF_ACC002")
            val accounts = listOf(
                createAccount(id = 1L, sfid = "SF_ACC001", name = "(유)경산식품", externalKey = "1025172",
                    address1 = "전라남도 목포시", representative = "김정자", phone = "061-123-4567"),
                createAccount(id = 2L, sfid = "SF_ACC002", name = "(주)대한식품", externalKey = "1025173",
                    address1 = "서울시 강남구", representative = "이영희", phone = "02-111-2222")
            )
            val schedules = listOf(
                createStoreSchedule(account = "SF_ACC001", startDate = now),
                createStoreSchedule(account = "SF_ACC002", startDate = now)
            )

            whenever(userRepository.findById(userId)).thenReturn(Optional.of(mockUser))
            whenever(storeScheduleRepository.findDistinctAccountsByFullNameAndStartDateBetween(testUserSfid, startDate, endDate))
                .thenReturn(distinctAccountSfids)
            whenever(accountRepository.findAll()).thenReturn(accounts)
            whenever(storeScheduleRepository.findByFullNameAndStartDateBetween(testUserSfid, startDate, endDate))
                .thenReturn(schedules)

            // When
            val result = myStoreService.getMyStores(userId, null)

            // Then
            assertThat(result.stores).hasSize(2)
            assertThat(result.totalCount).isEqualTo(2)
            assertThat(result.stores[0].storeName).isEqualTo("(유)경산식품")
            assertThat(result.stores[0].storeCode).isEqualTo("1025172")
            assertThat(result.stores[0].representativeName).isEqualTo("김정자")
        }

        @Test
        @DisplayName("결과 없음 - 스케줄 데이터 없음 -> 빈 리스트 + totalCount=0")
        fun getMyStores_noSchedules() {
            // Given
            val userId = 1L
            val mockUser = createUser(id = userId, sfid = testUserSfid)
            val now = LocalDate.now()
            val yearMonth = YearMonth.from(now)
            val startDate = yearMonth.atDay(1)
            val endDate = yearMonth.atEndOfMonth()

            whenever(userRepository.findById(userId)).thenReturn(Optional.of(mockUser))
            whenever(storeScheduleRepository.findDistinctAccountsByFullNameAndStartDateBetween(testUserSfid, startDate, endDate))
                .thenReturn(emptyList())

            // When
            val result = myStoreService.getMyStores(userId, null)

            // Then
            assertThat(result.stores).isEmpty()
            assertThat(result.totalCount).isEqualTo(0)
        }

        @Test
        @DisplayName("사용자 미존재 - 잘못된 userId -> UserNotFoundException 예외")
        fun getMyStores_userNotFound() {
            // Given
            val userId = 999L
            whenever(userRepository.findById(userId)).thenReturn(Optional.empty())

            // When & Then
            assertThatThrownBy { myStoreService.getMyStores(userId, null) }
                .isInstanceOf(UserNotFoundException::class.java)
        }

        @Test
        @DisplayName("검색 — 거래처명 - '경산' 검색 -> '(유)경산식품' 매칭")
        fun getMyStores_searchByStoreName() {
            // Given
            val userId = 1L
            val keyword = "경산"
            val mockUser = createUser(id = userId, sfid = testUserSfid)
            val now = LocalDate.now()
            val yearMonth = YearMonth.from(now)
            val startDate = yearMonth.atDay(1)
            val endDate = yearMonth.atEndOfMonth()

            val distinctAccountSfids = listOf("SF_ACC001", "SF_ACC002")
            val accounts = listOf(
                createAccount(id = 1L, sfid = "SF_ACC001", name = "(유)경산식품", externalKey = "1025172"),
                createAccount(id = 2L, sfid = "SF_ACC002", name = "(주)대한식품", externalKey = "1025173")
            )
            val schedules = listOf(
                createStoreSchedule(account = "SF_ACC001", startDate = now),
                createStoreSchedule(account = "SF_ACC002", startDate = now)
            )

            whenever(userRepository.findById(userId)).thenReturn(Optional.of(mockUser))
            whenever(storeScheduleRepository.findDistinctAccountsByFullNameAndStartDateBetween(testUserSfid, startDate, endDate))
                .thenReturn(distinctAccountSfids)
            whenever(accountRepository.findAll()).thenReturn(accounts)
            whenever(storeScheduleRepository.findByFullNameAndStartDateBetween(testUserSfid, startDate, endDate))
                .thenReturn(schedules)

            // When
            val result = myStoreService.getMyStores(userId, keyword)

            // Then
            assertThat(result.stores).hasSize(1)
            assertThat(result.stores[0].storeName).isEqualTo("(유)경산식품")
        }

        @Test
        @DisplayName("거래처명 기준 오름차순 정렬")
        fun getMyStores_sortedByStoreName() {
            // Given
            val userId = 1L
            val mockUser = createUser(id = userId, sfid = testUserSfid)
            val now = LocalDate.now()
            val yearMonth = YearMonth.from(now)
            val startDate = yearMonth.atDay(1)
            val endDate = yearMonth.atEndOfMonth()

            val distinctAccountSfids = listOf("SF_ACC001", "SF_ACC002", "SF_ACC003")
            val accounts = listOf(
                createAccount(id = 1L, sfid = "SF_ACC001", name = "홈플러스 서면점", externalKey = "1025173"),
                createAccount(id = 2L, sfid = "SF_ACC002", name = "가나다식품", externalKey = "1025172"),
                createAccount(id = 3L, sfid = "SF_ACC003", name = "나라마트", externalKey = "1025174")
            )
            val schedules = listOf(
                createStoreSchedule(account = "SF_ACC001", startDate = now),
                createStoreSchedule(account = "SF_ACC002", startDate = now),
                createStoreSchedule(account = "SF_ACC003", startDate = now)
            )

            whenever(userRepository.findById(userId)).thenReturn(Optional.of(mockUser))
            whenever(storeScheduleRepository.findDistinctAccountsByFullNameAndStartDateBetween(testUserSfid, startDate, endDate))
                .thenReturn(distinctAccountSfids)
            whenever(accountRepository.findAll()).thenReturn(accounts)
            whenever(storeScheduleRepository.findByFullNameAndStartDateBetween(testUserSfid, startDate, endDate))
                .thenReturn(schedules)

            // When
            val result = myStoreService.getMyStores(userId, null)

            // Then
            assertThat(result.stores).hasSize(3)
            assertThat(result.stores[0].storeName).isEqualTo("가나다식품")
            assertThat(result.stores[1].storeName).isEqualTo("나라마트")
            assertThat(result.stores[2].storeName).isEqualTo("홈플러스 서면점")
        }
    }

    // ========== Helpers ==========

    private fun createUser(
        id: Long = 1L,
        employeeId: String = "12345678",
        sfid: String? = null
    ): User {
        return User(
            id = id,
            employeeId = employeeId,
            password = "encodedPassword",
            name = "테스트 사용자",
            orgName = "부산지점",
            passwordChangeRequired = false,
            sfid = sfid
        )
    }

    private fun createAccount(
        id: Long = 1L,
        sfid: String? = null,
        externalKey: String = "1025172",
        name: String = "(유)경산식품",
        address1: String? = "전라남도 목포시",
        representative: String? = "김정자",
        phone: String? = "061-123-4567"
    ): Account {
        return Account(
            id = id,
            sfid = sfid,
            externalKey = externalKey,
            name = name,
            address1 = address1,
            representative = representative,
            phone = phone
        )
    }

    private fun createStoreSchedule(
        account: String = "SF_ACC001",
        typeOfWork1: String = "진열",
        startDate: LocalDate = LocalDate.now()
    ): StoreSchedule {
        return StoreSchedule(
            fullName = testUserSfid,
            account = account,
            typeOfWork1 = typeOfWork1,
            startDate = startDate
        )
    }
}
