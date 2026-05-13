package com.otoki.powersales.account.service

import com.otoki.powersales.account.exception.AccountDeleteBlockedSapSyncedException
import com.otoki.powersales.account.exception.AccountNotFoundException
import com.otoki.powersales.account.repository.AccountRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 관리자 웹 거래처 삭제 도메인 서비스. (Spec #642 P1-B)
 *
 * ## 레거시 매핑
 * - SF Apex: `AccountTrigger.trigger:1-3` (beforeDelete) → `AccountTriggerHandler.cls:35-37` (`AccountDeleteCheck` 위임) → `AccountTriggerHandler.cls:157-162` (가드 본체)
 * - flow-legacy: flow-legacy yaml (Spec #642) (5 step)
 * - origin spec: #642
 *
 * ## 레거시 동작 요약
 * 1. 입력: 영업사원이 SF Lightning Account 페이지의 표준 '삭제' 버튼 클릭 → SF 표준 delete API 호출 (단건).
 * 2. AccountTrigger before delete → `AccountDeleteCheck()` 호출 (Trigger.old 의 각 Account 순회).
 * 3. ExternalKey 가드 — `ExternalKey__c != null` 이면 `addError('ExternalKey__c', '거래처 코드가 있는 거래처는 삭제가 불가능합니다.')` → DML 실패. SAP 동기 거래처 영구 차단.
 * 4. Owner 가드 — `OwnerId != UserInfo.getUserId()` 이면 `addError('OwnerId', '자신의 신규 거래처만 삭제가 가능합니다.')` → DML 실패. 본인 신규 거래처 한정.
 * 5. 검증 통과 시 SF 표준 delete → 휴지통(15일) 보관 → 자동 영구 삭제 (또는 명시적 `emptyRecycleBin`). `after delete` / `after undelete` 미정의 (휴지통 복원 시 가드 재검증 부재 — 레거시 알려진 위험).
 *
 * ## 신규 차이
 * - **진입점**: 영업사원 native (SF Lightning) → 관리자 웹 (`DELETE /api/v1/admin/accounts/{id}`). 모바일 미운영. 결정 근거: 스펙 §3 Q1.
 * - **삭제 방식**: SF 휴지통 hard-delete (15일 후 영구) → application 차원 soft-delete (`is_deleted=true` UPDATE 단방향). `@SQLDelete` / `@Where` / `@SoftDelete` 미사용. 결정 근거: 스펙 §3 Q2.
 * - **ExternalKey 가드 인계**: 동일 정책 — `external_key IS NOT NULL` 이면 409 (`ACCOUNT_DELETE_BLOCKED_SAP_SYNCED`). SAP 인바운드 멱등성 보장. 결정 근거: 스펙 §3 Q3.
 * - **Owner 가드 자연 소멸**: 관리자 대행 모델 (#640) 에서 OwnerId 가드 의미 변형. 관리자 권한자는 native 거래처(`external_key IS NULL`) 모두 삭제 가능. 결정 근거: 스펙 §3 Q4.
 * - **권한**: SF Profile/PermissionSet → `AdminPermission.ACCOUNT_DELETE` (신규 enum). `ACCOUNT_WRITE` (등록) 와 별도 키로 분리 운영. 결정 근거: 스펙 §3 Q6.
 * - **404 멱등 응답**: row 부재 또는 이미 `is_deleted=true` 인 경우 404 통일 (멱등 보장 — 동일 요청 반복 시 첫 호출만 200, 이후는 404). 레거시는 row 부재 시 SF 표준 응답.
 * - **undelete (복원) 미인계**: 레거시도 `after undelete` trigger 미정의 (가드 재검증 부재). 다른 도메인 admin 도 복원 화면 미운영. 결정 근거: 스펙 §3 Q5.
 */
@Service
class AccountDeleteService(
    private val accountRepository: AccountRepository
) {

    @Transactional
    fun delete(id: Int) {
        val account = accountRepository.findActiveById(id)
            ?: throw AccountNotFoundException()

        if (account.externalKey != null) {
            throw AccountDeleteBlockedSapSyncedException()
        }

        account.isDeleted = true
    }
}
