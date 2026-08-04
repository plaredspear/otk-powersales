package com.otoki.powersales.domain.activity.schedule.dto.response

import com.fasterxml.jackson.annotation.JsonIgnore
import java.time.LocalDateTime

/**
 * 판매여사원 일일 안전점검 현황 (RPA용) 응답 (Spec #842).
 *
 * 레거시 매핑: SF Report `X00/new_report_xdB` (RPA용·scope=organization·25컬럼).
 * #841 영업지원실용과 동일하되 마지막 컬럼이 CommuteDate 대신 여사원일정 스케줄번호.
 * 레거시는 전사 고정이나 신규는 지점 스코프(branchCode 선택) 적용 — 기록된 이탈.
 */
data class FemaleEmployeeSafetyCheckRpaResponse(
    val date: String,
    val items: List<FemaleEmployeeSafetyCheckRpaItem>,
)

/**
 * 안전점검 현황 (RPA) 1행 — (여사원일정 1건) × employee × account 조인.
 *
 * 25컬럼: #841 컬럼 + 여사원일정 스케줄번호(CommuteDate 대체). enum 필드는 `@JsonValue` 로 한글 displayName 직렬화.
 * scheduleName = 여사원일정 레코드의 스케줄번호(TeamMemberSchedule.name, `TS{00000000}` AutoNumber). 부재 시 null.
 * checkTime 은 한국어 표기 문자열(`2026. 8. 1. 오후 1:36`), checkTimeAt 은 엑셀 날짜 셀 전용 원본값(JSON 미노출).
 */
data class FemaleEmployeeSafetyCheckRpaItem(
    val employeeCode: String,
    val ladyName: String,
    val employeeOrgName: String?,
    val accountType: String?,
    val accountSapCode: String?,
    val accountName: String?,
    val workingCategory1: String?,
    val checkTime: String?,
    /** 엑셀 날짜 셀 전용 원본 점검시각 — 응답 JSON 에는 포함하지 않는다 (표기값은 [checkTime]). */
    @get:JsonIgnore
    val checkTimeAt: LocalDateTime?,
    val isWorkReport: String?,
    val hrCode: String?,
    val equipment1: String?,
    val equipment2: String?,
    val equipment3: String?,
    val equipment4: String?,
    val equipment5: String?,
    val equipment6: String?,
    val equipment7: String?,
    val equipment8: String?,
    val equipment9: String?,
    val precaution: String?,
    val precautionChk: Double?,
    val workingCategory2: String?,
    val workingCategory3: String?,
    val secondWorkType: String?,
    val scheduleName: String?,
)
