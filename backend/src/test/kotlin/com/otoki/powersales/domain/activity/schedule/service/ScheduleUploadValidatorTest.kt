package com.otoki.powersales.domain.activity.schedule.service

import com.otoki.powersales.domain.foundation.account.entity.Account
import com.otoki.powersales.domain.org.employee.entity.Employee
import com.otoki.powersales.domain.activity.schedule.entity.DisplayWorkSchedule
import com.otoki.powersales.domain.activity.schedule.enums.TypeOfWork3
import com.otoki.powersales.domain.activity.schedule.enums.TypeOfWork5
import com.otoki.powersales.domain.activity.schedule.service.ScheduleExcelParser
import com.otoki.powersales.domain.activity.schedule.service.ScheduleUploadValidator
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.temporal.ChronoUnit

@DisplayName("ScheduleUploadValidator 테스트")
class ScheduleUploadValidatorTest {

    private val validator = ScheduleUploadValidator()

    private val employeeMap = mapOf(
        "20030001" to createEmployee("20030001", "홍길동", "USR001", "재직"),
        "20030002" to createEmployee("20030002", "김철수", "USR002", "재직"),
        "99999999" to createEmployee("99999999", "퇴직자", "USR999", "퇴직", endDate = LocalDate.of(2026, 1, 31))
    )

    private val accountMap = mapOf(
        "ACC001" to createAccount("ACC001", "ACC_SFID_001", "이마트 강남점", id = 1),
        "ACC002" to createAccount("ACC002", "ACC_SFID_002", "홈플러스 역삼점", id = 2)
    )

    @Nested
    @DisplayName("Happy Path")
    inner class HappyPathTests {

        @Test
        @DisplayName("정상 데이터 - 검증 통과")
        fun validate_success() {
            val rows = listOf(
                createParsedRow(4, "20030001", "홍길동", "ACC001", "이마트 강남점", "고정", "상온", "상시", "2026-04-01", null)
            )

            val result = validator.validate(rows, employeeMap, accountMap, emptyList())

            assertThat(result.errors).isEmpty()
            assertThat(result.validRows).hasSize(1)
            assertThat(result.previews).hasSize(1)
            assertThat(result.validRows[0].userEmployeeCode).isEqualTo("20030001")
            assertThat(result.validRows[0].accountId).isEqualTo(1)
            assertThat(result.validRows[0].typeOfWork4).isEqualTo("상온")
        }

        @Test
        @DisplayName("종료일 미입력 - endDate null 허용")
        fun validate_nullEndDate() {
            val rows = listOf(
                createParsedRow(4, "20030001", "홍길동", "ACC001", null, "순회", "냉동/냉장", "상시", "2026-04-01", null)
            )

            val result = validator.validate(rows, employeeMap, accountMap, emptyList())

            assertThat(result.errors).isEmpty()
            assertThat(result.validRows[0].endDate).isNull()
        }
    }

    @Nested
    @DisplayName("V1 - 사원번호 존재")
    inner class V1Tests {

        @Test
        @DisplayName("존재하지 않는 사원번호 - 에러")
        fun v1_employeeNotFound() {
            val rows = listOf(
                createParsedRow(4, "88888888", "없는사원", "ACC001", null, "고정", "상온", "상시", "2026-04-01", null)
            )

            val result = validator.validate(rows, employeeMap, accountMap, emptyList())

            assertThat(result.errors).hasSize(1)
            assertThat(result.errors[0].message).contains("존재하지 않는 사원")
        }
    }

    @Nested
    @DisplayName("V2 - 재직 상태 / 앱 로그인 활성화 + 종료일 유예")
    inner class V2Tests {

        @Test
        @DisplayName("퇴직 사원 + endDate 과거 - 에러")
        fun v2_resignedWithPastEndDate() {
            val rows = listOf(
                createParsedRow(4, "99999999", "퇴직자", "ACC001", null, "고정", "상온", "상시", "2026-04-01", null)
            )

            val result = validator.validate(rows, employeeMap, accountMap, emptyList())

            assertThat(result.errors).hasSize(1)
            assertThat(result.errors[0].message).contains("퇴직한 사원")
        }

        @Test
        @DisplayName("퇴직 사원 + endDate 미래 - 유예 통과")
        fun v2_resignedWithFutureEndDate() {
            val futureRetireMap = mapOf(
                "99999999" to createEmployee(
                    "99999999", "퇴직예정", "USR999", "퇴직",
                    endDate = LocalDate.now().plus(30, ChronoUnit.DAYS)
                )
            )
            val rows = listOf(
                createParsedRow(4, "99999999", "퇴직예정", "ACC001", null, "고정", "상온", "상시", "2026-04-01", null)
            )

            val result = validator.validate(rows, futureRetireMap, accountMap, emptyList())

            assertThat(result.errors).isEmpty()
        }

        @Test
        @DisplayName("퇴직 사원 + endDate null - 유예 통과")
        fun v2_resignedWithoutEndDate() {
            val nullEndMap = mapOf(
                "99999999" to createEmployee("99999999", "퇴직자(EndDate미상)", "USR999", "퇴직", endDate = null)
            )
            val rows = listOf(
                createParsedRow(4, "99999999", "퇴직자(EndDate미상)", "ACC001", null, "고정", "상온", "상시", "2026-04-01", null)
            )

            val result = validator.validate(rows, nullEndMap, accountMap, emptyList())

            assertThat(result.errors).isEmpty()
        }

        @Test
        @DisplayName("재직 + appLoginActive false + endDate 과거 - 에러")
        fun v2_inactiveLoginWithPastEndDate() {
            val inactiveMap = mapOf(
                "20030001" to createEmployee(
                    "20030001", "홍길동", "USR001", "재직",
                    appLoginActive = false, endDate = LocalDate.of(2026, 1, 31)
                )
            )
            val rows = listOf(
                createParsedRow(4, "20030001", "홍길동", "ACC001", null, "고정", "상온", "상시", "2026-04-01", null)
            )

            val result = validator.validate(rows, inactiveMap, accountMap, emptyList())

            assertThat(result.errors).hasSize(1)
            assertThat(result.errors[0].message).contains("퇴직한 사원")
        }

        @Test
        @DisplayName("재직 + appLoginActive false + endDate null - 유예 통과")
        fun v2_inactiveLoginWithoutEndDate() {
            val inactiveMap = mapOf(
                "20030001" to createEmployee("20030001", "홍길동", "USR001", "재직", appLoginActive = false)
            )
            val rows = listOf(
                createParsedRow(4, "20030001", "홍길동", "ACC001", null, "고정", "상온", "상시", "2026-04-01", null)
            )

            val result = validator.validate(rows, inactiveMap, accountMap, emptyList())

            assertThat(result.errors).isEmpty()
        }

        @Test
        @DisplayName("재직 + appLoginActive null + endDate null - 유예 통과")
        fun v2_nullAppLoginActiveWithoutEndDate() {
            val nullActiveMap = mapOf(
                "20030001" to createEmployee("20030001", "홍길동", "USR001", "재직", appLoginActive = null)
            )
            val rows = listOf(
                createParsedRow(4, "20030001", "홍길동", "ACC001", null, "고정", "상온", "상시", "2026-04-01", null)
            )

            val result = validator.validate(rows, nullActiveMap, accountMap, emptyList())

            assertThat(result.errors).isEmpty()
        }

        @Test
        @DisplayName("재직 + appLoginActive true - 정상")
        fun v2_activeEmployee() {
            val rows = listOf(
                createParsedRow(4, "20030001", "홍길동", "ACC001", null, "고정", "상온", "상시", "2026-04-01", null)
            )

            val result = validator.validate(rows, employeeMap, accountMap, emptyList())

            assertThat(result.errors).isEmpty()
        }
    }

    @Nested
    @DisplayName("V3 - 거래처코드 존재")
    inner class V3Tests {

        @Test
        @DisplayName("존재하지 않는 거래처 - 에러")
        fun v3_accountNotFound() {
            val rows = listOf(
                createParsedRow(4, "20030001", "홍길동", "INVALID", null, "고정", "상온", "상시", "2026-04-01", null)
            )

            val result = validator.validate(rows, employeeMap, accountMap, emptyList())

            assertThat(result.errors).hasSize(1)
            assertThat(result.errors[0].message).contains("존재하지 않는 거래처")
        }
    }

    @Nested
    @DisplayName("V3a - 거래처 폐업 상태")
    inner class V3aTests {

        @Test
        @DisplayName("폐업 거래처 (예외 미충족) - 에러")
        fun v3a_closedAccount() {
            val closedAccountMap = mapOf(
                "ACC001" to createAccount("ACC001", "ACC_SFID_001", "이마트 강남점", accountStatusName = "폐업", accountGroup = "2000")
            )
            val rows = listOf(
                createParsedRow(4, "20030001", "홍길동", "ACC001", null, "고정", "상온", "상시", "2026-04-01", null)
            )

            val result = validator.validate(rows, employeeMap, closedAccountMap, emptyList())

            assertThat(result.errors).hasSize(1)
            assertThat(result.errors[0].message).contains("폐업 상태의 거래처입니다")
        }

        @Test
        @DisplayName("폐업 거래처 예외 허용 (배포처) - 정상")
        fun v3a_closedAccountExemptByDistribution() {
            val exemptAccountMap = mapOf(
                "ACC001" to createAccount("ACC001", "ACC_SFID_001", "이마트 강남점", accountStatusName = "폐업", accountGroup = "1000", distribution = "X")
            )
            val rows = listOf(
                createParsedRow(4, "20030001", "홍길동", "ACC001", null, "고정", "상온", "상시", "2026-04-01", null)
            )

            val result = validator.validate(rows, employeeMap, exemptAccountMap, emptyList())

            assertThat(result.errors).isEmpty()
        }

        @Test
        @DisplayName("폐업 거래처 예외 허용 (ABCTypeCode 3062) - 정상")
        fun v3a_closedAccountExemptByAbcTypeCode() {
            val exemptAccountMap = mapOf(
                "ACC001" to createAccount("ACC001", "ACC_SFID_001", "이마트 강남점", accountStatusName = "폐업", accountGroup = "1010", abcTypeCode = "3062")
            )
            val rows = listOf(
                createParsedRow(4, "20030001", "홍길동", "ACC001", null, "고정", "상온", "상시", "2026-04-01", null)
            )

            val result = validator.validate(rows, employeeMap, exemptAccountMap, emptyList())

            assertThat(result.errors).isEmpty()
        }

        @Test
        @DisplayName("폐업 거래처 - 그룹 맞지만 배포처+ABCType 모두 미충족 - 에러")
        fun v3a_closedAccountGroupMatchButNoDistributionOrAbcType() {
            val accountMap = mapOf(
                "ACC001" to createAccount("ACC001", "ACC_SFID_001", "이마트 강남점", accountStatusName = "폐업", accountGroup = "1000", distribution = null, abcTypeCode = "1000")
            )
            val rows = listOf(
                createParsedRow(4, "20030001", "홍길동", "ACC001", null, "고정", "상온", "상시", "2026-04-01", null)
            )

            val result = validator.validate(rows, employeeMap, accountMap, emptyList())

            assertThat(result.errors).hasSize(1)
            assertThat(result.errors[0].message).contains("폐업 상태의 거래처입니다")
        }

        @Test
        @DisplayName("미존재 거래처는 V3에서 이미 실패 - V3a 미실행")
        fun v3a_nonExistentAccountSkipped() {
            val rows = listOf(
                createParsedRow(4, "20030001", "홍길동", "INVALID", null, "고정", "상온", "상시", "2026-04-01", null)
            )

            val result = validator.validate(rows, employeeMap, accountMap, emptyList())

            assertThat(result.errors).hasSize(1)
            assertThat(result.errors[0].message).contains("존재하지 않는 거래처")
            assertThat(result.errors[0].message).doesNotContain("폐업")
        }

        @Test
        @DisplayName("퇴직(EndDate 과거) 사원 + 폐업 거래처 동시 - 에러 2건")
        fun v3a_inactiveEmployeeAndClosedAccountBothErrors() {
            val inactiveEmployeeMap = mapOf(
                "20030001" to createEmployee(
                    "20030001", "홍길동", "USR001", "재직",
                    appLoginActive = false, endDate = LocalDate.of(2026, 1, 31)
                )
            )
            val closedAccountMap = mapOf(
                "ACC001" to createAccount("ACC001", "ACC_SFID_001", "이마트 강남점", accountStatusName = "폐업", accountGroup = "2000")
            )
            val rows = listOf(
                createParsedRow(4, "20030001", "홍길동", "ACC001", null, "고정", "상온", "상시", "2026-04-01", null)
            )

            val result = validator.validate(rows, inactiveEmployeeMap, closedAccountMap, emptyList())

            assertThat(result.errors).hasSize(2)
            assertThat(result.errors.map { it.message }).anySatisfy { assertThat(it).contains("퇴직한 사원") }
            assertThat(result.errors.map { it.message }).anySatisfy { assertThat(it).contains("폐업 상태의 거래처입니다") }
        }

        @Test
        @DisplayName("활성 거래처 - 정상 (V3a 해당 없음)")
        fun v3a_activeAccount() {
            val rows = listOf(
                createParsedRow(4, "20030001", "홍길동", "ACC001", null, "고정", "상온", "상시", "2026-04-01", null)
            )

            val result = validator.validate(rows, employeeMap, accountMap, emptyList())

            assertThat(result.errors).isEmpty()
        }
    }

    @Nested
    @DisplayName("V4 - 시작일 <= 종료일")
    inner class V4Tests {

        @Test
        @DisplayName("시작일 > 종료일 - 에러")
        fun v4_startAfterEnd() {
            val rows = listOf(
                createParsedRow(4, "20030001", "홍길동", "ACC001", null, "고정", "상온", "상시", "2026-04-01", "2026-03-01")
            )

            val result = validator.validate(rows, employeeMap, accountMap, emptyList())

            assertThat(result.errors).hasSize(1)
            assertThat(result.errors[0].message).contains("시작일이 종료일보다 이후")
        }
    }

    @Nested
    @DisplayName("V5 - 근무형태3 유효성")
    inner class V5Tests {

        @Test
        @DisplayName("유효하지 않은 근무형태3 - 에러")
        fun v5_invalidWorkType3() {
            val rows = listOf(
                createParsedRow(4, "20030001", "홍길동", "ACC001", null, "파견", "상온", "상시", "2026-04-01", null)
            )

            val result = validator.validate(rows, employeeMap, accountMap, emptyList())

            assertThat(result.errors).hasSize(1)
            assertThat(result.errors[0].message).contains("유효하지 않은 근무형태3 '파견'")
        }
    }

    @Nested
    @DisplayName("V5a - 근무형태4 유효성")
    inner class V5aTests {

        @Test
        @DisplayName("상온 - 정상")
        fun v5a_validNormal() {
            val rows = listOf(
                createParsedRow(4, "20030001", "홍길동", "ACC001", null, "고정", "상온", "상시", "2026-04-01", null)
            )

            val result = validator.validate(rows, employeeMap, accountMap, emptyList())

            assertThat(result.errors).isEmpty()
        }

        @Test
        @DisplayName("냉동/냉장 - 정상")
        fun v5a_validCold() {
            val rows = listOf(
                createParsedRow(4, "20030001", "홍길동", "ACC001", null, "고정", "냉동/냉장", "상시", "2026-04-01", null)
            )

            val result = validator.validate(rows, employeeMap, accountMap, emptyList())

            assertThat(result.errors).isEmpty()
        }

        @Test
        @DisplayName("유효하지 않은 근무형태4 - 에러")
        fun v5a_invalidWorkType4() {
            val rows = listOf(
                createParsedRow(4, "20030001", "홍길동", "ACC001", null, "고정", "기타", "상시", "2026-04-01", null)
            )

            val result = validator.validate(rows, employeeMap, accountMap, emptyList())

            assertThat(result.errors).hasSize(1)
            assertThat(result.errors[0].message).contains("유효하지 않은 근무형태4 '기타'")
        }

        @Test
        @DisplayName("근무형태4 미입력 - 에러")
        fun v5a_missingWorkType4() {
            val rows = listOf(
                createParsedRow(4, "20030001", "홍길동", "ACC001", null, "고정", null, "상시", "2026-04-01", null)
            )

            val result = validator.validate(rows, employeeMap, accountMap, emptyList())

            assertThat(result.errors).hasSize(1)
            assertThat(result.errors[0].message).contains("근무형태4는 필수 입력입니다")
        }
    }

    @Nested
    @DisplayName("V7 - 임시는 순회만")
    inner class V7Tests {

        @Test
        @DisplayName("임시 + 고정 조합 - 에러")
        fun v7_tempWithFixed() {
            // 종료일을 채워 V7a(임시 종료일 필수) 와 분리 — 순수 V7(순회만) 위반만 검증.
            val rows = listOf(
                createParsedRow(4, "20030001", "홍길동", "ACC001", null, "고정", "상온", "임시", "2026-04-01", "2026-04-30")
            )

            val result = validator.validate(rows, employeeMap, accountMap, emptyList())

            assertThat(result.errors).hasSize(1)
            assertThat(result.errors[0].message).contains("임시 배치는 순회만 가능")
        }

        @Test
        @DisplayName("임시 + 순회 - 정상")
        fun v7_tempWithRound() {
            val rows = listOf(
                createParsedRow(4, "20030001", "홍길동", "ACC001", null, "순회", "상온", "임시", "2026-04-01", "2026-04-30")
            )

            val result = validator.validate(rows, employeeMap, accountMap, emptyList())

            assertThat(result.errors).isEmpty()
        }
    }

    @Nested
    @DisplayName("V7a - 임시는 종료일 필수")
    inner class V7aTests {

        @Test
        @DisplayName("임시 + 종료일 없음 - 에러")
        fun v7a_tempWithoutEndDate() {
            val rows = listOf(
                createParsedRow(4, "20030001", "홍길동", "ACC001", null, "순회", "상온", "임시", "2026-04-01", null)
            )

            val result = validator.validate(rows, employeeMap, accountMap, emptyList())

            assertThat(result.errors).hasSize(1)
            assertThat(result.errors[0].message).contains("임시 배치는 종료일이 필수입니다")
        }

        @Test
        @DisplayName("임시 + 종료일 있음 - 정상")
        fun v7a_tempWithEndDate() {
            val rows = listOf(
                createParsedRow(4, "20030001", "홍길동", "ACC001", null, "순회", "상온", "임시", "2026-04-01", "2026-04-30")
            )

            val result = validator.validate(rows, employeeMap, accountMap, emptyList())

            assertThat(result.errors).isEmpty()
        }

        @Test
        @DisplayName("상시 + 종료일 없음 - 정상 (임시가 아니면 종료일 선택)")
        fun v7a_permanentWithoutEndDate() {
            val rows = listOf(
                createParsedRow(4, "20030001", "홍길동", "ACC001", null, "순회", "상온", "상시", "2026-04-01", null)
            )

            val result = validator.validate(rows, employeeMap, accountMap, emptyList())

            assertThat(result.errors).isEmpty()
        }
    }

    @Nested
    @DisplayName("V8 - DB 기존 레코드 기간 중복")
    inner class V8Tests {

        @Test
        @DisplayName("기존 스케줄과 기간 중복 - 에러")
        fun v8_dbOverlap() {
            val existingSchedules = listOf(
                DisplayWorkSchedule(
                    employee = Employee(id = 1L, employeeCode = "20030001", name = "테스트"),
                    account = Account(id = 1),
                    startDate = LocalDate.of(2026, 3, 1),
                    endDate = LocalDate.of(2026, 5, 1),
                    typeOfWork3 = TypeOfWork3.ROTATION,
                    typeOfWork5 = TypeOfWork5.REGULAR
                )
            )
            val rows = listOf(
                createParsedRow(4, "20030001", "홍길동", "ACC001", null, "순회", "상온", "상시", "2026-04-01", "2026-06-01")
            )

            val result = validator.validate(rows, employeeMap, accountMap, existingSchedules)

            assertThat(result.errors).hasSize(1)
            assertThat(result.errors[0].message).contains("기존 스케줄과 기간 중복")
        }
    }

    @Nested
    @DisplayName("V9 - 파일 내 중복")
    inner class V9Tests {

        @Test
        @DisplayName("파일 내 동일 사원/거래처/기간 중복 - 에러")
        fun v9_fileInternalDuplicate() {
            val rows = listOf(
                createParsedRow(4, "20030001", "홍길동", "ACC001", null, "순회", "상온", "상시", "2026-04-01", "2026-04-30"),
                createParsedRow(5, "20030001", "홍길동", "ACC001", null, "순회", "상온", "상시", "2026-04-15", "2026-05-15")
            )

            val result = validator.validate(rows, employeeMap, accountMap, emptyList())

            assertThat(result.errors).hasSize(1)
            assertThat(result.errors[0].message).contains("파일 내 중복")
            assertThat(result.validRows).hasSize(1)
        }
    }

    @Nested
    @DisplayName("C1 - 고정 배치 조합")
    inner class C1Tests {

        @Test
        @DisplayName("고정 존재 시 다른 유형 추가 불가 - 에러")
        fun c1_fixedExists() {
            val existingSchedules = listOf(
                DisplayWorkSchedule(
                    employee = Employee(id = 1L, employeeCode = "20030001", name = "테스트"),
                    account = Account(id = 2),
                    startDate = LocalDate.of(2026, 3, 1),
                    endDate = LocalDate.of(2026, 5, 1),
                    typeOfWork3 = TypeOfWork3.FIXED,
                    typeOfWork5 = TypeOfWork5.REGULAR
                )
            )
            val rows = listOf(
                createParsedRow(4, "20030001", "홍길동", "ACC001", null, "순회", "상온", "상시", "2026-04-01", "2026-04-30")
            )

            val result = validator.validate(rows, employeeMap, accountMap, existingSchedules)

            assertThat(result.errors).hasSize(1)
            assertThat(result.errors[0].message).contains("고정 배치가 이미 존재")
        }
    }

    @Nested
    @DisplayName("C2 - 격고 최대 2개")
    inner class C2Tests {

        @Test
        @DisplayName("격고 2개 존재 시 추가 불가 - 에러")
        fun c2_alternateLimit() {
            val existingSchedules = listOf(
                DisplayWorkSchedule(employee = Employee(id = 1L, employeeCode = "20030001", name = "테스트"), account = Account(id = 101), startDate = LocalDate.of(2026, 3, 1), endDate = LocalDate.of(2026, 5, 1), typeOfWork3 = TypeOfWork3.GAP, typeOfWork5 = TypeOfWork5.REGULAR),
                DisplayWorkSchedule(employee = Employee(id = 1L, employeeCode = "20030001", name = "테스트"), account = Account(id = 102), startDate = LocalDate.of(2026, 3, 1), endDate = LocalDate.of(2026, 5, 1), typeOfWork3 = TypeOfWork3.GAP, typeOfWork5 = TypeOfWork5.REGULAR)
            )
            val rows = listOf(
                createParsedRow(4, "20030001", "홍길동", "ACC001", null, "격고", "상온", "상시", "2026-04-01", "2026-04-30")
            )

            val result = validator.validate(rows, employeeMap, accountMap, existingSchedules)

            assertThat(result.errors).hasSize(1)
            assertThat(result.errors[0].message).contains("격고 배치가 이미 2개 존재")
        }
    }

    @Nested
    @DisplayName("C2a - 순회+격고 공존 시 격고 1건 제한")
    inner class C2aTests {

        @Test
        @DisplayName("순회+격고1 존재, 격고 추가 - 에러")
        fun c2a_patrolAndAlternateExist_alternateBlocked() {
            val existingSchedules = listOf(
                DisplayWorkSchedule(employee = Employee(id = 1L, employeeCode = "20030001", name = "테스트"), account = Account(id = 101), startDate = LocalDate.of(2026, 3, 1), endDate = LocalDate.of(2026, 5, 1), typeOfWork3 = TypeOfWork3.ROTATION, typeOfWork5 = TypeOfWork5.REGULAR),
                DisplayWorkSchedule(employee = Employee(id = 1L, employeeCode = "20030001", name = "테스트"), account = Account(id = 102), startDate = LocalDate.of(2026, 3, 1), endDate = LocalDate.of(2026, 5, 1), typeOfWork3 = TypeOfWork3.GAP, typeOfWork5 = TypeOfWork5.REGULAR)
            )
            val rows = listOf(
                createParsedRow(4, "20030001", "홍길동", "ACC001", null, "격고", "상온", "상시", "2026-04-01", "2026-04-30")
            )

            val result = validator.validate(rows, employeeMap, accountMap, existingSchedules)

            assertThat(result.errors).hasSize(1)
            assertThat(result.errors[0].message).contains("순회 레코드가 존재하므로 격고는 1건만 등록 가능합니다")
        }

        @Test
        @DisplayName("순회만 존재, 격고 추가 - 성공")
        fun c2a_patrolOnly_alternateAllowed() {
            val existingSchedules = listOf(
                DisplayWorkSchedule(employee = Employee(id = 1L, employeeCode = "20030001", name = "테스트"), account = Account(id = 101), startDate = LocalDate.of(2026, 3, 1), endDate = LocalDate.of(2026, 5, 1), typeOfWork3 = TypeOfWork3.ROTATION, typeOfWork5 = TypeOfWork5.REGULAR)
            )
            val rows = listOf(
                createParsedRow(4, "20030001", "홍길동", "ACC001", null, "격고", "상온", "상시", "2026-04-01", "2026-04-30")
            )

            val result = validator.validate(rows, employeeMap, accountMap, existingSchedules)

            assertThat(result.errors).isEmpty()
        }

        @Test
        @DisplayName("격고1만 존재(순회 없음), 격고 추가 - 성공 (격고 2건 이내)")
        fun c2a_alternateOnly_secondAlternateAllowed() {
            val existingSchedules = listOf(
                DisplayWorkSchedule(employee = Employee(id = 1L, employeeCode = "20030001", name = "테스트"), account = Account(id = 101), startDate = LocalDate.of(2026, 3, 1), endDate = LocalDate.of(2026, 5, 1), typeOfWork3 = TypeOfWork3.GAP, typeOfWork5 = TypeOfWork5.REGULAR)
            )
            val rows = listOf(
                createParsedRow(4, "20030001", "홍길동", "ACC001", null, "격고", "상온", "상시", "2026-04-01", "2026-04-30")
            )

            val result = validator.validate(rows, employeeMap, accountMap, existingSchedules)

            assertThat(result.errors).isEmpty()
        }

        @Test
        @DisplayName("파일 내 순회+격고, 격고 추가 - 에러")
        fun c2a_fileInternalPatrolAndAlternate_alternateBlocked() {
            val threeAccountMap = accountMap + mapOf(
                "ACC003" to createAccount("ACC003", "ACC_SFID_003", "롯데마트 서초점", id = 3)
            )
            val rows = listOf(
                createParsedRow(4, "20030001", "홍길동", "ACC001", null, "순회", "상온", "상시", "2026-04-01", "2026-04-30"),
                createParsedRow(5, "20030001", "홍길동", "ACC002", null, "격고", "상온", "상시", "2026-04-01", "2026-04-30"),
                createParsedRow(6, "20030001", "홍길동", "ACC003", null, "격고", "상온", "상시", "2026-04-01", "2026-04-30")
            )

            val result = validator.validate(rows, employeeMap, threeAccountMap, emptyList())

            assertThat(result.errors).hasSize(1)
            assertThat(result.errors[0].row).isEqualTo(6)
            assertThat(result.errors[0].message).contains("순회 레코드가 존재하므로 격고는 1건만 등록 가능합니다")
        }
    }

    @Nested
    @DisplayName("C3 - 임시 최대 1개")
    inner class C3Tests {

        @Test
        @DisplayName("임시 1개 존재 시 추가 불가 - 에러")
        fun c3_tempLimit() {
            val existingSchedules = listOf(
                DisplayWorkSchedule(employee = Employee(id = 1L, employeeCode = "20030001", name = "테스트"), account = Account(id = 101), startDate = LocalDate.of(2026, 3, 1), endDate = LocalDate.of(2026, 5, 1), typeOfWork3 = TypeOfWork3.ROTATION, typeOfWork5 = TypeOfWork5.TEMPORARY)
            )
            val rows = listOf(
                createParsedRow(4, "20030001", "홍길동", "ACC001", null, "순회", "상온", "임시", "2026-04-01", "2026-04-30")
            )

            val result = validator.validate(rows, employeeMap, accountMap, existingSchedules)

            assertThat(result.errors).hasSize(1)
            assertThat(result.errors[0].message).contains("임시 배치가 이미 존재")
        }
    }

    private fun createParsedRow(
        rowNumber: Int,
        employeeCode: String?,
        employeeName: String?,
        accountCode: String?,
        accountName: String?,
        typeOfWork3: String?,
        typeOfWork4: String?,
        typeOfWork5: String?,
        startDateStr: String?,
        endDateStr: String?
    ): ScheduleExcelParser.ParsedRow {
        val startDate = startDateStr?.let { LocalDate.parse(it) }
        val endDate = endDateStr?.let { LocalDate.parse(it) }
        return ScheduleExcelParser.ParsedRow(
            rowNumber = rowNumber,
            employeeCode = employeeCode,
            employeeName = employeeName,
            accountCode = accountCode,
            accountName = accountName,
            typeOfWork3 = typeOfWork3,
            typeOfWork4 = typeOfWork4,
            typeOfWork5 = typeOfWork5,
            startDateStr = startDateStr,
            endDateStr = endDateStr,
            startDate = startDate,
            endDate = endDate
        )
    }

    @Nested
    @DisplayName("validateSingle - 단건 등록 검증")
    inner class ValidateSingleTests {

        private val employee = createEmployee("20030001", "홍길동", "USR001", "재직")
        private val account = createAccount("ACC001", "ACC_SFID_001", "이마트 강남점", id = 1)

        @Test
        @DisplayName("정상 데이터 - 검증 통과")
        fun validateSingle_success() {
            val result = validator.validateSingle(
                employeeCode = "20030001",
                accountCode = "ACC001",
                typeOfWork3 = "고정",
                typeOfWork4 = "상온",
                typeOfWork5 = "상시",
                startDate = LocalDate.of(2026, 5, 1),
                endDate = null,
                employee = employee,
                account = account,
                existingSchedules = emptyList()
            )

            assertThat(result.messages).isEmpty()
            assertThat(result.validatedRow).isNotNull
            assertThat(result.validatedRow!!.userEmployeeCode).isEqualTo("20030001")
        }

        @Test
        @DisplayName("사원 미존재 - 에러 메시지 + validatedRow null")
        fun validateSingle_employeeNotFound() {
            val result = validator.validateSingle(
                employeeCode = "99999999",
                accountCode = "ACC001",
                typeOfWork3 = "고정",
                typeOfWork4 = "상온",
                typeOfWork5 = "상시",
                startDate = LocalDate.of(2026, 5, 1),
                endDate = null,
                employee = null,
                account = account,
                existingSchedules = emptyList()
            )

            assertThat(result.messages).anyMatch { it.contains("존재하지 않는 사원") }
            assertThat(result.validatedRow).isNull()
        }

        @Test
        @DisplayName("V4 시작일이 종료일보다 늦음 - 에러")
        fun validateSingle_startDateAfterEndDate() {
            val result = validator.validateSingle(
                employeeCode = "20030001",
                accountCode = "ACC001",
                typeOfWork3 = "고정",
                typeOfWork4 = "상온",
                typeOfWork5 = "상시",
                startDate = LocalDate.of(2026, 5, 10),
                endDate = LocalDate.of(2026, 5, 1),
                employee = employee,
                account = account,
                existingSchedules = emptyList()
            )

            assertThat(result.messages).anyMatch { it.contains("시작일이 종료일보다 이후") }
            assertThat(result.validatedRow).isNull()
        }

        @Test
        @DisplayName("V7 임시 + 고정 - 차단")
        fun validateSingle_temporaryWithFixed() {
            val result = validator.validateSingle(
                employeeCode = "20030001",
                accountCode = "ACC001",
                typeOfWork3 = "고정",
                typeOfWork4 = "상온",
                typeOfWork5 = "임시",
                startDate = LocalDate.of(2026, 5, 1),
                endDate = LocalDate.of(2026, 5, 31),
                employee = employee,
                account = account,
                existingSchedules = emptyList()
            )

            assertThat(result.messages).anyMatch { it.contains("임시 배치는 순회만 가능") }
            assertThat(result.validatedRow).isNull()
        }

        @Test
        @DisplayName("V7a 임시 + 종료일 없음 - 차단")
        fun validateSingle_temporaryWithoutEndDate() {
            val result = validator.validateSingle(
                employeeCode = "20030001",
                accountCode = "ACC001",
                typeOfWork3 = "순회",
                typeOfWork4 = "상온",
                typeOfWork5 = "임시",
                startDate = LocalDate.of(2026, 5, 1),
                endDate = null,
                employee = employee,
                account = account,
                existingSchedules = emptyList()
            )

            assertThat(result.messages).anyMatch { it.contains("임시 배치는 종료일이 필수입니다") }
            assertThat(result.validatedRow).isNull()
        }

        @Test
        @DisplayName("V7a 임시 + 종료일 있음 - 통과")
        fun validateSingle_temporaryWithEndDate() {
            val result = validator.validateSingle(
                employeeCode = "20030001",
                accountCode = "ACC001",
                typeOfWork3 = "순회",
                typeOfWork4 = "상온",
                typeOfWork5 = "임시",
                startDate = LocalDate.of(2026, 5, 1),
                endDate = LocalDate.of(2026, 5, 31),
                employee = employee,
                account = account,
                existingSchedules = emptyList()
            )

            assertThat(result.messages).isEmpty()
            assertThat(result.validatedRow).isNotNull
        }

        @Test
        @DisplayName("V8 동일 거래처/사원 기간 중복 - 차단")
        fun validateSingle_duplicateAccount() {
            val existing = DisplayWorkSchedule(
                id = 1L,
                employee = employee,
                account = account,
                typeOfWork3 = TypeOfWork3.FIXED,
                typeOfWork5 = TypeOfWork5.REGULAR,
                startDate = LocalDate.of(2026, 4, 1),
                endDate = LocalDate.of(2026, 6, 30)
            )

            val result = validator.validateSingle(
                employeeCode = "20030001",
                accountCode = "ACC001",
                typeOfWork3 = "순회",
                typeOfWork4 = "상온",
                typeOfWork5 = "상시",
                startDate = LocalDate.of(2026, 5, 1),
                endDate = LocalDate.of(2026, 5, 31),
                employee = employee,
                account = account,
                existingSchedules = listOf(existing)
            )

            assertThat(result.messages).anyMatch { it.contains("기간내에 동일한 거래처") }
            assertThat(result.validatedRow).isNull()
        }
    }

    private fun createEmployee(
        employeeCode: String,
        name: String,
        sfid: String,
        status: String,
        appLoginActive: Boolean? = true,
        endDate: LocalDate? = null
    ): Employee = Employee(
        id = 1L,
        employeeCode = employeeCode,
        name = name,
        sfid = sfid,
        status = status,
        role = null,
        appLoginActive = appLoginActive,
        endDate = endDate
    )

    private fun createAccount(
        externalKey: String,
        sfid: String,
        name: String,
        id: Long = 1L,
        accountStatusName: String? = null,
        accountGroup: String? = null,
        distribution: String? = null,
        abcTypeCode: String? = null
    ): Account = Account(
        id = id,
        externalKey = externalKey,
        sfid = sfid,
        name = name,
        accountStatusName = accountStatusName,
        accountGroup = accountGroup,
        distribution = distribution,
        abcTypeCode = abcTypeCode
    )
}
