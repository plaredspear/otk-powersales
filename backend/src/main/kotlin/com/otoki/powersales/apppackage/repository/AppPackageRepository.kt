package com.otoki.powersales.apppackage.repository

import com.otoki.powersales.apppackage.entity.AppPackage
import com.otoki.powersales.apppackage.entity.AppPlatform
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
     * 요청 version_code 초과 ~ 최신 사이에 force_update=true 버전이 존재하는지 (강제 업데이트 판정).
     * 중간 강제 버전을 건너뛴 우회를 막는다.
     */
    fun existsByPlatformAndForceUpdateTrueAndVersionCodeGreaterThan(
        platform: AppPlatform,
        versionCode: Long,
    ): Boolean

    /** 동일 platform 의 기존 최신 지정을 해제 (setLatest 2-step 1단계). */
    @Modifying
    @Query("update AppPackage a set a.isLatest = false where a.platform = :platform and a.isLatest = true")
    fun clearLatest(@Param("platform") platform: AppPlatform): Int
}
