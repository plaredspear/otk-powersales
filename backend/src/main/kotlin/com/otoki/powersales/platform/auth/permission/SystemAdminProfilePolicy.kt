package com.otoki.powersales.platform.auth.permission

import com.otoki.powersales.platform.auth.entity.AppAuthority

/**
 * 12종 Profile 의 SoT + AppAuthority picklist → Profile.name 매핑.
 *
 * Profile.name (한글) 이 권한 모델 입력 SoT — ROLE_ 산출 / EmployeeProfileResolver / UserProvisioningService 가 본 매핑을 공유.
 * AppAuthority picklist 부재 (영업부장 / 사업부장 / 본부장 / Staff 등) 직위는 [profileNameForRole] 의 null fallback —
 * 운영 환경에서는 EmployeeProfileResolver.resolveProfileId 의 Org + jikchak 분기로 정확한 Profile 산출.
 *
 * Profile row 자체의 SoT 는 **SF Profile 마이그레이션** (Stage1 Profile 적재) — 본 object 는 backend 코드의
 * 한글 상수 정렬을 위한 lookup key 만 보유한다. dev/prod 환경에서는 SF 의 운영 Profile row 가 sfid 와 함께
 * profile 테이블에 적재되어 있어야 하며 (Stage1 + post-copy hook 의 SF Admin → '시스템 관리자' rename 포함),
 * local 환경에서는 [com.otoki.powersales.common.config.LocalDataInitializer] 가 부족분 Profile row 를 자체 시드한다.
 */
object SystemAdminProfilePolicy {

    const val SYSTEM_ADMIN_PROFILE_NAME = "시스템 관리자"

    /**
     * 시스템 관리자 여부 — Profile.name 기반 단일 판정.
     *
     * 코드베이스 곳곳의 `profileName == "시스템 관리자"` 문자열 비교 (일부는 로컬 const 재정의) 가
     * 본 메서드로 수렴 대상. null 이면 false (비 시스템 관리자).
     */
    fun isSystemAdmin(profileName: String?): Boolean = profileName == SYSTEM_ADMIN_PROFILE_NAME

    /**
     * 12종 Profile 의 SoT.
     *
     * 한글 표기는 SF retrieve 메타 (`force-app/main/default/profiles/<name>.profile-meta.xml`) 의 실측을 따른다.
     * 예: '8. 마케팅' (점 다음 공백), '9. Staff' (점 다음 공백) — SF 운영 Profile.Name 그대로.
     * '시스템 관리자' 는 SF 부재 (SF 표준 'System Administrator' / 'Admin') — Stage1 Profile 적재 후
     * [com.otoki.powersales._migration.sf.stage1.Stage1S3CopyService] 의 post-copy hook 이 SF Admin row 의
     * name 만 alias 로 rename.
     */
    val REQUIRED_PROFILE_NAMES: List<String> = listOf(
        SYSTEM_ADMIN_PROFILE_NAME,
        "8. 마케팅",
        "9. Staff",
        "6.조장",
        "4.지점장",
        "3.영업부장",
        "2.사업부장",
        "1.본부장",
        "5.영업사원",
        "7.영업사원 + 조장",
        "공장관계자",
        "OLS",
    )

    /**
     * SF AppAuthority picklist value → Profile.name 매핑 — Provisioning / Seed 시점 profileId 결정.
     *
     * SF picklist 4종 (조장 / 여사원 / 지점장 / AccountViewAll) 만 입력 가능.
     * null 또는 매칭 부재 시 5.영업사원 fallback.
     */
    fun profileNameForRole(role: String?): String = when (role) {
        AppAuthority.LEADER -> "6.조장"
        AppAuthority.BRANCH_MANAGER -> "4.지점장"
        AppAuthority.WOMAN -> "5.영업사원"
        AppAuthority.ACCOUNT_VIEW_ALL -> "5.영업사원"
        else -> "5.영업사원"
    }
}
