package com.otoki.internal

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class OtokiInternalApplication

fun main(args: Array<String>) {
	runApplication<OtokiInternalApplication>(*args)
}
