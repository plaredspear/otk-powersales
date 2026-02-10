package com.otoki.internal.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.time.Clock

/**
 * 애플리케이션 공통 설정
 */
@Configuration
class AppConfig {

    /**
     * 시스템 Clock Bean
     * 테스트에서 시간을 제어할 수 있도록 Bean으로 등록
     */
    @Bean
    fun clock(): Clock = Clock.systemDefaultZone()
}
