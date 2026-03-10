package com.otoki.internal.admin.service

import com.otoki.internal.admin.dto.DataScope
import com.otoki.internal.admin.scope.DataScopeHolder
import com.otoki.internal.sap.entity.User
import com.otoki.internal.sap.repository.UserRepository
import com.otoki.internal.sap.entity.Account
import com.otoki.internal.sap.repository.AccountRepository
import com.otoki.internal.sap.entity.MonthlySalesHistory
import com.otoki.internal.sap.repository.MonthlySalesHistoryRepository
import com.otoki.internal.common.entity.StoreSchedule
import com.otoki.internal.common.repository.StoreScheduleRepository
import org.assertj.core.api.Assertions.assertThat
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

@ExtendWith(MockitoExtension::class)
@DisplayName("AdminDashboardService 테스트")
class AdminDashboardServiceTest {

    @Mock
    private lateinit var dataScopeHolder: DataScopeHolder

    @Mock
    private lateinit var monthlySalesHistoryRepository: MonthlySalesHistoryRepository

    @Mock
    private lateinit var storeScheduleRepository: StoreScheduleRepository

    @Mock
    private lateinit var accountRepository: AccountRepository

    @Mock
    private lateinit var userRepository: UserRepository

    @InjectMocks
    private lateinit var adminDashboardService: AdminDashboardService

    @Nested
    @DisplayName("getDashboard - 대시보드 조회")
    inner class GetDashboardTests {

        // ========== 기본 조회 (전체 범위) ==========

        @Test
        @DisplayName("전체 범위 - 영업본부장(isAllBranches=true) -> 전체 매출/인원 데이터 반환")
        fun allBranches_returnsAllData() {
            // Given
            val yearMonth = "2026-03"
            val scope = DataScope(branchCodes = emptyList(), isAllBranches = true)

            val salesData = listOf(
                createSalesHistory(targetMonthResults = 1000.0, shipClosingAmount = 800.0,
                    accountExternalKey = "EK001"),
                createSalesHistory(targetMonthResults = 2000.0, shipClosingAmount = 1500.0,
                    accountExternalKey = "EK002")
            )
            val lastYearSalesData = listOf(
                createSalesHistory(shipClosingAmount = 600.0, accountExternalKey = "EK001"),
                createSalesHistory(shipClosingAmount = 1000.0, accountExternalKey = "EK002")
            )

            val accounts = listOf(
                createAccount(sfid = "ACC001", externalKey = "EK001", abcType = "대형마트"),
                createAccount(sfid = "ACC002", externalKey = "EK002", abcType = "슈퍼")
            )

            val schedules = listOf(
                createStoreSchedule(accountSfid = "ACC001", employeeSfid = "EMP001", typeOfWork1 = "고정"),
                createStoreSchedule(accountSfid = "ACC002", employeeSfid = "EMP002", typeOfWork1 = "순회")
            )
            val prevSchedules = listOf(
                createStoreSchedule(accountSfid = "ACC001", employeeSfid = "EMP001", typeOfWork1 = "고정")
            )

            val activeUsers = listOf(
                createUser(id = 10L, sfid = "EMP001", status = "재직", costCenterCode = "B001",
                    birthDate = "1990-05-15"),
                createUser(id = 11L, sfid = "EMP002", status = "재직", costCenterCode = "B002",
                    birthDate = "1985-03-20")
            )
            val onLeaveUsers = listOf(
                createUser(id = 12L, sfid = "EMP003", status = "휴직", costCenterCode = "B001")
            )

            whenever(dataScopeHolder.require()).thenReturn(scope)

            // Sales: current year
            whenever(monthlySalesHistoryRepository.findBySalesYearAndSalesMonth("2026", "03"))
                .thenReturn(salesData)
            // Sales: last year
            whenever(monthlySalesHistoryRepository.findBySalesYearAndSalesMonth("2025", "03"))
                .thenReturn(lastYearSalesData)

            // Accounts for channel classification
            whenever(accountRepository.findAll()).thenReturn(accounts)

            // Schedules: current month
            whenever(storeScheduleRepository.findByConfirmedTrueAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
                eq(LocalDate.of(2026, 3, 31)), eq(LocalDate.of(2026, 3, 1))
            )).thenReturn(schedules)
            // Schedules: previous month
            whenever(storeScheduleRepository.findByConfirmedTrueAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
                eq(LocalDate.of(2026, 2, 28)), eq(LocalDate.of(2026, 2, 1))
            )).thenReturn(prevSchedules)

            // Account lookup for schedule accountSfids
            whenever(accountRepository.findBySfidIn(any())).thenReturn(accounts)

            // Users
            whenever(userRepository.findByStatus("재직")).thenReturn(activeUsers)
            whenever(userRepository.findByStatus("휴직")).thenReturn(onLeaveUsers)

            // When
            val result = adminDashboardService.getDashboard(yearMonth, null)

            // Then - salesSummary
            assertThat(result.salesSummary.yearMonth).isEqualTo("2026-03")
            assertThat(result.salesSummary.branchName).isNull()
            assertThat(result.salesSummary.targetAmount).isEqualTo(3000L)
            assertThat(result.salesSummary.actualAmount).isEqualTo(2300L)
            assertThat(result.salesSummary.progressRate).isGreaterThan(0.0)

            // Then - lastYear comparison
            assertThat(result.salesSummary.lastYearAmount).isEqualTo(1600L)
            assertThat(result.salesSummary.lastYearRatio).isGreaterThan(0.0)

            // Then - staffDeployment
            assertThat(result.staffDeployment.yearMonth).isEqualTo("2026-03")
            assertThat(result.staffDeployment.branchName).isNull()

            // Then - basicStats
            assertThat(result.basicStats.branchName).isNull()
            assertThat(result.basicStats.staffType.promotion).isEqualTo(2)
            assertThat(result.basicStats.staffType.osc).isEqualTo(0)
            assertThat(result.basicStats.totalByPosition.active).isEqualTo(2)
            assertThat(result.basicStats.totalByPosition.onLeave).isEqualTo(1)
        }

        // ========== 지점 범위 ==========

        @Test
        @DisplayName("지점 범위 - 조장(branchCodes 제한) -> 해당 지점 데이터만 반환")
        fun branchScope_returnsFilteredData() {
            // Given
            val yearMonth = "2026-03"
            val scope = DataScope(branchCodes = listOf("B001"), isAllBranches = false)

            val branchAccounts = listOf(
                createAccount(sfid = "ACC001", externalKey = "EK001", branchCode = "B001",
                    branchName = "서울지점", abcType = "대형마트")
            )

            val salesData = listOf(
                createSalesHistory(targetMonthResults = 500.0, shipClosingAmount = 300.0,
                    accountExternalKey = "EK001")
            )
            val lastYearSalesData = listOf(
                createSalesHistory(shipClosingAmount = 250.0, accountExternalKey = "EK001")
            )

            val schedules = listOf(
                createStoreSchedule(accountSfid = "ACC001", employeeSfid = "EMP001", typeOfWork1 = "고정")
            )

            val activeUsers = listOf(
                createUser(id = 10L, sfid = "EMP001", status = "재직", costCenterCode = "B001",
                    birthDate = "1992-07-10")
            )

            whenever(dataScopeHolder.require()).thenReturn(scope)

            // Branch name resolution
            whenever(accountRepository.findByBranchCodeIn(listOf("B001"))).thenReturn(branchAccounts)

            // Sales (filtered by externalKeys)
            whenever(monthlySalesHistoryRepository.findBySalesYearAndSalesMonthAndAccountExternalKeyIn(
                eq("2026"), eq("03"), eq(listOf("EK001"))
            )).thenReturn(salesData)
            whenever(monthlySalesHistoryRepository.findBySalesYearAndSalesMonthAndAccountExternalKeyIn(
                eq("2025"), eq("03"), eq(listOf("EK001"))
            )).thenReturn(lastYearSalesData)

            // Accounts for channel classification
            whenever(accountRepository.findAll()).thenReturn(branchAccounts)

            // Schedules (filtered by accountSfids)
            whenever(storeScheduleRepository.findByConfirmedTrueAndStartDateLessThanEqualAndEndDateGreaterThanEqualAndAccountIn(
                eq(LocalDate.of(2026, 3, 31)), eq(LocalDate.of(2026, 3, 1)), eq(listOf("ACC001"))
            )).thenReturn(schedules)
            whenever(storeScheduleRepository.findByConfirmedTrueAndStartDateLessThanEqualAndEndDateGreaterThanEqualAndAccountIn(
                eq(LocalDate.of(2026, 2, 28)), eq(LocalDate.of(2026, 2, 1)), eq(listOf("ACC001"))
            )).thenReturn(emptyList())

            // Account lookup for schedule accountSfids
            whenever(accountRepository.findBySfidIn(any())).thenReturn(branchAccounts)

            // Users (filtered by costCenterCode)
            whenever(userRepository.findByCostCenterCodeInAndStatus(listOf("B001"), "재직"))
                .thenReturn(activeUsers)
            whenever(userRepository.findByCostCenterCodeInAndStatus(listOf("B001"), "휴직"))
                .thenReturn(emptyList())

            // When
            val result = adminDashboardService.getDashboard(yearMonth, null)

            // Then
            assertThat(result.salesSummary.branchName).isEqualTo("서울지점")
            assertThat(result.salesSummary.targetAmount).isEqualTo(500L)
            assertThat(result.salesSummary.actualAmount).isEqualTo(300L)
            assertThat(result.salesSummary.lastYearAmount).isEqualTo(250L)
            assertThat(result.basicStats.branchName).isEqualTo("서울지점")
            assertThat(result.basicStats.totalByPosition.active).isEqualTo(1)
            assertThat(result.basicStats.totalByPosition.onLeave).isEqualTo(0)
        }

        // ========== 데이터 없음 ==========

        @Test
        @DisplayName("데이터 없음 - 매출/스케줄 데이터 없음 -> 금액 0, 카운트 0")
        fun noData_returnsZeros() {
            // Given
            val yearMonth = "2026-03"
            val scope = DataScope(branchCodes = emptyList(), isAllBranches = true)

            whenever(dataScopeHolder.require()).thenReturn(scope)

            // No sales data
            whenever(monthlySalesHistoryRepository.findBySalesYearAndSalesMonth("2026", "03"))
                .thenReturn(emptyList())
            whenever(monthlySalesHistoryRepository.findBySalesYearAndSalesMonth("2025", "03"))
                .thenReturn(emptyList())

            // No schedule data
            whenever(storeScheduleRepository.findByConfirmedTrueAndStartDateLessThanEqualAndEndDateGreaterThanEqual(any(), any()))
                .thenReturn(emptyList())

            // No users
            whenever(userRepository.findByStatus("재직")).thenReturn(emptyList())
            whenever(userRepository.findByStatus("휴직")).thenReturn(emptyList())

            // When
            val result = adminDashboardService.getDashboard(yearMonth, null)

            // Then - salesSummary
            assertThat(result.salesSummary.targetAmount).isEqualTo(0L)
            assertThat(result.salesSummary.actualAmount).isEqualTo(0L)
            assertThat(result.salesSummary.progressRate).isEqualTo(0.0)
            assertThat(result.salesSummary.lastYearAmount).isEqualTo(0L)
            assertThat(result.salesSummary.lastYearRatio).isEqualTo(0.0)
            assertThat(result.salesSummary.channelSales).isEmpty()

            // Then - staffDeployment
            assertThat(result.staffDeployment.byAccountType).isEmpty()
            assertThat(result.staffDeployment.byWorkType).isEmpty()
            assertThat(result.staffDeployment.byChannelAndWorkType).isEmpty()

            // Then - basicStats
            assertThat(result.basicStats.staffType.promotion).isEqualTo(0)
            assertThat(result.basicStats.totalByPosition.active).isEqualTo(0)
            assertThat(result.basicStats.totalByPosition.onLeave).isEqualTo(0)
            assertThat(result.basicStats.byAgeGroup).isEmpty()
            assertThat(result.basicStats.byWorkType.fixed).isEqualTo(0)
            assertThat(result.basicStats.byWorkType.alternating).isEqualTo(0)
            assertThat(result.basicStats.byWorkType.visiting).isEqualTo(0)
        }

        // ========== branchCode 파라미터 적용 ==========

        @Test
        @DisplayName("branchCode 파라미터 - 전체범위에서 특정 지점 필터 -> 해당 지점만 조회")
        fun branchCodeParam_filtersAllBranchesScope() {
            // Given
            val yearMonth = "2026-03"
            val allScope = DataScope(branchCodes = emptyList(), isAllBranches = true)
            // branchCode 파라미터로 "B002" 지정 -> effectiveScope = DataScope(listOf("B002"), false)

            val filteredAccounts = listOf(
                createAccount(sfid = "ACC002", externalKey = "EK002", branchCode = "B002",
                    branchName = "부산지점", abcType = "편의점")
            )

            val salesData = listOf(
                createSalesHistory(targetMonthResults = 700.0, shipClosingAmount = 400.0,
                    accountExternalKey = "EK002")
            )

            whenever(dataScopeHolder.require()).thenReturn(allScope)

            // Branch name resolution for effective scope
            whenever(accountRepository.findByBranchCodeIn(listOf("B002"))).thenReturn(filteredAccounts)

            // Sales filtered by externalKeys
            whenever(monthlySalesHistoryRepository.findBySalesYearAndSalesMonthAndAccountExternalKeyIn(
                eq("2026"), eq("03"), eq(listOf("EK002"))
            )).thenReturn(salesData)
            whenever(monthlySalesHistoryRepository.findBySalesYearAndSalesMonthAndAccountExternalKeyIn(
                eq("2025"), eq("03"), eq(listOf("EK002"))
            )).thenReturn(emptyList())

            // Accounts for channel classification
            whenever(accountRepository.findAll()).thenReturn(filteredAccounts)

            // Schedules filtered by accountSfids
            whenever(storeScheduleRepository.findByConfirmedTrueAndStartDateLessThanEqualAndEndDateGreaterThanEqualAndAccountIn(
                eq(LocalDate.of(2026, 3, 31)), eq(LocalDate.of(2026, 3, 1)), eq(listOf("ACC002"))
            )).thenReturn(emptyList())
            whenever(storeScheduleRepository.findByConfirmedTrueAndStartDateLessThanEqualAndEndDateGreaterThanEqualAndAccountIn(
                eq(LocalDate.of(2026, 2, 28)), eq(LocalDate.of(2026, 2, 1)), eq(listOf("ACC002"))
            )).thenReturn(emptyList())

            // Users filtered by costCenterCode
            whenever(userRepository.findByCostCenterCodeInAndStatus(listOf("B002"), "재직"))
                .thenReturn(emptyList())
            whenever(userRepository.findByCostCenterCodeInAndStatus(listOf("B002"), "휴직"))
                .thenReturn(emptyList())

            // When
            val result = adminDashboardService.getDashboard(yearMonth, "B002")

            // Then
            assertThat(result.salesSummary.branchName).isEqualTo("부산지점")
            assertThat(result.salesSummary.targetAmount).isEqualTo(700L)
            assertThat(result.salesSummary.actualAmount).isEqualTo(400L)
        }

        @Test
        @DisplayName("branchCode 파라미터 - 지점범위에서 권한 외 지점 요청 -> 원래 scope 유지")
        fun branchCodeParam_outsideScope_ignoresFilter() {
            // Given
            val yearMonth = "2026-03"
            val scope = DataScope(branchCodes = listOf("B001"), isAllBranches = false)
            // branchCode="B999" 요청하지만 scope에 없으므로 원래 scope 유지

            val branchAccounts = listOf(
                createAccount(sfid = "ACC001", externalKey = "EK001", branchCode = "B001",
                    branchName = "서울지점", abcType = "대형마트")
            )

            whenever(dataScopeHolder.require()).thenReturn(scope)

            // Branch name: original scope branchCodes
            whenever(accountRepository.findByBranchCodeIn(listOf("B001"))).thenReturn(branchAccounts)

            // Sales
            whenever(monthlySalesHistoryRepository.findBySalesYearAndSalesMonthAndAccountExternalKeyIn(
                eq("2026"), eq("03"), eq(listOf("EK001"))
            )).thenReturn(emptyList())
            whenever(monthlySalesHistoryRepository.findBySalesYearAndSalesMonthAndAccountExternalKeyIn(
                eq("2025"), eq("03"), eq(listOf("EK001"))
            )).thenReturn(emptyList())

            // Schedules
            whenever(storeScheduleRepository.findByConfirmedTrueAndStartDateLessThanEqualAndEndDateGreaterThanEqualAndAccountIn(
                eq(LocalDate.of(2026, 3, 31)), eq(LocalDate.of(2026, 3, 1)), eq(listOf("ACC001"))
            )).thenReturn(emptyList())
            whenever(storeScheduleRepository.findByConfirmedTrueAndStartDateLessThanEqualAndEndDateGreaterThanEqualAndAccountIn(
                eq(LocalDate.of(2026, 2, 28)), eq(LocalDate.of(2026, 2, 1)), eq(listOf("ACC001"))
            )).thenReturn(emptyList())

            // Users
            whenever(userRepository.findByCostCenterCodeInAndStatus(listOf("B001"), "재직"))
                .thenReturn(emptyList())
            whenever(userRepository.findByCostCenterCodeInAndStatus(listOf("B001"), "휴직"))
                .thenReturn(emptyList())

            // When
            val result = adminDashboardService.getDashboard(yearMonth, "B999")

            // Then - scope was not changed, still uses B001 (branchName = 서울지점)
            assertThat(result.salesSummary.branchName).isEqualTo("서울지점")
        }

        // ========== 전년 비교 ==========

        @Test
        @DisplayName("전년 비교 - 전년 매출 있음 -> lastYearAmount, lastYearRatio 정확히 계산")
        fun lastYearComparison_calculatesRatio() {
            // Given
            val yearMonth = "2026-06"
            val scope = DataScope(branchCodes = emptyList(), isAllBranches = true)

            val currentSales = listOf(
                createSalesHistory(targetMonthResults = 10000.0, shipClosingAmount = 8000.0,
                    accountExternalKey = "EK001")
            )
            val lastYearSales = listOf(
                createSalesHistory(shipClosingAmount = 5000.0, accountExternalKey = "EK001")
            )

            val accounts = listOf(
                createAccount(sfid = "ACC001", externalKey = "EK001", abcType = "대형마트")
            )

            whenever(dataScopeHolder.require()).thenReturn(scope)

            // Current year sales
            whenever(monthlySalesHistoryRepository.findBySalesYearAndSalesMonth("2026", "06"))
                .thenReturn(currentSales)
            // Last year sales
            whenever(monthlySalesHistoryRepository.findBySalesYearAndSalesMonth("2025", "06"))
                .thenReturn(lastYearSales)

            // Accounts for channel classification
            whenever(accountRepository.findAll()).thenReturn(accounts)

            // Schedules
            whenever(storeScheduleRepository.findByConfirmedTrueAndStartDateLessThanEqualAndEndDateGreaterThanEqual(any(), any()))
                .thenReturn(emptyList())

            // Users
            whenever(userRepository.findByStatus("재직")).thenReturn(emptyList())
            whenever(userRepository.findByStatus("휴직")).thenReturn(emptyList())

            // When
            val result = adminDashboardService.getDashboard(yearMonth, null)

            // Then
            assertThat(result.salesSummary.actualAmount).isEqualTo(8000L)
            assertThat(result.salesSummary.lastYearAmount).isEqualTo(5000L)
            // lastYearRatio = 8000 / 5000 * 100 = 160.0
            assertThat(result.salesSummary.lastYearRatio).isEqualTo(160.0)
        }

        @Test
        @DisplayName("전년 비교 - 전년 매출 0 -> lastYearRatio 0.0")
        fun lastYearComparison_zeroLastYear_ratioIsZero() {
            // Given
            val yearMonth = "2026-06"
            val scope = DataScope(branchCodes = emptyList(), isAllBranches = true)

            val currentSales = listOf(
                createSalesHistory(targetMonthResults = 5000.0, shipClosingAmount = 3000.0,
                    accountExternalKey = "EK001")
            )

            whenever(dataScopeHolder.require()).thenReturn(scope)
            whenever(monthlySalesHistoryRepository.findBySalesYearAndSalesMonth("2026", "06"))
                .thenReturn(currentSales)
            whenever(monthlySalesHistoryRepository.findBySalesYearAndSalesMonth("2025", "06"))
                .thenReturn(emptyList())

            whenever(accountRepository.findAll()).thenReturn(emptyList())

            whenever(storeScheduleRepository.findByConfirmedTrueAndStartDateLessThanEqualAndEndDateGreaterThanEqual(any(), any()))
                .thenReturn(emptyList())

            whenever(userRepository.findByStatus("재직")).thenReturn(emptyList())
            whenever(userRepository.findByStatus("휴직")).thenReturn(emptyList())

            // When
            val result = adminDashboardService.getDashboard(yearMonth, null)

            // Then
            assertThat(result.salesSummary.lastYearAmount).isEqualTo(0L)
            assertThat(result.salesSummary.lastYearRatio).isEqualTo(0.0)
        }

        // ========== yearMonth 기본값 ==========

        @Test
        @DisplayName("yearMonth null - yearMonth 미지정 -> 현재 년월 사용")
        fun yearMonthNull_usesCurrentMonth() {
            // Given
            val scope = DataScope(branchCodes = emptyList(), isAllBranches = true)

            whenever(dataScopeHolder.require()).thenReturn(scope)
            whenever(monthlySalesHistoryRepository.findBySalesYearAndSalesMonth(any(), any()))
                .thenReturn(emptyList())
            whenever(storeScheduleRepository.findByConfirmedTrueAndStartDateLessThanEqualAndEndDateGreaterThanEqual(any(), any()))
                .thenReturn(emptyList())
            whenever(userRepository.findByStatus("재직")).thenReturn(emptyList())
            whenever(userRepository.findByStatus("휴직")).thenReturn(emptyList())

            // When
            val result = adminDashboardService.getDashboard(null, null)

            // Then - yearMonth should match current year-month pattern
            assertThat(result.salesSummary.yearMonth).matches("\\d{4}-\\d{2}")
        }

        // ========== 채널별 매출 ==========

        @Test
        @DisplayName("채널별 매출 - 다양한 abcType -> 채널 분류 정확히 매핑")
        fun channelSales_classifiesCorrectly() {
            // Given
            val yearMonth = "2026-03"
            val scope = DataScope(branchCodes = emptyList(), isAllBranches = true)

            val salesData = listOf(
                createSalesHistory(accountExternalKey = "EK001",
                    abcClosingAmount1 = 100.0, abcClosingAmount2 = 200.0, abcClosingAmount3 = 50.0,
                    targetMonthResults = 1000.0, shipClosingAmount = 0.0),
                createSalesHistory(accountExternalKey = "EK002",
                    abcClosingAmount1 = 300.0, abcClosingAmount2 = 0.0, abcClosingAmount3 = 0.0,
                    targetMonthResults = 500.0, shipClosingAmount = 0.0),
                createSalesHistory(accountExternalKey = "EK003",
                    abcClosingAmount1 = 50.0, abcClosingAmount2 = 0.0, abcClosingAmount3 = 0.0,
                    targetMonthResults = 200.0, shipClosingAmount = 0.0)
            )

            val accounts = listOf(
                createAccount(sfid = "A1", externalKey = "EK001", abcType = "대형마트"),
                createAccount(sfid = "A2", externalKey = "EK002", abcType = "슈퍼"),
                createAccount(sfid = "A3", externalKey = "EK003", abcType = "편의점")
            )

            whenever(dataScopeHolder.require()).thenReturn(scope)
            whenever(monthlySalesHistoryRepository.findBySalesYearAndSalesMonth("2026", "03"))
                .thenReturn(salesData)
            whenever(monthlySalesHistoryRepository.findBySalesYearAndSalesMonth("2025", "03"))
                .thenReturn(emptyList())
            whenever(accountRepository.findAll()).thenReturn(accounts)
            whenever(storeScheduleRepository.findByConfirmedTrueAndStartDateLessThanEqualAndEndDateGreaterThanEqual(any(), any()))
                .thenReturn(emptyList())
            whenever(userRepository.findByStatus("재직")).thenReturn(emptyList())
            whenever(userRepository.findByStatus("휴직")).thenReturn(emptyList())

            // When
            val result = adminDashboardService.getDashboard(yearMonth, null)

            // Then
            val channelMap = result.salesSummary.channelSales.associateBy { it.channelName }
            assertThat(channelMap).containsKeys("대형마트", "슈퍼", "편의점")
            // 대형마트: abc1(100) + abc2(200) + abc3(50) = 350
            assertThat(channelMap["대형마트"]!!.actualAmount).isEqualTo(350L)
            assertThat(channelMap["대형마트"]!!.targetAmount).isEqualTo(1000L)
            // 슈퍼: abc1(300) = 300
            assertThat(channelMap["슈퍼"]!!.actualAmount).isEqualTo(300L)
            // 편의점: abc1(50) = 50
            assertThat(channelMap["편의점"]!!.actualAmount).isEqualTo(50L)
        }

        // ========== 진행률 계산 ==========

        @Test
        @DisplayName("진행률 - 목표 대비 실적 비율 -> progressRate 정확히 계산")
        fun progressRate_calculatedCorrectly() {
            // Given
            val yearMonth = "2026-03"
            val scope = DataScope(branchCodes = emptyList(), isAllBranches = true)

            val salesData = listOf(
                createSalesHistory(targetMonthResults = 10000.0, shipClosingAmount = 7500.0,
                    accountExternalKey = "EK001")
            )

            whenever(dataScopeHolder.require()).thenReturn(scope)
            whenever(monthlySalesHistoryRepository.findBySalesYearAndSalesMonth("2026", "03"))
                .thenReturn(salesData)
            whenever(monthlySalesHistoryRepository.findBySalesYearAndSalesMonth("2025", "03"))
                .thenReturn(emptyList())
            whenever(accountRepository.findAll()).thenReturn(emptyList())
            whenever(storeScheduleRepository.findByConfirmedTrueAndStartDateLessThanEqualAndEndDateGreaterThanEqual(any(), any()))
                .thenReturn(emptyList())
            whenever(userRepository.findByStatus("재직")).thenReturn(emptyList())
            whenever(userRepository.findByStatus("휴직")).thenReturn(emptyList())

            // When
            val result = adminDashboardService.getDashboard(yearMonth, null)

            // Then - progressRate = 7500 / 10000 * 100 = 75.0
            assertThat(result.salesSummary.progressRate).isEqualTo(75.0)
        }

        @Test
        @DisplayName("진행률 - 목표금액 0 -> progressRate 0.0")
        fun progressRate_zeroTarget_returnsZero() {
            // Given
            val yearMonth = "2026-03"
            val scope = DataScope(branchCodes = emptyList(), isAllBranches = true)

            val salesData = listOf(
                createSalesHistory(targetMonthResults = 0.0, shipClosingAmount = 500.0,
                    accountExternalKey = "EK001")
            )

            whenever(dataScopeHolder.require()).thenReturn(scope)
            whenever(monthlySalesHistoryRepository.findBySalesYearAndSalesMonth("2026", "03"))
                .thenReturn(salesData)
            whenever(monthlySalesHistoryRepository.findBySalesYearAndSalesMonth("2025", "03"))
                .thenReturn(emptyList())
            whenever(accountRepository.findAll()).thenReturn(emptyList())
            whenever(storeScheduleRepository.findByConfirmedTrueAndStartDateLessThanEqualAndEndDateGreaterThanEqual(any(), any()))
                .thenReturn(emptyList())
            whenever(userRepository.findByStatus("재직")).thenReturn(emptyList())
            whenever(userRepository.findByStatus("휴직")).thenReturn(emptyList())

            // When
            val result = adminDashboardService.getDashboard(yearMonth, null)

            // Then
            assertThat(result.salesSummary.progressRate).isEqualTo(0.0)
        }
    }

    // ========== Helper Factories ==========

    private fun createSalesHistory(
        id: Long = 0,
        salesYear: String? = null,
        salesMonth: String? = null,
        targetMonthResults: Double? = null,
        shipClosingAmount: Double? = null,
        abcClosingAmount1: Double? = null,
        abcClosingAmount2: Double? = null,
        abcClosingAmount3: Double? = null,
        accountExternalKey: String? = null,
        accountType: String? = null,
        accountBranchName: String? = null
    ): MonthlySalesHistory {
        return MonthlySalesHistory(
            id = id,
            salesYear = salesYear,
            salesMonth = salesMonth,
            targetMonthResults = targetMonthResults,
            shipClosingAmount = shipClosingAmount,
            abcClosingAmount1 = abcClosingAmount1,
            abcClosingAmount2 = abcClosingAmount2,
            abcClosingAmount3 = abcClosingAmount3,
            accountExternalKey = accountExternalKey,
            accountType = accountType,
            accountBranchName = accountBranchName
        )
    }

    private fun createStoreSchedule(
        id: Long = 0,
        sfid: String? = null,
        accountSfid: String? = null,
        confirmed: Boolean? = true,
        employeeSfid: String? = null,
        typeOfWork1: String? = null,
        startDate: LocalDate? = null,
        endDate: LocalDate? = null
    ): StoreSchedule {
        return StoreSchedule(
            id = id,
            sfid = sfid,
            account = accountSfid,
            confirmed = confirmed,
            fullName = employeeSfid,
            typeOfWork1 = typeOfWork1,
            startDate = startDate,
            endDate = endDate
        )
    }

    private fun createAccount(
        id: Int = 0,
        sfid: String? = null,
        name: String? = null,
        abcType: String? = null,
        externalKey: String? = null,
        branchCode: String? = null,
        branchName: String? = null
    ): Account {
        return Account(
            id = id,
            sfid = sfid,
            name = name,
            abcType = abcType,
            externalKey = externalKey,
            branchCode = branchCode,
            branchName = branchName
        )
    }

    private fun createUser(
        id: Long = 0,
        employeeId: String = "EMP001",
        name: String = "테스트사원",
        appAuthority: String? = null,
        costCenterCode: String? = null,
        sfid: String? = null,
        birthDate: String? = null,
        status: String? = null
    ): User {
        return User(
            id = id,
            employeeId = employeeId,
            name = name,
            appAuthority = appAuthority,
            costCenterCode = costCenterCode,
            sfid = sfid,
            birthDate = birthDate,
            status = status
        )
    }
}
