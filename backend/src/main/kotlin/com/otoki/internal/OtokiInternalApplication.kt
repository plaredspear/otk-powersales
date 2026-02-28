package com.otoki.internal

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.data.redis.repository.configuration.EnableRedisRepositories

@SpringBootApplication
@EnableRedisRepositories(basePackages = [])
class OtokiInternalApplication

fun main(args: Array<String>) {
	runApplication<OtokiInternalApplication>(*args)
}
