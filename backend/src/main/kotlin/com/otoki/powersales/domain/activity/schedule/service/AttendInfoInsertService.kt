package com.otoki.powersales.domain.activity.schedule.service

import com.otoki.powersales.domain.activity.schedule.entity.AttendInfo
import com.otoki.powersales.domain.activity.schedule.enums.AttendType
import com.otoki.powersales.domain.activity.schedule.repository.AttendInfoRepository
import com.otoki.powersales.domain.activity.schedule.service.dto.AttendInfoInsertCommand
import com.otoki.powersales.domain.activity.schedule.service.dto.AttendInfoInsertFailedRow
import com.otoki.powersales.domain.activity.schedule.service.dto.AttendInfoInsertResult
import org.springframework.stereotype.Service

/**
 * 출근 정보 INSERT 도메인 서비스 (단일 청크 단위).
 *
 * ## 레거시 매핑
 * - 진입점: SAP 인바운드 어댑터 [com.otoki.powersales.external.sap.inbound.service.SapAttendInfoService]
 * - origin spec: #562 (SAP 출근 정보 인바운드) — 어댑터/도메인 분리: #635 P2-B
 *
 * ## 레거시 동작 요약
 * 1. 입력: `List<AttendInfoInsertCommand>` — 어댑터가 분할한 단일 청크. INSERT only, 멱등성 미보장.
 * 2. 레거시 정합 — 수신 필드 명시 필수/형식 검증으로 행을 거부하지 않는다 (레거시 IF_REST_SAP_AttendInfo 에
 *    검증 게이트 없음, raw 문자열 그대로 저장 — 날짜 변환조차 안 함). 4필드 필수·날짜 형식 검증을 제거하고
 *    raw 적재한다. AttendType 룩업 ([AttendType.Companion.fromCode]) 실패 시 원본 코드 그대로 저장 (D3 결정).
 * 3. 외부 호출: [AttendInfoRepository.saveAll] (전 행 일괄).
 *    적재된 entity 는 [AttendInfoInsertResult.savedAttendInfos] 로 return — 어댑터가 후처리 (Schedule 변환) 호출 시 사용.
 *
 * ## 신규 차이 — 동등 (생략)
 *
 * 트랜잭션 경계는 어댑터의 [com.otoki.powersales.external.sap.inbound.service.ChunkedUpsertHelper] (`REQUIRES_NEW`) 가 청크 단위로 부여한다.
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
            // 레거시 IF_REST_SAP_AttendInfo 정합 — 수신 필드에 대한 명시적 필수/형식 검증으로 행을
            // 거부하는 코드가 레거시에 전무하다 (EmployeeCode/StartDate/EndDate/AttendType/Status 를
            // 무검증 raw 매핑, 날짜는 Text 컬럼에 문자열 그대로 저장 — convertStringToDate 미사용,
            // Database.insert allOrNone=false). 신규도 4필드 필수·날짜 형식 검증을 제거하고 raw 적재한다.
            val employeeCode = command.employeeCode?.takeIf { it.isNotBlank() }
            val startDate = command.startDate?.takeIf { it.isNotBlank() }
            val endDate = command.endDate?.takeIf { it.isNotBlank() }
            val attendType = command.attendType?.takeIf { it.isNotBlank() }

            // AttendType 룩업: 매칭 실패 시 원본 코드 그대로 저장 (D3 결정, 거부하지 않음)
            attendType?.let { AttendType.fromCode(it) }

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
}
