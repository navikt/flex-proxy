package no.nav.helse.flex.proxy

import org.springframework.http.RequestEntity
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate
import org.springframework.web.client.exchange


@Component
class ProxyService(
        private val restTemplate: RestTemplate) {


    fun call(requestEntity: RequestEntity<Any>): ResponseEntity<Any> {

        return restTemplate.exchange(requestEntity)
    }
}
