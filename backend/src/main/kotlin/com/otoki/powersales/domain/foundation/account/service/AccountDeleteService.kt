package com.otoki.powersales.domain.foundation.account.service

import com.otoki.powersales.domain.foundation.account.exception.AccountDeleteBlockedSapSyncedException
import com.otoki.powersales.domain.foundation.account.exception.AccountDeleteNotOwnerException
import com.otoki.powersales.domain.foundation.account.exception.AccountNotFoundException
import com.otoki.powersales.domain.foundation.account.repository.AccountRepository
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
 * - **Owner 가드 (SF 동등 복원)**: 레거시 `OwnerId != UserInfo.getUserId()` 가드를 신규에 그대로 인계 — `account.ownerUser.id != requesterUserId` 이면 차단(`AccountDeleteNotOwnerException`, 403). SF 삭제 trigger 는 Profile 예외가 없어 시스템 관리자도 동일 적용되므로, 신규도 권한 키와 무관하게 owner 본인만 삭제 가능. (과거 스펙 §3 Q4 의 "자연 소멸" 결정은 SF 동등성 기준으로 재검토되어 복원됨.)
 * - **권한**: SF Profile/PermissionSet → `AdminPermission.ACCOUNT_DELETE` (신규 enum). `ACCOUNT_WRITE` (등록) 와 별도 키로 분리 운영. 결정 근거: 스펙 §3 Q6.
 * - **404 멱등 응답**: row 부재 또는 이미 `is_deleted=true` 인 경우 404 통일 (멱등 보장 — 동일 요청 반복 시 첫 호출만 200, 이후는 404). 레거시는 row 부재 시 SF 표준 응답.
 * - **undelete (복원) 미인계**: 레거시도 `after undelete` trigger 미정의 (가드 재검증 부재). 다른 도메인 admin 도 복원 화면 미운영. 결정 근거: 스펙 §3 Q5.
 */
@Service
class AccountDeleteService(
    private val accountRepository: AccountRepository
) {

    /**
     * 거래처 soft-delete. 레거시 SF `AccountDeleteCheck` 의 두 가드를 순서대로 적용한다:
     * 1. ExternalKey 가드 — SAP 동기 거래처(`external_key IS NOT NULL`) 차단.
     * 2. Owner 가드 — `account.ownerUser.id != requesterUserId` 이면 차단 (SF `OwnerId != UserInfo.getUserId()` 동등).
     *    SF 삭제 trigger 는 Profile 예외가 없어 시스템 관리자도 동일 적용 → 권한 키와 무관하게 owner 본인만 삭제 가능.
     *    owner 가 없는(NULL) 거래처는 어떤 요청자와도 일치하지 않으므로 삭제 불가 (SF 에서 OwnerId 는 항상 존재하므로
     *    NULL 은 신규 데이터 정합 이슈 — 삭제 차단이 안전).
     *
     * @param requesterUserId 삭제 요청자(로그인 사용자)의 User.id — `account.ownerUser.id` 와 비교.
     */
    @Transactional
    fun delete(id: Long, requesterUserId: Long) {
        val account = accountRepository.findActiveById(id)
            ?: throw AccountNotFoundException()

        if (account.externalKey != null) {
            throw AccountDeleteBlockedSapSyncedException()
        }

        if (account.ownerUser?.id != requesterUserId) {
            throw AccountDeleteNotOwnerException()
        }

        account.isDeleted = true
    }
}
