package no.nav.helse.flex.selftest

import no.nav.helse.flex.log
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.*
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.client.RestTemplate
import org.springframework.web.client.exchange
import org.springframework.web.client.getForEntity
import java.net.URI

const val APPLICATION_LIVENESS = "Application is alive!"
const val APPLICATION_READY = "Application is ready!"
const val APPLICATION_NOT_READY = "Application is NOT ready!"

@RestController
class SelfTestController(
        private val restTemplate: RestTemplate,
        @Value("\${service.gateway.url}") private val url: String,
        @Value("\${service.gateway.key}") private val serviceGatewayKey: String,
        @Value("\${service.liveness.path}") private val livenessPath: String
) {

    var ready = false
    private val logger = log()
    private val hasServiceGatewayKey = serviceGatewayKey != ""


    @GetMapping("/internal/isAlive", produces = [MediaType.TEXT_PLAIN_VALUE])
    fun isAlive(): ResponseEntity<String> {
        return ResponseEntity.ok(APPLICATION_LIVENESS)
    }

    @GetMapping("/internal/isReady", produces = [MediaType.TEXT_PLAIN_VALUE])
    fun isReady(): ResponseEntity<String> {
        if (ready) {
            return ResponseEntity.ok(APPLICATION_READY)
        }
        return try {

            val headers = HttpHeaders()

            if (hasServiceGatewayKey) {
                headers.set("x-nav-apiKey", serviceGatewayKey)
            }

            val request = RequestEntity<Any>(headers, HttpMethod.GET, URI(url + livenessPath))

            val res: ResponseEntity<String> = restTemplate.exchange(request)

            if (res.statusCode.is2xxSuccessful) {
                logger.info("I am ready")
                ready = true
                ResponseEntity.ok(APPLICATION_READY)
            } else {
                throw RuntimeException("Ikke klar")
            }

        } catch (e: Exception) {
            logger.info("Proxy er ikke klar", e)
            ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(APPLICATION_NOT_READY)
        }
    }
}
