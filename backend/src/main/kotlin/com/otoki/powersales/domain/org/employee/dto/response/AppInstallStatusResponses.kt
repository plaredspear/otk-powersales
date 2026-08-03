package com.otoki.powersales.domain.org.employee.dto.response

import com.otoki.powersales.domain.org.employee.entity.Employee

/**
 * 앱 미설치 추정 여사원 1건 — 설치 안내 대상.
 *
 * 안내 발송에 필요한 최소 식별 정보(사번 / 이름 / 지점명) 만 노출한다.
 */
data class AppUninstalledFemaleStaffItem(
    val employeeCode: String,
    val name: String,
    val branchName: String?,
) {
    companion object {
        fun from(employee: Employee): AppUninstalledFemaleStaffItem = AppUninstalledFemaleStaffItem(
            // 조회 술어가 사번 보유 사원만 남기므로 null 은 발생하지 않는다 (방어적 fallback).
            employeeCode = employee.employeeCode ?: "",
            name = employee.name,
            // 지점명 = 사원 소속 조직명. SF `BranchName__c` (= `FullName__r.DKRetail__OrgName__c`) 와 동일 축이며
            // 여사원 현황 목록의 「소속」 컬럼과 같은 값이다.
            branchName = employee.orgName,
        )
    }
}

/**
 * 앱 미설치 추정 여사원 집계 — 화면 수치 표시용.
 *
 * [uninstalledCount] 는 엑셀 다운로드 행 수와 항상 일치한다 (같은 조회 결과에서 산출).
 */
data class AppUninstalledFemaleStaffSummary(
    val uninstalledCount: Int,
    val targetCount: Long,
)
