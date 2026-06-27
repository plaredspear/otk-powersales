package com.otoki.powersales.platform.apppackage.repository

import com.otoki.powersales.platform.apppackage.entity.AppPlatform
import com.otoki.powersales.platform.apppackage.entity.QAppPackage.Companion.appPackage
import com.querydsl.jpa.impl.JPAQueryFactory
import org.springframework.transaction.annotation.Transactional

open class AppPackageRepositoryCustomImpl(
    private val queryFactory: JPAQueryFactory
) : AppPackageRepositoryCustom {

    override fun findMaxForceUpdateVersionCode(platform: AppPlatform): Long? {
        return queryFactory
            .select(appPackage.versionCode.max())
            .from(appPackage)
            .where(
                appPackage.platform.eq(platform),
                appPackage.forceUpdate.isTrue,
            )
            .fetchOne()
    }

    @Transactional
    override fun clearLatest(platform: AppPlatform): Int {
        return queryFactory
            .update(appPackage)
            .set(appPackage.isLatest, false)
            .where(
                appPackage.platform.eq(platform),
                appPackage.isLatest.isTrue,
            )
            .execute()
            .toInt()
    }
}
