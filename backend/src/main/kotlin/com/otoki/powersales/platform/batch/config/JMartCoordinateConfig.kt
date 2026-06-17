package com.otoki.powersales.platform.batch.config

import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Configuration

@Configuration
@EnableConfigurationProperties(JMartCoordinateProperties::class)
class JMartCoordinateConfig
