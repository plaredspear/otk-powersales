package com.otoki.internal.service

import com.otoki.internal.entity.Store
import com.otoki.internal.entity.StoreSchedule
import com.otoki.internal.entity.User
import com.otoki.internal.exception.UserNotFoundException
import com.otoki.internal.repository.StoreRepository
import com.otoki.internal.repository.StoreScheduleRepository
import com.otoki.internal.repository.UserRepository
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.whenever
import java.time.LocalDate
import java.time.YearMonth

@ExtendWith(MockitoExtension::class)
@DisplayName("MyStoreService 테스트")
class MyStoreServiceTest {

    @Mock
    private lateinit var userRepository: UserRepository

    @Mock
    private lateinit var storeScheduleRepository: StoreScheduleRepository

    @Mock
    private lateinit var storeRepository: StoreRepository

    @InjectMocks
    private lateinit var myStoreService: MyStoreService

    // ========== getMyStores Tests ==========

    @Nested
    @DisplayName("getMyStores - 내 거래처 목록 조회")
    inner class GetMyStoresTests {

        @Test
        @DisplayName("한 달 거래처 조회 - 스케줄 데이터 존재 -> 중복 제거된 거래처 목록 반환")
        fun getMyStores_withScheduleData() {
            // Given
            val userId = 1L
            val now = LocalDate.now()
            val yearMonth = YearMonth.from(now)
            val startDate = yearMonth.atDay(1)
            val endDate = yearMonth.atEndOfMonth()

            val distinctStoreIds = listOf(1025172L, 1025173L, 1025174L)
            val stores = listOf(
                createStore(
                    id = 1025172L,
                    storeCode = "1025172",
                    storeName = "(유)경산식품",
                    address = "전라남도 목포시 임암로20번길 6",
                    representativeName = "김정자",
                    phoneNumber = "061-123-4567"
                ),
                createStore(
                    id = 1025173L,
                    storeCode = "1025173",
                    storeName = "(주)대한식품",
                    address = "서울시 강남구",
                    representativeName = "이영희",
                    phoneNumber = "02-111-2222"
                ),
                createStore(
                    id = 1025174L,
                    storeCode = "1025174",
                    storeName = "부산마트",
                    address = "부산시 해운대구",
                    representativeName = "박철수",
                    phoneNumber = "051-333-4444"
                )
            )
            val schedules = listOf(
                createStoreSchedule(userId = userId, storeId = 1025172L, storeName = "(유)경산식품", storeCode = "1025172"),
                createStoreSchedule(userId = userId, storeId = 1025173L, storeName = "(주)대한식품", storeCode = "1025173"),
                createStoreSchedule(userId = userId, storeId = 1025174L, storeName = "부산마트", storeCode = "1025174")
            )

            whenever(userRepository.existsById(userId)).thenReturn(true)
            whenever(storeScheduleRepository.findDistinctStoreIdsByUserIdAndScheduleDateBetween(userId, startDate, endDate))
                .thenReturn(distinctStoreIds)
            whenever(storeRepository.findByIdIn(distinctStoreIds)).thenReturn(stores)
            whenever(storeScheduleRepository.findByUserIdAndScheduleDateBetween(userId, startDate, endDate))
                .thenReturn(schedules)

            // When
            val result = myStoreService.getMyStores(userId, null)

            // Then
            assertThat(result.stores).hasSize(3)
            assertThat(result.totalCount).isEqualTo(3)
            assertThat(result.stores[0].storeId).isEqualTo(1025172L)
            assertThat(result.stores[0].storeName).isEqualTo("(유)경산식품")
            assertThat(result.stores[0].storeCode).isEqualTo("1025172")
            assertThat(result.stores[0].address).isEqualTo("전라남도 목포시 임암로20번길 6")
            assertThat(result.stores[0].representativeName).isEqualTo("김정자")
            assertThat(result.stores[0].phoneNumber).isEqualTo("061-123-4567")
        }

        @Test
        @DisplayName("중복 제거 - 동일 거래처가 5일에 스케줄 -> 해당 거래처 1건만 반환")
        fun getMyStores_deduplicatesStores() {
            // Given
            val userId = 1L
            val now = LocalDate.now()
            val yearMonth = YearMonth.from(now)
            val startDate = yearMonth.atDay(1)
            val endDate = yearMonth.atEndOfMonth()

            val distinctStoreIds = listOf(1025172L)
            val stores = listOf(
                createStore(
                    id = 1025172L,
                    storeCode = "1025172",
                    storeName = "(유)경산식품",
                    address = "전라남도 목포시 임암로20번길 6",
                    representativeName = "김정자",
                    phoneNumber = "061-123-4567"
                )
            )
            val schedules = listOf(
                createStoreSchedule(userId = userId, storeId = 1025172L, storeName = "(유)경산식품", storeCode = "1025172", scheduleDate = now.withDayOfMonth(1)),
                createStoreSchedule(userId = userId, storeId = 1025172L, storeName = "(유)경산식품", storeCode = "1025172", scheduleDate = now.withDayOfMonth(5)),
                createStoreSchedule(userId = userId, storeId = 1025172L, storeName = "(유)경산식품", storeCode = "1025172", scheduleDate = now.withDayOfMonth(10)),
                createStoreSchedule(userId = userId, storeId = 1025172L, storeName = "(유)경산식품", storeCode = "1025172", scheduleDate = now.withDayOfMonth(15)),
                createStoreSchedule(userId = userId, storeId = 1025172L, storeName = "(유)경산식품", storeCode = "1025172", scheduleDate = now.withDayOfMonth(20))
            )

            whenever(userRepository.existsById(userId)).thenReturn(true)
            whenever(storeScheduleRepository.findDistinctStoreIdsByUserIdAndScheduleDateBetween(userId, startDate, endDate))
                .thenReturn(distinctStoreIds)
            whenever(storeRepository.findByIdIn(distinctStoreIds)).thenReturn(stores)
            whenever(storeScheduleRepository.findByUserIdAndScheduleDateBetween(userId, startDate, endDate))
                .thenReturn(schedules)

            // When
            val result = myStoreService.getMyStores(userId, null)

            // Then
            assertThat(result.stores).hasSize(1)
            assertThat(result.totalCount).isEqualTo(1)
            assertThat(result.stores[0].storeId).isEqualTo(1025172L)
            assertThat(result.stores[0].storeName).isEqualTo("(유)경산식품")
        }

        @Test
        @DisplayName("검색 — 거래처명 - '경산' 검색 -> '(유)경산식품' 매칭")
        fun getMyStores_searchByStoreName() {
            // Given
            val userId = 1L
            val keyword = "경산"
            val now = LocalDate.now()
            val yearMonth = YearMonth.from(now)
            val startDate = yearMonth.atDay(1)
            val endDate = yearMonth.atEndOfMonth()

            val distinctStoreIds = listOf(1025172L, 1025173L)
            val stores = listOf(
                createStore(
                    id = 1025172L,
                    storeCode = "1025172",
                    storeName = "(유)경산식품",
                    address = "전라남도 목포시 임암로20번길 6",
                    representativeName = "김정자",
                    phoneNumber = "061-123-4567"
                ),
                createStore(
                    id = 1025173L,
                    storeCode = "1025173",
                    storeName = "(주)대한식품",
                    address = "서울시 강남구",
                    representativeName = "이영희",
                    phoneNumber = "02-111-2222"
                )
            )
            val schedules = listOf(
                createStoreSchedule(userId = userId, storeId = 1025172L, storeName = "(유)경산식품", storeCode = "1025172"),
                createStoreSchedule(userId = userId, storeId = 1025173L, storeName = "(주)대한식품", storeCode = "1025173")
            )

            whenever(userRepository.existsById(userId)).thenReturn(true)
            whenever(storeScheduleRepository.findDistinctStoreIdsByUserIdAndScheduleDateBetween(userId, startDate, endDate))
                .thenReturn(distinctStoreIds)
            whenever(storeRepository.findByIdIn(distinctStoreIds)).thenReturn(stores)
            whenever(storeScheduleRepository.findByUserIdAndScheduleDateBetween(userId, startDate, endDate))
                .thenReturn(schedules)

            // When
            val result = myStoreService.getMyStores(userId, keyword)

            // Then
            assertThat(result.stores).hasSize(1)
            assertThat(result.totalCount).isEqualTo(1)
            assertThat(result.stores[0].storeName).isEqualTo("(유)경산식품")
        }

        @Test
        @DisplayName("검색 — 거래처코드 - '1025172' 검색 -> 해당 코드 거래처 매칭")
        fun getMyStores_searchByStoreCode() {
            // Given
            val userId = 1L
            val keyword = "1025172"
            val now = LocalDate.now()
            val yearMonth = YearMonth.from(now)
            val startDate = yearMonth.atDay(1)
            val endDate = yearMonth.atEndOfMonth()

            val distinctStoreIds = listOf(1025172L, 1025173L)
            val stores = listOf(
                createStore(
                    id = 1025172L,
                    storeCode = "1025172",
                    storeName = "(유)경산식품",
                    address = "전라남도 목포시 임암로20번길 6",
                    representativeName = "김정자",
                    phoneNumber = "061-123-4567"
                ),
                createStore(
                    id = 1025173L,
                    storeCode = "1025173",
                    storeName = "(주)대한식품",
                    address = "서울시 강남구",
                    representativeName = "이영희",
                    phoneNumber = "02-111-2222"
                )
            )
            val schedules = listOf(
                createStoreSchedule(userId = userId, storeId = 1025172L, storeName = "(유)경산식품", storeCode = "1025172"),
                createStoreSchedule(userId = userId, storeId = 1025173L, storeName = "(주)대한식품", storeCode = "1025173")
            )

            whenever(userRepository.existsById(userId)).thenReturn(true)
            whenever(storeScheduleRepository.findDistinctStoreIdsByUserIdAndScheduleDateBetween(userId, startDate, endDate))
                .thenReturn(distinctStoreIds)
            whenever(storeRepository.findByIdIn(distinctStoreIds)).thenReturn(stores)
            whenever(storeScheduleRepository.findByUserIdAndScheduleDateBetween(userId, startDate, endDate))
                .thenReturn(schedules)

            // When
            val result = myStoreService.getMyStores(userId, keyword)

            // Then
            assertThat(result.stores).hasSize(1)
            assertThat(result.totalCount).isEqualTo(1)
            assertThat(result.stores[0].storeCode).isEqualTo("1025172")
            assertThat(result.stores[0].storeName).isEqualTo("(유)경산식품")
        }

        @Test
        @DisplayName("결과 없음 - 스케줄 데이터 없음 -> 빈 리스트 + totalCount=0")
        fun getMyStores_noSchedules() {
            // Given
            val userId = 1L
            val now = LocalDate.now()
            val yearMonth = YearMonth.from(now)
            val startDate = yearMonth.atDay(1)
            val endDate = yearMonth.atEndOfMonth()

            whenever(userRepository.existsById(userId)).thenReturn(true)
            whenever(storeScheduleRepository.findDistinctStoreIdsByUserIdAndScheduleDateBetween(userId, startDate, endDate))
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
            whenever(userRepository.existsById(userId)).thenReturn(false)

            // When & Then
            assertThatThrownBy { myStoreService.getMyStores(userId, null) }
                .isInstanceOf(UserNotFoundException::class.java)
        }

        @Test
        @DisplayName("Store 마스터 정보 병합 - Store 마스터가 있으면 대표자명/전화번호 포함")
        fun getMyStores_withStoreMaster() {
            // Given
            val userId = 1L
            val now = LocalDate.now()
            val yearMonth = YearMonth.from(now)
            val startDate = yearMonth.atDay(1)
            val endDate = yearMonth.atEndOfMonth()

            val distinctStoreIds = listOf(1025172L)
            val stores = listOf(
                createStore(
                    id = 1025172L,
                    storeCode = "1025172",
                    storeName = "(유)경산식품",
                    address = "전라남도 목포시 임암로20번길 6",
                    representativeName = "김정자",
                    phoneNumber = "061-123-4567"
                )
            )
            val schedules = listOf(
                createStoreSchedule(userId = userId, storeId = 1025172L, storeName = "(유)경산식품", storeCode = "1025172")
            )

            whenever(userRepository.existsById(userId)).thenReturn(true)
            whenever(storeScheduleRepository.findDistinctStoreIdsByUserIdAndScheduleDateBetween(userId, startDate, endDate))
                .thenReturn(distinctStoreIds)
            whenever(storeRepository.findByIdIn(distinctStoreIds)).thenReturn(stores)
            whenever(storeScheduleRepository.findByUserIdAndScheduleDateBetween(userId, startDate, endDate))
                .thenReturn(schedules)

            // When
            val result = myStoreService.getMyStores(userId, null)

            // Then
            assertThat(result.stores).hasSize(1)
            assertThat(result.stores[0].representativeName).isEqualTo("김정자")
            assertThat(result.stores[0].phoneNumber).isEqualTo("061-123-4567")
        }

        @Test
        @DisplayName("Store 마스터 없는 fallback - Store 마스터 없으면 StoreSchedule 정보만 사용 (representativeName/phoneNumber null)")
        fun getMyStores_withoutStoreMaster() {
            // Given
            val userId = 1L
            val now = LocalDate.now()
            val yearMonth = YearMonth.from(now)
            val startDate = yearMonth.atDay(1)
            val endDate = yearMonth.atEndOfMonth()

            val distinctStoreIds = listOf(1025172L)
            val stores = emptyList<Store>()
            val schedules = listOf(
                createStoreSchedule(
                    userId = userId,
                    storeId = 1025172L,
                    storeName = "(유)경산식품",
                    storeCode = "1025172",
                    address = "전라남도 목포시"
                )
            )

            whenever(userRepository.existsById(userId)).thenReturn(true)
            whenever(storeScheduleRepository.findDistinctStoreIdsByUserIdAndScheduleDateBetween(userId, startDate, endDate))
                .thenReturn(distinctStoreIds)
            whenever(storeRepository.findByIdIn(distinctStoreIds)).thenReturn(stores)
            whenever(storeScheduleRepository.findByUserIdAndScheduleDateBetween(userId, startDate, endDate))
                .thenReturn(schedules)

            // When
            val result = myStoreService.getMyStores(userId, null)

            // Then
            assertThat(result.stores).hasSize(1)
            assertThat(result.stores[0].storeId).isEqualTo(1025172L)
            assertThat(result.stores[0].storeName).isEqualTo("(유)경산식품")
            assertThat(result.stores[0].storeCode).isEqualTo("1025172")
            assertThat(result.stores[0].address).isEqualTo("전라남도 목포시")
            assertThat(result.stores[0].representativeName).isNull()
            assertThat(result.stores[0].phoneNumber).isNull()
        }

        @Test
        @DisplayName("거래처명 기준 오름차순 정렬 - 결과가 storeName 오름차순으로 정렬됨")
        fun getMyStores_sortedByStoreName() {
            // Given
            val userId = 1L
            val now = LocalDate.now()
            val yearMonth = YearMonth.from(now)
            val startDate = yearMonth.atDay(1)
            val endDate = yearMonth.atEndOfMonth()

            val distinctStoreIds = listOf(1025173L, 1025172L, 1025174L)
            val stores = listOf(
                createStore(
                    id = 1025173L,
                    storeCode = "1025173",
                    storeName = "홈플러스 서면점",
                    address = "부산시 서면",
                    representativeName = "이영희",
                    phoneNumber = "02-111-2222"
                ),
                createStore(
                    id = 1025172L,
                    storeCode = "1025172",
                    storeName = "가나다식품",
                    address = "전라남도 목포시",
                    representativeName = "김정자",
                    phoneNumber = "061-123-4567"
                ),
                createStore(
                    id = 1025174L,
                    storeCode = "1025174",
                    storeName = "나라마트",
                    address = "부산시 해운대구",
                    representativeName = "박철수",
                    phoneNumber = "051-333-4444"
                )
            )
            val schedules = listOf(
                createStoreSchedule(userId = userId, storeId = 1025173L, storeName = "홈플러스 서면점", storeCode = "1025173"),
                createStoreSchedule(userId = userId, storeId = 1025172L, storeName = "가나다식품", storeCode = "1025172"),
                createStoreSchedule(userId = userId, storeId = 1025174L, storeName = "나라마트", storeCode = "1025174")
            )

            whenever(userRepository.existsById(userId)).thenReturn(true)
            whenever(storeScheduleRepository.findDistinctStoreIdsByUserIdAndScheduleDateBetween(userId, startDate, endDate))
                .thenReturn(distinctStoreIds)
            whenever(storeRepository.findByIdIn(distinctStoreIds)).thenReturn(stores)
            whenever(storeScheduleRepository.findByUserIdAndScheduleDateBetween(userId, startDate, endDate))
                .thenReturn(schedules)

            // When
            val result = myStoreService.getMyStores(userId, null)

            // Then
            assertThat(result.stores).hasSize(3)
            assertThat(result.stores[0].storeName).isEqualTo("가나다식품")
            assertThat(result.stores[1].storeName).isEqualTo("나라마트")
            assertThat(result.stores[2].storeName).isEqualTo("홈플러스 서면점")
        }

        @Test
        @DisplayName("검색 키워드 대소문자 무시 - 'KYUNGSAN' 검색 -> '경산식품' 매칭되지 않음 (한글-영문 불일치)")
        fun getMyStores_searchCaseInsensitive() {
            // Given
            val userId = 1L
            val keyword = "GYEONGSAN"
            val now = LocalDate.now()
            val yearMonth = YearMonth.from(now)
            val startDate = yearMonth.atDay(1)
            val endDate = yearMonth.atEndOfMonth()

            val distinctStoreIds = listOf(1025172L)
            val stores = listOf(
                createStore(
                    id = 1025172L,
                    storeCode = "1025172",
                    storeName = "(유)경산식품",
                    address = "전라남도 목포시",
                    representativeName = "김정자",
                    phoneNumber = "061-123-4567"
                )
            )
            val schedules = listOf(
                createStoreSchedule(userId = userId, storeId = 1025172L, storeName = "(유)경산식품", storeCode = "1025172")
            )

            whenever(userRepository.existsById(userId)).thenReturn(true)
            whenever(storeScheduleRepository.findDistinctStoreIdsByUserIdAndScheduleDateBetween(userId, startDate, endDate))
                .thenReturn(distinctStoreIds)
            whenever(storeRepository.findByIdIn(distinctStoreIds)).thenReturn(stores)
            whenever(storeScheduleRepository.findByUserIdAndScheduleDateBetween(userId, startDate, endDate))
                .thenReturn(schedules)

            // When
            val result = myStoreService.getMyStores(userId, keyword)

            // Then
            assertThat(result.stores).isEmpty()
            assertThat(result.totalCount).isEqualTo(0)
        }

        @Test
        @DisplayName("검색 키워드가 공백인 경우 - 필터링하지 않고 전체 결과 반환")
        fun getMyStores_searchWithBlankKeyword() {
            // Given
            val userId = 1L
            val keyword = "   "
            val now = LocalDate.now()
            val yearMonth = YearMonth.from(now)
            val startDate = yearMonth.atDay(1)
            val endDate = yearMonth.atEndOfMonth()

            val distinctStoreIds = listOf(1025172L, 1025173L)
            val stores = listOf(
                createStore(
                    id = 1025172L,
                    storeCode = "1025172",
                    storeName = "(유)경산식품",
                    address = "전라남도 목포시",
                    representativeName = "김정자",
                    phoneNumber = "061-123-4567"
                ),
                createStore(
                    id = 1025173L,
                    storeCode = "1025173",
                    storeName = "(주)대한식품",
                    address = "서울시 강남구",
                    representativeName = "이영희",
                    phoneNumber = "02-111-2222"
                )
            )
            val schedules = listOf(
                createStoreSchedule(userId = userId, storeId = 1025172L, storeName = "(유)경산식품", storeCode = "1025172"),
                createStoreSchedule(userId = userId, storeId = 1025173L, storeName = "(주)대한식품", storeCode = "1025173")
            )

            whenever(userRepository.existsById(userId)).thenReturn(true)
            whenever(storeScheduleRepository.findDistinctStoreIdsByUserIdAndScheduleDateBetween(userId, startDate, endDate))
                .thenReturn(distinctStoreIds)
            whenever(storeRepository.findByIdIn(distinctStoreIds)).thenReturn(stores)
            whenever(storeScheduleRepository.findByUserIdAndScheduleDateBetween(userId, startDate, endDate))
                .thenReturn(schedules)

            // When
            val result = myStoreService.getMyStores(userId, keyword)

            // Then
            assertThat(result.stores).hasSize(2)
            assertThat(result.totalCount).isEqualTo(2)
        }
    }

    // ========== Helpers ==========

    private fun createUser(
        id: Long = 1L,
        employeeId: String = "12345678"
    ): User {
        return User(
            id = id,
            employeeId = employeeId,
            password = "encodedPassword",
            name = "테스트 사용자",
            orgName = "부산지점",
            passwordChangeRequired = false
        )
    }

    private fun createStore(
        id: Long = 1025172L,
        storeCode: String = "1025172",
        storeName: String = "(유)경산식품",
        address: String? = "전라남도 목포시 임암로20번길 6",
        representativeName: String? = "김정자",
        phoneNumber: String? = "061-123-4567"
    ): Store {
        return Store(
            id = id,
            storeCode = storeCode,
            storeName = storeName,
            address = address,
            representativeName = representativeName,
            phoneNumber = phoneNumber
        )
    }

    private fun createStoreSchedule(
        userId: Long = 1L,
        storeId: Long = 1025172L,
        storeName: String = "(유)경산식품",
        storeCode: String = "1025172",
        address: String? = "전라남도 목포시",
        scheduleDate: LocalDate = LocalDate.now()
    ): StoreSchedule {
        return StoreSchedule(
            userId = userId,
            storeId = storeId,
            storeName = storeName,
            storeCode = storeCode,
            workCategory = "진열",
            address = address,
            scheduleDate = scheduleDate
        )
    }
}
