package com.otoki.powersales.user.repository

import com.otoki.powersales.platform.auth.sharing.entity.QPermissionSetAssignment.Companion.permissionSetAssignment
import com.otoki.powersales.user.entity.QUser.Companion.user
import com.otoki.powersales.user.entity.User
import com.querydsl.core.BooleanBuilder
import com.querydsl.jpa.impl.JPAQueryFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.support.PageableExecutionUtils

class UserRepositoryCustomImpl(
    private val queryFactory: JPAQueryFactory
) : UserRepositoryCustom {

    override fun findUsers(keyword: String?, isActive: Boolean?, pageable: Pageable): Page<User> {
        val where = BooleanBuilder()
            .and(user.isDeleted.isNull.or(user.isDeleted.isFalse))

        if (!keyword.isNullOrBlank()) {
            where.and(
                user.username.containsIgnoreCase(keyword)
                    .or(user.employeeCode.containsIgnoreCase(keyword))
                    .or(user.name.containsIgnoreCase(keyword))
            )
        }
        if (isActive != null) {
            where.and(user.isActive.eq(isActive))
        }

        val content = queryFactory
            .selectFrom(user)
            .where(where)
            .orderBy(user.name.asc())
            .offset(pageable.offset)
            .limit(pageable.pageSize.toLong())
            .fetch()

        val countQuery = queryFactory
            .select(user.count())
            .from(user)
            .where(where)

        return PageableExecutionUtils.getPage(content, pageable) {
            countQuery.fetchOne() ?: 0L
        }
    }

    override fun findUsersByProfileId(profileId: Long, keyword: String?, pageable: Pageable): Page<User> {
        val where = BooleanBuilder()
            .and(user.profileId.eq(profileId))
            .and(user.isDeleted.isNull.or(user.isDeleted.isFalse))

        if (!keyword.isNullOrBlank()) {
            where.and(
                user.employeeCode.containsIgnoreCase(keyword)
                    .or(user.name.containsIgnoreCase(keyword))
            )
        }

        val content = queryFactory
            .selectFrom(user)
            .where(where)
            .orderBy(user.name.asc())
            .offset(pageable.offset)
            .limit(pageable.pageSize.toLong())
            .fetch()

        val countQuery = queryFactory
            .select(user.count())
            .from(user)
            .where(where)

        return PageableExecutionUtils.getPage(content, pageable) {
            countQuery.fetchOne() ?: 0L
        }
    }

    override fun findUsersByPermissionSetFlagsId(
        permissionSetFlagsId: Long,
        keyword: String?,
        pageable: Pageable,
    ): Page<User> {
        val where = BooleanBuilder()
            .and(permissionSetAssignment.permissionSetFlagsId.eq(permissionSetFlagsId))
            .and(permissionSetAssignment.isActive.isTrue)
            .and(permissionSetAssignment.assigneeUserId.eq(user.id))
            .and(user.isDeleted.isNull.or(user.isDeleted.isFalse))

        if (!keyword.isNullOrBlank()) {
            where.and(
                user.employeeCode.containsIgnoreCase(keyword)
                    .or(user.name.containsIgnoreCase(keyword))
            )
        }

        val content = queryFactory
            .select(user)
            .from(permissionSetAssignment, user)
            .where(where)
            .orderBy(user.name.asc())
            .offset(pageable.offset)
            .limit(pageable.pageSize.toLong())
            .fetch()

        val countQuery = queryFactory
            .select(user.count())
            .from(permissionSetAssignment, user)
            .where(where)

        return PageableExecutionUtils.getPage(content, pageable) {
            countQuery.fetchOne() ?: 0L
        }
    }
}
