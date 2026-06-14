package com.otoki.powersales.platform.common.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.data.redis.connection.RedisConnectionFactory
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory

// 로컬 프로파일(외부 의존 없는 기동) 에서 JwtTokenProvider 등이 요구하는 RedisTemplate 의존성을 충족하기 위한 stub.
// application-local.yml 에서 Redis auto-config 를 제외했기에 RedisConnectionFactory 만 직접 등록하고,
// RedisTemplate 는 기존 RedisConfig (@ConditionalOnBean(RedisConnectionFactory)) 가 처리하도록 위임한다.
// LettuceConnectionFactory 는 실제 연결을 lazy 로 시도하므로 기동 자체는 차단되지 않는다.
@Configuration
@Profile("local")
class LocalRedisStubConfig {

    @Bean
    fun redisConnectionFactory(): RedisConnectionFactory = LettuceConnectionFactory()
}
