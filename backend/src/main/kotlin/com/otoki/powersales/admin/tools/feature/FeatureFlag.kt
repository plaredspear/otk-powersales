package com.otoki.powersales.admin.tools.feature

/**
 * 개발자 도구 > 대시보드 > 기능 활성화 로 on/off 하는 기능 토글(feature flag) 목록.
 *
 * 상태는 Redis 에 지속 저장되어 앱 재시작 후에도 유지되며(로그 레벨과 달리 임시 조정이 아님),
 * 미설정 시 기본은 **활성**이다. flag 별로 **예외 사번** 목록을 둘 수 있다.
 *
 * flag 는 용도에 따라 두 종류다:
 * - **차단형** — 특정 등록(create) API 를 런타임에 차단/허용한다. 비활성 시 409 로 요청이 거부되고,
 *   예외 사번은 비활성 상태에서도 등록할 수 있다. 컨트롤러에서 `ensureEnabled` 로 게이트한다.
 * - **동작 전환형** — 신규 기능 적용 여부를 전환한다(활성=신규 동작, 비활성=이전 동작). 요청을
 *   거부하지 않으므로 `ensureEnabled` 가 아니라 `isEnabled` 로 분기한다. 신규 기능 배포 후
 *   문제가 드러났을 때 배포 없이 되돌리기 위한 스위치이며, 안정화되면 flag 와 이전 경로를 함께 제거한다.
 *
 * Redis 미가동/장애 시 **활성 폴백**이므로(FeatureToggleStore.getState), 동작 전환형 flag 는
 * "활성 = 신규(더 안전한 쪽)" 이 되도록 극성을 잡아야 장애 시 안전한 방향으로 떨어진다.
 *
 * [code] 는 Redis key 와 API 계약(웹/모바일)에 노출되는 안정 식별자이므로 함부로 바꾸지 않는다.
 * [label] 은 관리자 화면 및 차단 안내 문구에 쓰이는 한글 표시명이다.
 */
enum class FeatureFlag(val code: String, val label: String) {
    /** 제품 클레임 등록 (POST /api/v1/mobile/claims). 차단형. */
    PRODUCT_CLAIM("PRODUCT_CLAIM", "제품 클레임 등록"),

    /** 물류 클레임 등록 (POST /api/v1/mobile/suggestions, category=LOGISTICS_CLAIM 포함). 차단형. */
    LOGISTICS_CLAIM("LOGISTICS_CLAIM", "물류 클레임 등록"),

    /** 주문 등록 (POST /api/v1/mobile/order-requests). 차단형. */
    ORDER_REQUEST("ORDER_REQUEST", "주문 등록"),

    /**
     * 출근등록 일정 소유자/일자 검증 (POST /api/v1/mobile/attendance 의 scheduleId 분기). 동작 전환형.
     *
     * - 활성(기본) = 신규: 요청 사원 본인의 오늘 일정만 등록 허용.
     * - 비활성 = 이전: 일정 존재 여부만 확인 (타인 일정·다른 일자 등록 가능).
     *
     * 진열/행사/대리출근 경로가 이미 갖춘 가드를 scheduleId 분기에도 맞춘 변경이라, 정상 사용자는
     * 영향받지 않는다(대상 목록이 본인·오늘 일정만 내려줌). 예상 못 한 차단이 발생했을 때
     * 배포 없이 되돌리기 위한 스위치다.
     */
    ATTENDANCE_SCHEDULE_OWNER_DATE_CHECK(
        "ATTENDANCE_SCHEDULE_OWNER_DATE_CHECK",
        "출근등록 일정 소유자/일자 검증",
    ),

    /**
     * 주문서 작성 거래처를 진열마스터 기준으로 한정 (GET /api/v1/mobile/accounts/my?scope=order_write).
     * 동작 전환형.
     *
     * - 활성(기본) = 신규: 여사원 경로에서 **확정(Confirmed) + 오늘이 진열 기간 안**인
     *   `DisplayWorkScheduleMaster` 의 거래처만 노출한다.
     * - 비활성 = 이전: 팀멤버스케줄(전월 25일~당월 말일) ∪ 진열 일정 (레거시 `accountSelectList order=order`).
     *
     * 팀멤버스케줄(TMS)은 출근등록 시점에 생성되는 **실적 기록**이라 주문 작성 시점의 근무 예정을
     * 나타내지 못하고, 행사 일정(`Promotion → PromotionEmployee → TMS`) 거래처까지 포함한다.
     * 행사 근무에는 주문서를 작성하지 않으므로 계획 마스터인 진열마스터를 기준으로 바꾼 것이다.
     *
     * 조회 필터(`scope=order`)는 이 전환의 대상이 아니다 — 진열이 끝난 거래처로 과거 주문을
     * 검색해야 하므로 기존 범위를 유지한다.
     *
     * 주의: 이 flag 는 "활성 = 더 좁은 쪽" 이라 Redis 장애 시 활성 폴백이 신규(좁은) 동작이 된다.
     * 관리자 일정 등록 등으로 진열마스터 없이 TMS 만 존재하는 거래처는 노출되지 않으므로,
     * 그런 사례가 보고되면 이 flag 를 비활성화해 이전 동작으로 되돌린다.
     */
    ORDER_ACCOUNT_DISPLAY_SCHEDULE_ONLY(
        "ORDER_ACCOUNT_DISPLAY_SCHEDULE_ONLY",
        "주문서 거래처 진열마스터 기준",
    ),
    ;

    companion object {
        /** [code] 로 flag 를 찾는다. 없으면 null. */
        fun fromCode(code: String): FeatureFlag? = entries.find { it.code == code }
    }
}
