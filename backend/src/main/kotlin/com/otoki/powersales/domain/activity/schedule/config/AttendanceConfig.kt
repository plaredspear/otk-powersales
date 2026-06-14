package com.otoki.powersales.domain.activity.schedule.config

import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Configuration

/**
 * 출근 등록 도메인 설정 활성화 (Spec #585).
 */
@Configuration
@EnableConfigurationProperties(AttendanceProperties::class)
class AttendanceConfig
