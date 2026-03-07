package com.otoki.internal.notice.entity

/**
 * 공지사항 분류 Enum
 * - dbValue: DB 컬럼(dkretail__category__c)에 저장되는 Salesforce Picklist 원본값
 * - apiCode: API 요청/응답에 사용되는 코드
 * - displayName: 화면 표시용 한글명
 */
enum class NoticeCategory(
    val displayName: String,
    val dbValue: String,
    val apiCode: String
) {
    COMPANY("회사공지", "회사공지", "COMPANY"),
    BRANCH("지점공지", "영업부/지점공지", "BRANCH"),
    EDUCATION("교육", "교육", "EDUCATION");

    companion object {
        fun fromApiCode(code: String): NoticeCategory {
            return entries.find { it.apiCode == code }
                ?: throw IllegalArgumentException("Invalid category: $code")
        }

        fun fromDbValue(value: String): NoticeCategory {
            return entries.find { it.dbValue == value }
                ?: throw IllegalArgumentException("Invalid db value: $value")
        }
    }
}
