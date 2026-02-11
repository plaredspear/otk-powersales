package com.otoki.internal.entity

/**
 * 클레임 사진 유형 Enum
 * 클레임 신고 시 첨부하는 사진의 유형을 구분한다.
 */
enum class ClaimPhotoType {
    DEFECT,  // 불량 사진
    LABEL,   // 일부인 사진
    RECEIPT  // 구매 영수증 사진
}
