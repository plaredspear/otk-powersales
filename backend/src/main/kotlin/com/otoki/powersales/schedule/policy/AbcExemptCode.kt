package com.otoki.powersales.schedule.policy

/**
 * 출근 등록 GPS 거리 검증 면제 ABC 코드 (Spec #586).
 *
 * 대리점/특수거래처 등 사원이 매장에 직접 방문하기 어려운 채널은
 * 거리 검증을 우회한다. 레거시 Heroku `HomeController#commuteReg` 의 인라인 콤마
 * 문자열 + `indexOf` 부분일치 결함을 정확 일치(equality) 비교로 정형화했다.
 *
 * 변경 절차: 코드 변경 + 배포. 운영 콘솔 / DB 마스터 / 환경변수 미사용.
 *
 * 매칭 규칙:
 * - 입력은 trim/lowercase 정규화 없이 그대로 비교한다 (마스터 값은 SAP 코드 원본 사용).
 * - null / 빈 문자열 / enum 값과 불일치하면 면제 아님 (`false`).
 */
enum class AbcExemptCode(val code: String) {
    CODE_1110("1110"),
    CODE_1120("1120"),
    CODE_1130("1130"),
    CODE_1140("1140"),
    CODE_1210("1210"),
    CODE_1220("1220"),
    CODE_1510("1510"),
    CODE_1530("1530"),
    CODE_1810("1810"),
    CODE_1900("1900");

    companion object {
        private val CODES: Set<String> = entries.map { it.code }.toSet()

        /**
         * 입력 ABC 코드가 면제 목록에 정확 일치하는지 판정한다.
         *
         * @param code Account.abcTypeCode 값 (`null` 가능)
         * @return 면제 코드면 `true`, 그 외(공백/null/불일치/부분일치) 모두 `false`
         */
        fun isExempt(code: String?): Boolean {
            if (code.isNullOrEmpty()) return false
            return code in CODES
        }
    }
}
