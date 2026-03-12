package com.otoki.internal.promotion.entity

enum class StandLocation(
    val displayName: String,
    val displayOrder: Int
) {
    FROZEN_EVENT("냉동행사장", 1),
    ISLAND("아일랜드", 2),
    END_CAP("엔드", 3),
    FLAT_TABLE("평대", 4),
    FOOD_TRUCK("푸드트럭", 5),
    EVENT_STAND("행사매대", 6);

    companion object {
        fun fromDisplayName(name: String): StandLocation? =
            entries.find { it.displayName == name }
    }
}
