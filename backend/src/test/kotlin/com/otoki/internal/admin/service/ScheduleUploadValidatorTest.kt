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
        "ACC001" to createAccount("ACC001", "ACC_SFID_001", "이마트 강남점"),
        "ACC002" to createAccount("ACC002", "ACC_SFID_002", "홈플러스 역삼점")
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
            assertThat(result.validRows[0].accountSfid).isEqualTo("ACC_SFID_001")
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
                    fullName = "20030001",
                    account = "ACC_SFID_001",
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
                    fullName = "20030001",
                    account = "ACC_SFID_002",
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
                DisplayWorkSchedule(fullName = "20030001", account = "ACC_SFID_X1", startDate = LocalDate.of(2026, 3, 1), endDate = LocalDate.of(2026, 5, 1), typeOfWork3 = "격고", typeOfWork5 = "상시"),
                DisplayWorkSchedule(fullName = "20030001", account = "ACC_SFID_X2", startDate = LocalDate.of(2026, 3, 1), endDate = LocalDate.of(2026, 5, 1), typeOfWork3 = "격고", typeOfWork5 = "상시")
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
                DisplayWorkSchedule(fullName = "20030001", account = "ACC_SFID_X1", startDate = LocalDate.of(2026, 3, 1), endDate = LocalDate.of(2026, 5, 1), typeOfWork3 = "순회", typeOfWork5 = "임시")
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
        status: String
    ): User = User(
        id = 1L,
        employeeId = employeeId,
        name = name,
        sfid = sfid,
        status = status,
        appAuthority = null,
        appLoginActive = true
    )

    private fun createAccount(
        externalKey: String,
        sfid: String,
        name: String
    ): Account = Account(
        id = 1,
        externalKey = externalKey,
        sfid = sfid,
        name = name
    )
}
