package com.otoki.powersales.product.entity

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue

/**
 * Salesforce `NewProduct__c.ManagementType__c` picklist enum (관리유형/제품 분류).
 *
 * 단일 권위: docs/plan/old_source_260408/sf-object-meta/_raw/NewProduct__c.json picklistValues (count=84)
 * Spec #737 §3.5 — SF picklist 정의 그대로 enum 변환.
 *
 * enum 식별자는 영문 식별 명칭이며, displayName 이 SF 원본 한국어 옵션값.
 */
enum class ManagementType(
    val displayName: String
) {
    COMMON("공통"),
    THREE_CLASSIFICATION("3분류"),
    LL_NOODLES("LL면류"),
    MUSTARD("겨자류"),
    EGG_AND_EGG_PRODUCTS("계란및난가공품류"),
    CANNED_FRUITS("과일통조림류"),
    SOUP_AND_STEW("국·찌개류"),
    NOODLES("국수류"),
    NOODLE_BROTH("국수장국류"),
    OTHER_AGRICULTURAL_PRODUCTS("기타농산가공품류"),
    OTHER_PASTES("기타장류"),
    OTHER_PRODUCTS("기타제품류"),
    NATTO("낫또류"),
    FROZEN_CONVENIENCE_FOODS("냉동간편식류"),
    FROZEN_BREADS("냉동빵류"),
    REFRIGERATED_CONVENIENCE_FOODS("냉장간편식류"),
    REFRIGERATED_NOODLES("냉장면류"),
    SCORCHED_RICE("누룽지류"),
    TEA("다류"),
    KELP("다시마류"),
    DRESSINGS("드레싱류"),
    PERILLA_OIL("들기름류"),
    DESSERTS("디저트류"),
    RICE_CAKES("떡류"),
    RAMEN_SNACKS("라면스낵류"),
    MICROWAVE_FOODS("렌지류"),
    MARGARINE("마아가린류"),
    MAYONNAISE("마요네스류"),
    DUMPLINGS("만두류"),
    SYRUP_STARCH("물엿류"),
    SEAWEED("미역류"),
    COOKING_WINE("미향류"),
    RICE_TOPPINGS("밥친구류"),
    HONEY("벌꿀류"),
    ROASTED_SESAME("볶음참깨류"),
    BAG_NOODLES("봉지면류"),
    POWDER_PORRIDGE("분말죽류"),
    POWDER_JJAJANG("분말짜장류"),
    GIFT_SET("선물세트"),
    SAUCES("소스류"),
    SEAFOOD_PRODUCTS("수산가공품류"),
    IMPORTED_PASTA("수입파스타류"),
    SWEET_CORN("스위트콘류"),
    SPAGHETTI_SAUCE("스파게티소스류"),
    SOUP("스프류"),
    SYRUP("시럽류"),
    COOKING_OIL("식용유"),
    VINEGAR("식초류"),
    GLASS_NOODLES("당면류"),
    LIQUID_SOUP("액상스프류"),
    SEASONING_SAUCE("양념장류"),
    ICE("얼음류"),
    OLD_STYLE_JAPCHAE("옛날잡채류"),
    OTTOGI_RICE("오뚜기밥류"),
    OTTOGI_RAW_RICE("오뚜기쌀류"),
    WASABI("와사비류"),
    CUP_NOODLES_BOWL("용기면류"),
    CUP_PORRIDGE("용기죽류"),
    MEAT_SAUCE("육류소스류"),
    DRINKING_VINEGAR("음용식초류"),
    LEE_KUM_KEE_SAUCE("이금기소스류"),
    COOKED_FROZEN("조리냉동류"),
    SEASONINGS("조미료류"),
    INSTANT_SOUP("즉석국류"),
    JAM("쨈류"),
    SESAME_OIL("참기름류"),
    TUNA("참치류"),
    CHO_GOCHUJANG("초고추장류"),
    CHEESE("치즈류"),
    CURRY("카레류"),
    CANNED_HAM("캔햄류"),
    CUP_NOODLE_BRAND("컵누들류"),
    CUP_NOODLES("컵면류"),
    CUP_RICE("컵밥류"),
    KETCHUP("케찹류"),
    TABASCO_SAUCE("타바스코소스류"),
    STEW("탕류"),
    POUCH_PORRIDGE("파우치죽류"),
    PREMIX("프리믹스류"),
    PICKLES("피클류"),
    HASH("하이스류"),
    FLAVORED_OIL("향미유류"),
    SPICES("향신료류"),
    PEPPER("후추류");

    @JsonValue
    fun toJson(): String = displayName

    companion object {
        @JvmStatic
        @JsonCreator
        fun fromDisplayName(value: String): ManagementType =
            entries.find { it.displayName == value }
                ?: throw IllegalArgumentException("유효하지 않은 관리유형: $value")

        fun fromDisplayNameOrNull(value: String?): ManagementType? {
            if (value.isNullOrBlank()) return null
            return entries.find { it.displayName == value }
        }
    }
}
