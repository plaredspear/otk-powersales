package com.otoki.powersales.agreement.service

import com.otoki.powersales.agreement.dto.request.AdminAgreementWordCreateRequest
import com.otoki.powersales.agreement.dto.response.AdminAgreementWordActiveResponse
import com.otoki.powersales.agreement.dto.response.AdminAgreementWordCreateResponse
import com.otoki.powersales.common.entity.AgreementWord
import com.otoki.powersales.common.repository.AgreementWordRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 관리자 웹 약관 등록 + 활성 약관 조회 도메인 서비스. (Spec #658 P1-B)
 *
 * ## 레거시 매핑
 * - SF Apex: `AgreementWordTrigger.trigger:1-3` (beforeInsert) → `AgreementWordTriggerHandler.cls:33-56` (`AgreementWordBeforeInsert`)
 * - flow-legacy: flow-legacy yaml (Spec #655)
 * - origin spec: #658 (#655 audit 후속 인계)
 *
 * ## 레거시 동작 요약
 * 1. 입력: 시스템 관리자가 SF Setup → Object Manager → 동의 문구 → New 클릭으로 단일 row 등록.
 * 2. AgreementWordTrigger before insert → `AgreementWordBeforeInsert()` 가 검증 게이트 3건 fire:
 *    (a) AfterActiveDate 중복 (`agreementList[0]` 만 비교 — loop 미실행 버그) → addError
 *    (b) `ActiveDate__c != null` → addError
 *    (c) `Active__c == true` → addError
 * 3. `Test.isRunningTest()` 인 경우 모든 검증 우회.
 * 4. 검증 통과 시 SF 표준 INSERT — `Active=False`, `ActiveDate=null` 강제. Heroku Connect 가 SF→Postgres 단방향 동기.
 *
 * ## 신규 차이
 * - **진입점**: SF Setup 단일 채널 → 관리자 웹 (`POST /api/v1/admin/agreement-words`). 결정 근거: 스펙 §3 Q1.
 * - **검증 게이트 (b)(c)**: SF Trigger addError → DTO Bean Validation (`@AssertFalse` / `@Null`) + Service 단 fallback (active=false / activeDate=null 강제 적재). 결정 근거: 스펙 §3 Q6/Q7.
 * - **검증 게이트 (a) AfterActiveDate 중복**: 레거시 loop 미실행 버그 + 6개월 cycle 등록 빈도 매우 낮음 → 신규 미도입 (운영자 신뢰). 결정 근거: 스펙 §3 Q2 = 옵션 3.
 * - **활성 단일성 강제**: 레거시 부재 + 신규도 미도입 (`AgreementWordRepository.findFirstByActiveTrueAndIsDeletedFalse` 가 다건 시 첫 1건 채택). 결정 근거: 스펙 §3 Q5 = #654 Q5 정합.
 * - **Heroku Connect 동기**: 자연 소멸 (Tech stack 차이) — 신규는 PostgreSQL 직접 적재.
 * - **활성 토글 책임**: cycle batch (#654) 단독. 본 Service 는 신규 등록만 담당 — `active=false`, `activeDate=null` 적재로 책임 경계 정합.
 */
@Service
class AdminAgreementWordService(
    private val agreementWordRepository: AgreementWordRepository
) {

    /**
     * 신규 약관 등록. DTO `@AssertFalse`/`@Null` 검증 통과 후에도 Service 단에서 active/activeDate
     * fallback 강제 적재 — DTO 우회 호출 (단위 테스트 / 내부 호출) 안전망.
     */
    @Transactional
    fun createAgreementWord(request: AdminAgreementWordCreateRequest): AdminAgreementWordCreateResponse {
        val entity = AgreementWord(
            name = request.name,
            contents = request.contents,
            afterActiveDate = request.afterActiveDate,
            active = false,
            activeDate = null,
            isDeleted = false
        )
        val saved = agreementWordRepository.save(entity)
        return AdminAgreementWordCreateResponse.from(saved)
    }

    /**
     * 활성 약관 조회 (Web 미리보기 카드용). 다건 활성 시 첫 1건 채택 — Active 단일성 강제 부재 (#654 Q5 정합).
     */
    @Transactional(readOnly = true)
    fun getActiveAgreementWord(): AdminAgreementWordActiveResponse? {
        return agreementWordRepository.findFirstByActiveTrueAndIsDeletedFalse()
            .map { AdminAgreementWordActiveResponse.from(it) }
            .orElse(null)
    }
}
