package com.otoki.powersales.domain.org.employee.enums

import com.otoki.powersales.platform.auth.entity.AppAuthority

/**
 * 여사원 인원현황 모수 정의 — 레거시 SF 홈 대시보드(조장) 인원현황 리포트와 동일한 필터 축.
 *
 * 레거시 원본: `reports/X00/new_report_72Y.report-meta.xml`
 * ("판촉직/OSC직 인원현황(대시보드 전용)", `reportType=CustomEntity$DKRetail__Employee__c`).
 * 리포트 필터 4개(AND) 를 그대로 옮긴다:
 *
 * | 레거시 필터 | 신규 대응 |
 * |---|---|
 * | `DKRetail__JobCode__c` equals `판촉직,레이디직,OSC직` | [FemaleStaffJobCode.ALL_CODES] |
 * | `DKRetail__Status__c` notEqual `퇴직` | `status IS NULL OR status <> '퇴직'` (호출부) |
 * | `DKRetail__AppAuthority__c` equals `조장,여사원` | [ROLES] |
 * | 사원명 notContain `테스트,관리자,파워세일즈` | [EXCLUDED_NAME_KEYWORDS] |
 *
 * 이 모수는 **대시보드 기본현황 집계와 여사원 현황 목록이 공유**한다 — 두 화면의 총원이 어긋나면
 * 사용자가 같은 지점에서 다른 숫자를 보게 되므로, 축을 바꿀 때는 항상 양쪽을 함께 검토할 것.
 *
 * 지점 스코프는 본 정의에 포함하지 않는다. 레거시 리포트는 `scope=organization` 이고, 지점 제한은
 * 대시보드의 `dashboardType=LoggedInUser` + OWD Private 조합(로그인 사용자 가시성)에서 나온다.
 * 신규는 각 화면의 resolver 가 명시적으로 산출한다.
 */
object FemaleStaffHeadcountFilter {

    /**
     * 인원현황 모수에 포함하는 앱 권한 — 여사원 + 조장.
     * 레거시 리포트 필터 `AppAuthority__c IN ('조장','여사원')` 정합. 조장은 여사원 조직을 관리하는
     * 직책이라 인원현황에 함께 계상한다.
     *
     * 주의: 같은 대시보드의 "여사원 만나이 현황"(`new_report_fJs`) 은 `AppAuthority = '여사원'` 단독으로
     * 조장을 제외한다 — `Age__c` formula 가 여사원일 때만 값을 내기 때문. 컴포넌트마다 축이 다르므로
     * 본 상수를 만나이 집계에 그대로 재사용하지 말 것.
     */
    val ROLES: List<String> = listOf(AppAuthority.WOMAN, AppAuthority.LEADER)

    /**
     * 인원현황 모수에서 제외하는 사원명 키워드 (부분 일치, 대소문자 무시).
     * 레거시 리포트 필터 `CUST_NAME notContain '테스트,관리자,파워세일즈'` 정합 — 운영 데이터에 섞인
     * 테스트/시스템 계정을 인원수에서 걷어낸다. 누락하면 실제보다 인원이 부풀려진다.
     */
    val EXCLUDED_NAME_KEYWORDS: List<String> = listOf("테스트", "관리자", "파워세일즈")

    /**
     * 직급별 인원현황 표의 '판매조장' 그룹 판정값 — [com.otoki.powersales.domain.org.employee.entity.Employee.jikchak]
     * (직책명) 이 이 값이면 판매조장 열에 계상한다.
     *
     * 판정 축이 [ROLES] 의 '조장'([com.otoki.powersales.platform.auth.entity.AppAuthority.LEADER]) 과
     * 완전히 일치하지는 않는다 (운영 실측: jikchak='판매조장' 36명 중 role='여사원' 1명이 섞여 있고,
     * role='조장' 이면서 jikchak 이 null 인 2명은 빠진다). 표 헤더 문자열과 축을 일치시키기 위한
     * 사용자 결정이며, 그 결과 이 표의 총합계는 대시보드 총원과 1명 차이가 날 수 있다.
     */
    const val LEADER_JIKCHAK = "판매조장"
}
