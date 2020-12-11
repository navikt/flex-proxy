package no.nav.helse.flex.proxy

import no.nav.helse.flex.log
import org.springframework.http.HttpStatus
import org.springframework.http.RequestEntity
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Component
import org.springframework.retry.annotation.Retryable
import org.springframework.web.client.*

@Component
class ProxyService(private val restTemplate: RestTemplate) {
    private val logger = log()

    @Retryable(include = [RetryableException::class])
    fun call(requestEntity: RequestEntity<Any>): ResponseEntity<Any> {
        try {
            return restTemplate.exchange(requestEntity)
        } catch (e: Exception) {
            if (e.skalRetryes()) {
                logger.warn("Retryer kall mot service", e)
                throw RetryableException(e)
            }
            throw e
        }
    }
}

private fun Exception.skalRetryes(): Boolean {
    if (this is HttpServerErrorException) {
        val status = this.rawStatusCode
        if (listOf(HttpStatus.GATEWAY_TIMEOUT.value(), HttpStatus.BAD_GATEWAY.value(), HttpStatus.SERVICE_UNAVAILABLE.value()).contains(status)) {
            return true
        }
    }
    if(this is ResourceAccessException){
        return true
    }
    return false
}

class RetryableException(reason: Throwable) : RuntimeException(reason)
