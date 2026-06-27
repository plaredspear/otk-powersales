package com.otoki.powersales.platform.apppackage.repository

import com.otoki.powersales.platform.apppackage.entity.AppPlatform

interface AppPackageRepositoryCustom {

    /**
     * 플랫폼별 force_update=true 버전들의 최대 version_code (없으면 null). 강제 업데이트 판정용.
     *
     * `요청 versionCode < maxForceUpdateVersionCode` 이면 요청 버전 초과 ~ 최신 사이에 강제 버전이
     * 존재한다는 뜻 (중간 강제 버전을 건너뛴 우회를 막는다).
     */
    fun findMaxForceUpdateVersionCode(platform: AppPlatform): Long?

    /** 동일 platform 의 기존 최신 지정을 해제 (setLatest 2-step 1단계). */
    fun clearLatest(platform: AppPlatform): Int
}
