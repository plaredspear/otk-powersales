package com.otoki.powersales.schedule.service

import com.otoki.powersales.schedule.entity.AttendInfo
import com.otoki.powersales.schedule.entity.AttendType
import com.otoki.powersales.schedule.repository.AttendInfoRepository
import com.otoki.powersales.schedule.service.dto.AttendInfoInsertCommand
import com.otoki.powersales.schedule.service.dto.AttendInfoInsertFailedRow
import com.otoki.powersales.schedule.service.dto.AttendInfoInsertResult
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

/**
 * 출근 정보 INSERT 도메인 서비스 (단일 청크 단위).
 *
 * ## 레거시 매핑
 * - 진입점: SAP 인바운드 어댑터 [com.otoki.powersales.sap.inbound.service.SapAttendInfoService]
 * - origin spec: #562 (SAP 출근 정보 인바운드) — 어댑터/도메인 분리: #635 P2-B
 *
 * ## 레거시 동작 요약
 * 1. 입력: `List<AttendInfoInsertCommand>` — 어댑터가 분할한 단일 청크. INSERT only, 멱등성 미보장.
 * 2. 행 단위 검증:
 *    - 필수값 (`employeeCode`/`startDate`/`endDate`/`attendType`) 누락 → failures.
 *    - StartDate / EndDate (`yyyyMMdd`) 형식 위반 → failures.
 *    - AttendType 룩업 ([AttendType.fromCode]) 실패 시 원본 코드 그대로 저장 (D3 결정, 거부하지 않음).
 *    - 정상 행: 신규 [AttendInfo] 생성.
 * 3. 외부 호출: [AttendInfoRepository.saveAll] (성공 행만 일괄). 행 검증 실패는 트랜잭션 롤백하지 않음.
 *    적재된 entity 는 [AttendInfoInsertResult.savedAttendInfos] 로 return — 어댑터가 후처리 (Schedule 변환) 호출 시 사용.
 *
 * ## 신규 차이 — 동등 (생략)
 *
 * 트랜잭션 경계는 어댑터의 [com.otoki.powersales.sap.inbound.service.ChunkedUpsertHelper] (`REQUIRES_NEW`) 가 청크 단위로 부여한다.
 * 도메인 서비스 자체에는 `@Transactional` 을 부착하지 않는다.
 *
 * `sap.*` 패키지 의존 0건 — `ChunkedUpsertHelper` / `AttendInfoToScheduleConverter` / audit 침투 금지.
 */
@Service
class AttendInfoInsertService(
    private val attendInfoRepository: AttendInfoRepository
) {

    fun insert(commands: List<AttendInfoInsertCommand>): AttendInfoInsertResult {
        val failures = mutableListOf<AttendInfoInsertFailedRow>()
        val toSave = mutableListOf<AttendInfo>()

        commands.forEach { command ->
            val employeeCode = command.employeeCode?.takeIf { it.isNotBlank() }
            val startDate = command.startDate?.takeIf { it.isNotBlank() }
            val endDate = command.endDate?.takeIf { it.isNotBlank() }
            val attendType = command.attendType?.takeIf { it.isNotBlank() }

            if (employeeCode == null) {
                failures += AttendInfoInsertFailedRow(null, "EmployeeCode 필수")
                return@forEach
            }
            if (startDate == null) {
                failures += AttendInfoInsertFailedRow(employeeCode, "StartDate 필수")
                return@forEach
            }
            if (endDate == null) {
                failures += AttendInfoInsertFailedRow(employeeCode, "EndDate 필수")
                return@forEach
            }
            if (attendType == null) {
                failures += AttendInfoInsertFailedRow(employeeCode, "AttendType 필수")
                return@forEach
            }
            if (!isValidYyyymmdd(startDate)) {
                failures += AttendInfoInsertFailedRow(employeeCode + startDate, "StartDate YYYYMMDD 형식 오류: $startDate")
                return@forEach
            }
            if (!isValidYyyymmdd(endDate)) {
                failures += AttendInfoInsertFailedRow(employeeCode + endDate, "EndDate YYYYMMDD 형식 오류: $endDate")
                return@forEach
            }

            // AttendType 룩업: 매칭 실패 시 원본 코드 그대로 저장 (D3 결정, 거부하지 않음)
            AttendType.fromCode(attendType)

            toSave += AttendInfo(
                employeeCode = employeeCode,
                startDate = startDate,
                endDate = endDate,
                attendType = attendType,
                status = command.status
            )
        }

        val saved = if (toSave.isNotEmpty()) {
            attendInfoRepository.saveAll(toSave).toList()
        } else {
            emptyList()
        }

        return AttendInfoInsertResult(
            successCount = saved.size,
            failureCount = failures.size,
            failures = failures,
            savedAttendInfos = saved
        )
    }

    private fun isValidYyyymmdd(value: String): Boolean = try {
        LocalDate.parse(value, DATE_FORMAT)
        true
    } catch (_: DateTimeParseException) {
        false
    }

    companion object {
        private val DATE_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMdd")
    }
}
