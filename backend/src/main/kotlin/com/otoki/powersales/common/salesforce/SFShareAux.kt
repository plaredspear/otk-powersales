package com.otoki.powersales.common.salesforce

/**
 * SF Sharing Auxiliary — SF 권한/sharing 구현을 위해 신규 시스템에 추가된 application-native entity 마커.
 *
 * SF sObject 1:1 매핑 (`@SFObject`) 도 SF 메타 파생도 아닌 신규 시스템 자체 생성물 중,
 * SF sharing rule / role hierarchy 평가 인프라를 보조하기 위한 entity 에 부착.
 *
 * 부착 예: sharing recalc audit, UserRole 트리 정적 스냅샷.
 *
 * 비부착: admin permission matrix (RolePermission/UserPermission — 별도 차원), SAP/SF audit, outbox 등 SF sharing 평가와 무관한 entity.
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class SFShareAux
