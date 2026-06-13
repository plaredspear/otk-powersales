package com.otoki.powersales.platform.apppackage.repository

import com.otoki.powersales.platform.apppackage.entity.AppPlatform
import com.otoki.powersales.platform.apppackage.entity.AppPackage
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface AppPackageRepository : JpaRepository<AppPackage, Long> {

    /** 플랫폼별 최신 지정 버전. */
    fun findByPlatformAndIsLatestTrue(platform: AppPlatform): AppPackage?

    /** 최신 지정이 없을 때 fallback — version_code 최대값. */
    fun findTopByPlatformOrderByVersionCodeDesc(platform: AppPlatform): AppPackage?

    fun findByPlatform(platform: AppPlatform, pageable: Pageable): Page<AppPackage>

    fun existsByPlatformAndVersionCode(platform: AppPlatform, versionCode: Long): Boolean

    /**
     * 플랫폼별 force_update=true 버전들의 최대 version_code (없으면 null). 강제 업데이트 판정용.
     *
     * `요청 versionCode < maxForceUpdateVersionCode` 이면 요청 버전 초과 ~ 최신 사이에 강제 버전이
     * 존재한다는 뜻 (중간 강제 버전을 건너뛴 우회를 막는다). 단일 스칼라라 캐시 친화적 — 요청마다
     * versionCode 가 달라도 platform 단위 캐시 hit 으로 강제 판정이 가능하다.
     */
    @Query("select max(a.versionCode) from AppPackage a where a.platform = :platform and a.forceUpdate = true")
    fun findMaxForceUpdateVersionCode(@Param("platform") platform: AppPlatform): Long?

    /** 동일 platform 의 기존 최신 지정을 해제 (setLatest 2-step 1단계). */
    @Modifying
    @Query("update AppPackage a set a.isLatest = false where a.platform = :platform and a.isLatest = true")
    fun clearLatest(@Param("platform") platform: AppPlatform): Int
}
