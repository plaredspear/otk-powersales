package com.otoki.powersales.platform.common.util

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * 화면/엑셀 공통 한국어 일시 표기 유틸.
 *
 * ISO-8601 (`2026-08-01T13:36:33.601624`) 은 사용자 화면 표기로 부적합하여, 레거시 SF Report 표기와 동일한
 * `2026. 8. 1. 오후 1:36` 형태로 변환한다. 엑셀은 문자열이 아니라 **날짜 셀 + 표시 서식**으로 내보내
 * (정렬/필터가 날짜로 동작) 화면과 동일한 표기를 유지한다.
 */
object DateTimeDisplay {

    /** `2026. 7. 24. 오후 10:26` — 화면(JSON) 표기용. */
    private val KOREAN_DATE_TIME: DateTimeFormatter =
        DateTimeFormatter.ofPattern("yyyy. M. d. a h:mm", Locale.KOREAN)

    /**
     * 위 패턴과 동일 표기를 내는 엑셀 셀 서식 코드.
     *
     * `[$-412]` 는 ko-KR 로케일 강제 지정 — 이게 없으면 `AM/PM` 이 뷰어 로케일에 따라 `AM`/`PM` 으로 표시된다.
     */
    const val KOREAN_DATE_TIME_EXCEL_FORMAT = "[\$-412]yyyy. m. d. AM/PM h:mm"

    /** null 이면 null. */
    fun koreanDateTime(value: LocalDateTime?): String? = value?.format(KOREAN_DATE_TIME)
}
