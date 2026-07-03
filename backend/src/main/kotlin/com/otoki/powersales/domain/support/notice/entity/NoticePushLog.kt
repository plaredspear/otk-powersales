package com.otoki.powersales.domain.support.notice.entity

import com.otoki.powersales.domain.org.employee.entity.Employee
import com.otoki.powersales.platform.common.entity.BaseEntity
import com.otoki.powersales.platform.common.entity.DomainName
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table

/**
 * 공지사항 FCM push 발송 이력.
 *
 * web 관리자가 공지 상세 화면의 '푸시 발송' 버튼으로 즉시 발송할 때마다 1행 기록한다.
 * 발송 결과(성공/실패 건수) 확인 + 중복 발송 경고(마지막 발송 시각/횟수)의 근거로 사용한다.
 * SF 메타에 대응 SObject 없는 신규 로컬 테이블.
 */
@DomainName("공지사항푸시발송이력")
@Entity
@Table(name = "notice_push_log")
class NoticePushLog(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "notice_push_log_id")
    val id: Long = 0,

    @Column(name = "notice_id", nullable = false)
    val noticeId: Long = 0,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sent_by_id")
    val sentBy: Employee? = null,

    /** 발송 대상 공개범위(공지 scope displayName). 이력 표시용. */
    @Column(name = "target_scope", length = 255)
    val targetScope: String? = null,

    /** 발송 대상 토큰 수. */
    @Column(name = "target_count", nullable = false)
    val targetCount: Int = 0,

    @Column(name = "success_count", nullable = false)
    val successCount: Int = 0,

    @Column(name = "failure_count", nullable = false)
    val failureCount: Int = 0,
) : BaseEntity()
