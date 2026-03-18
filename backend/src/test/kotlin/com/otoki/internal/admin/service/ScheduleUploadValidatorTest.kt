package com.otoki.internal.admin.service

import com.otoki.internal.sap.entity.Account
import com.otoki.internal.sap.entity.User
import com.otoki.internal.schedule.entity.DisplayWorkSchedule
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDate

@DisplayName("ScheduleUploadValidator 테스트")
class ScheduleUploadValidatorTest {

    private val validator = ScheduleUploadValidator()

    private val userMap = mapOf(
        "20030001" to createUser("20030001", "홍길동", "USR001", "재직"),
        "20030002" to createUser("20030002", "김철수", "USR002", "재직"),
        "99999999" to createUser("99999999", "퇴직자", "USR999", "퇴직")
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
                createParsedRow(4, "20030001", "홍길동", "ACC001", "이마트 강남점", "고정", "상시", "2026-04-01", null)
            )

            val result = validator.validate(rows, userMap, accountMap, emptyList())

            assertThat(result.errors).isEmpty()
            assertThat(result.validRows).hasSize(1)
            assertThat(result.previews).hasSize(1)
            assertThat(result.validRows[0].userEmployeeId).isEqualTo("20030001")
            assertThat(result.validRows[0].accountId).isEqualTo(1)
        }

        @Test
        @DisplayName("종료일 미입력 - endDate null 허용")
        fun validate_nullEndDate() {
            val rows = listOf(
                createParsedRow(4, "20030001", "홍길동", "ACC001", null, "순회", "상시", "2026-04-01", null)
            )

            val result = validator.validate(rows, userMap, accountMap, emptyList())

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
                createParsedRow(4, "88888888", "없는사원", "ACC001", null, "고정", "상시", "2026-04-01", null)
            )

            val result = validator.validate(rows, userMap, accountMap, emptyList())

            assertThat(result.errors).hasSize(1)
            assertThat(result.errors[0].message).contains("존재하지 않는 사원")
        }
    }

    @Nested
    @DisplayName("V2 - 재직 상태")
    inner class V2Tests {

        @Test
        @DisplayName("퇴직한 사원 - 에러")
        fun v2_resignedEmployee() {
            val rows = listOf(
                createParsedRow(4, "99999999", "퇴직자", "ACC001", null, "고정", "상시", "2026-04-01", null)
            )

            val result = validator.validate(rows, userMap, accountMap, emptyList())

            assertThat(result.errors).hasSize(1)
            assertThat(result.errors[0].message).contains("퇴직한 사원")
        }
    }

    @Nested
    @DisplayName("V2a - 사원 앱 로그인 활성화")
    inner class V2aTests {

        @Test
        @DisplayName("appLoginActive가 false인 사원 - 에러")
        fun v2a_inactiveEmployee() {
            val inactiveUserMap = mapOf(
                "20030001" to createUser("20030001", "홍길동", "USR001", "재직", appLoginActive = false)
            )
            val rows = listOf(
                createParsedRow(4, "20030001", "홍길동", "ACC001", null, "고정", "상시", "2026-04-01", null)
            )

            val result = validator.validate(rows, inactiveUserMap, accountMap, emptyList())

            assertThat(result.errors).hasSize(1)
            assertThat(result.errors[0].message).contains("앱 로그인이 비활성화된 사원입니다")
        }

        @Test
        @DisplayName("appLoginActive가 null인 사원 - 에러")
        fun v2a_nullAppLoginActive() {
            val nullActiveUserMap = mapOf(
                "20030001" to createUser("20030001", "홍길동", "USR001", "재직", appLoginActive = null)
            )
            val rows = listOf(
                createParsedRow(4, "20030001", "홍길동", "ACC001", null, "고정", "상시", "2026-04-01", null)
            )

            val result = validator.validate(rows, nullActiveUserMap, accountMap, emptyList())

            assertThat(result.errors).hasSize(1)
            assertThat(result.errors[0].message).contains("앱 로그인이 비활성화된 사원입니다")
        }

        @Test
        @DisplayName("appLoginActive가 true인 사원 - 정상")
        fun v2a_activeEmployee() {
            val rows = listOf(
                createParsedRow(4, "20030001", "홍길동", "ACC001", null, "고정", "상시", "2026-04-01", null)
            )

            val result = validator.validate(rows, userMap, accountMap, emptyList())

            assertThat(result.errors).isEmpty()
        }

        @Test
        @DisplayName("퇴직 사원은 V2에서 이미 실패 - V2a 미실행")
        fun v2a_resignedEmployeeSkipped() {
            val rows = listOf(
                createParsedRow(4, "99999999", "퇴직자", "ACC001", null, "고정", "상시", "2026-04-01", null)
            )

            val result = validator.validate(rows, userMap, accountMap, emptyList())

            assertThat(result.errors).hasSize(1)
            assertThat(result.errors[0].message).contains("퇴직한 사원")
            assertThat(result.errors[0].message).doesNotContain("앱 로그인")
        }
    }

    @Nested
    @DisplayName("V3 - 거래처코드 존재")
    inner class V3Tests {

        @Test
        @DisplayName("존재하지 않는 거래처 - 에러")
        fun v3_accountNotFound() {
            val rows = listOf(
                createParsedRow(4, "20030001", "홍길동", "INVALID", null, "고정", "상시", "2026-04-01", null)
            )

            val result = validator.validate(rows, userMap, accountMap, emptyList())

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
                createParsedRow(4, "20030001", "홍길동", "ACC001", null, "고정", "상시", "2026-04-01", null)
            )

            val result = validator.validate(rows, userMap, closedAccountMap, emptyList())

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
                createParsedRow(4, "20030001", "홍길동", "ACC001", null, "고정", "상시", "2026-04-01", null)
            )

            val result = validator.validate(rows, userMap, exemptAccountMap, emptyList())

            assertThat(result.errors).isEmpty()
        }

        @Test
        @DisplayName("폐업 거래처 예외 허용 (ABCTypeCode 3062) - 정상")
        fun v3a_closedAccountExemptByAbcTypeCode() {
            val exemptAccountMap = mapOf(
                "ACC001" to createAccount("ACC001", "ACC_SFID_001", "이마트 강남점", accountStatusName = "폐업", accountGroup = "1010", abcTypeCode = "3062")
            )
            val rows = listOf(
                createParsedRow(4, "20030001", "홍길동", "ACC001", null, "고정", "상시", "2026-04-01", null)
            )

            val result = validator.validate(rows, userMap, exemptAccountMap, emptyList())

            assertThat(result.errors).isEmpty()
        }

        @Test
        @DisplayName("폐업 거래처 - 그룹 맞지만 배포처+ABCType 모두 미충족 - 에러")
        fun v3a_closedAccountGroupMatchButNoDistributionOrAbcType() {
            val accountMap = mapOf(
                "ACC001" to createAccount("ACC001", "ACC_SFID_001", "이마트 강남점", accountStatusName = "폐업", accountGroup = "1000", distribution = null, abcTypeCode = "1000")
            )
            val rows = listOf(
                createParsedRow(4, "20030001", "홍길동", "ACC001", null, "고정", "상시", "2026-04-01", null)
            )

            val result = validator.validate(rows, userMap, accountMap, emptyList())

            assertThat(result.errors).hasSize(1)
            assertThat(result.errors[0].message).contains("폐업 상태의 거래처입니다")
        }

        @Test
        @DisplayName("미존재 거래처는 V3에서 이미 실패 - V3a 미실행")
        fun v3a_nonExistentAccountSkipped() {
            val rows = listOf(
                createParsedRow(4, "20030001", "홍길동", "INVALID", null, "고정", "상시", "2026-04-01", null)
            )

            val result = validator.validate(rows, userMap, accountMap, emptyList())

            assertThat(result.errors).hasSize(1)
            assertThat(result.errors[0].message).contains("존재하지 않는 거래처")
            assertThat(result.errors[0].message).doesNotContain("폐업")
        }

        @Test
        @DisplayName("비활성 사원 + 폐업 거래처 동시 - 에러 2건")
        fun v3a_inactiveEmployeeAndClosedAccountBothErrors() {
            val inactiveUserMap = mapOf(
                "20030001" to createUser("20030001", "홍길동", "USR001", "재직", appLoginActive = false)
            )
            val closedAccountMap = mapOf(
                "ACC001" to createAccount("ACC001", "ACC_SFID_001", "이마트 강남점", accountStatusName = "폐업", accountGroup = "2000")
            )
            val rows = listOf(
                createParsedRow(4, "20030001", "홍길동", "ACC001", null, "고정", "상시", "2026-04-01", null)
            )

            val result = validator.validate(rows, inactiveUserMap, closedAccountMap, emptyList())

            assertThat(result.errors).hasSize(2)
            assertThat(result.errors.map { it.message }).anySatisfy { assertThat(it).contains("앱 로그인이 비활성화된 사원입니다") }
            assertThat(result.errors.map { it.message }).anySatisfy { assertThat(it).contains("폐업 상태의 거래처입니다") }
        }

        @Test
        @DisplayName("활성 거래처 - 정상 (V3a 해당 없음)")
        fun v3a_activeAccount() {
            val rows = listOf(
                createParsedRow(4, "20030001", "홍길동", "ACC001", null, "고정", "상시", "2026-04-01", null)
            )

            val result = validator.validate(rows, userMap, accountMap, emptyList())

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
                createParsedRow(4, "20030001", "홍길동", "ACC001", null, "고정", "상시", "2026-04-01", "2026-03-01")
            )

            val result = validator.validate(rows, userMap, accountMap, emptyList())

            assertThat(result.errors).hasSize(1)
            assertThat(result.errors[0].message).contains("시작일이 종료일보다 이후")
        }
    }

    @Nested
    @DisplayName("V5 - 근무유형3 유효성")
    inner class V5Tests {

        @Test
        @DisplayName("유효하지 않은 근무유형3 - 에러")
        fun v5_invalidWorkType3() {
            val rows = listOf(
                createParsedRow(4, "20030001", "홍길동", "ACC001", null, "파견", "상시", "2026-04-01", null)
            )

            val result = validator.validate(rows, userMap, accountMap, emptyList())

            assertThat(result.errors).hasSize(1)
            assertThat(result.errors[0].message).contains("유효하지 않은 근무유형3 '파견'")
        }
    }

    @Nested
    @DisplayName("V7 - 임시는 순회만")
    inner class V7Tests {

        @Test
        @DisplayName("임시 + 고정 조합 - 에러")
        fun v7_tempWithFixed() {
            val rows = listOf(
                createParsedRow(4, "20030001", "홍길동", "ACC001", null, "고정", "임시", "2026-04-01", null)
            )

            val result = validator.validate(rows, userMap, accountMap, emptyList())

            assertThat(result.errors).hasSize(1)
            assertThat(result.errors[0].message).contains("임시 배치는 순회만 가능")
        }

        @Test
        @DisplayName("임시 + 순회 - 정상")
        fun v7_tempWithRound() {
            val rows = listOf(
                createParsedRow(4, "20030001", "홍길동", "ACC001", null, "순회", "임시", "2026-04-01", "2026-04-30")
            )

            val result = validator.validate(rows, userMap, accountMap, emptyList())

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
                    employeeId = "20030001",
                    accountId = 1,
                    startDate = LocalDate.of(2026, 3, 1),
                    endDate = LocalDate.of(2026, 5, 1),
                    typeOfWork3 = "순회",
                    typeOfWork5 = "상시"
                )
            )
            val rows = listOf(
                createParsedRow(4, "20030001", "홍길동", "ACC001", null, "순회", "상시", "2026-04-01", "2026-06-01")
            )

            val result = validator.validate(rows, userMap, accountMap, existingSchedules)

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
                createParsedRow(4, "20030001", "홍길동", "ACC001", null, "순회", "상시", "2026-04-01", "2026-04-30"),
                createParsedRow(5, "20030001", "홍길동", "ACC001", null, "순회", "상시", "2026-04-15", "2026-05-15")
            )

            val result = validator.validate(rows, userMap, accountMap, emptyList())

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
            // 기존: USR001이 ACC_SFID_002에 고정 배치
            // 신규: USR001이 ACC_SFID_001에 순회 → 같은 사원/기간에 고정 존재 → C1 위반
            val existingSchedules = listOf(
                DisplayWorkSchedule(
                    employeeId = "20030001",
                    accountId = 2,
                    startDate = LocalDate.of(2026, 3, 1),
                    endDate = LocalDate.of(2026, 5, 1),
                    typeOfWork3 = "고정",
                    typeOfWork5 = "상시"
                )
            )
            val rows = listOf(
                createParsedRow(4, "20030001", "홍길동", "ACC001", null, "순회", "상시", "2026-04-01", "2026-04-30")
            )

            val result = validator.validate(rows, userMap, accountMap, existingSchedules)

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
            // 기존: USR001이 다른 거래처 2곳에 격고 배치
            // 신규: USR001이 ACC001에 격고 → C2 위반 (이미 2개)
            val existingSchedules = listOf(
                DisplayWorkSchedule(employeeId = "20030001", accountId = 101, startDate = LocalDate.of(2026, 3, 1), endDate = LocalDate.of(2026, 5, 1), typeOfWork3 = "격고", typeOfWork5 = "상시"),
                DisplayWorkSchedule(employeeId = "20030001", accountId = 102, startDate = LocalDate.of(2026, 3, 1), endDate = LocalDate.of(2026, 5, 1), typeOfWork3 = "격고", typeOfWork5 = "상시")
            )
            val rows = listOf(
                createParsedRow(4, "20030001", "홍길동", "ACC001", null, "격고", "상시", "2026-04-01", "2026-04-30")
            )

            val result = validator.validate(rows, userMap, accountMap, existingSchedules)

            assertThat(result.errors).hasSize(1)
            assertThat(result.errors[0].message).contains("격고 배치가 이미 2개 존재")
        }
    }

    @Nested
    @DisplayName("C3 - 임시 최대 1개")
    inner class C3Tests {

        @Test
        @DisplayName("임시 1개 존재 시 추가 불가 - 에러")
        fun c3_tempLimit() {
            // 기존: USR001이 다른 거래처에 임시 배치
            // 신규: USR001이 ACC001에 임시 → C3 위반
            val existingSchedules = listOf(
                DisplayWorkSchedule(employeeId = "20030001", accountId = 101, startDate = LocalDate.of(2026, 3, 1), endDate = LocalDate.of(2026, 5, 1), typeOfWork3 = "순회", typeOfWork5 = "임시")
            )
            val rows = listOf(
                createParsedRow(4, "20030001", "홍길동", "ACC001", null, "순회", "임시", "2026-04-01", "2026-04-30")
            )

            val result = validator.validate(rows, userMap, accountMap, existingSchedules)

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
            typeOfWork5 = typeOfWork5,
            startDateStr = startDateStr,
            endDateStr = endDateStr,
            startDate = startDate,
            endDate = endDate
        )
    }

    private fun createUser(
        employeeId: String,
        name: String,
        sfid: String,
        status: String,
        appLoginActive: Boolean? = true
    ): User = User(
        id = 1L,
        employeeId = employeeId,
        name = name,
        sfid = sfid,
        status = status,
        appAuthority = null,
        appLoginActive = appLoginActive
    )

    private fun createAccount(
        externalKey: String,
        sfid: String,
        name: String,
        id: Int = 1,
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
