package com.otoki.powersales.platform.apppackage.repository

import com.otoki.powersales.platform.apppackage.entity.AppPlatform
import com.otoki.powersales.platform.apppackage.entity.AppPackage
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository

interface AppPackageRepository : JpaRepository<AppPackage, Long>, AppPackageRepositoryCustom {

    /** 플랫폼별 최신 지정 버전. */
    fun findByPlatformAndIsLatestTrue(platform: AppPlatform): AppPackage?

    /** 최신 지정이 없을 때 fallback — version_code 최대값. */
    fun findTopByPlatformOrderByVersionCodeDesc(platform: AppPlatform): AppPackage?

    fun findByPlatform(platform: AppPlatform, pageable: Pageable): Page<AppPackage>

    fun existsByPlatformAndVersionCode(platform: AppPlatform, versionCode: Long): Boolean
}
