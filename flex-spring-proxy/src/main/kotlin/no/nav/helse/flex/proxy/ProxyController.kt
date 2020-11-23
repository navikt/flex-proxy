package no.nav.helse.flex.proxy

import no.nav.helse.flex.endpoints.EndpointMatcher
import no.nav.helse.flex.log
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.RequestEntity
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.client.HttpStatusCodeException
import org.springframework.web.util.WebUtils
import java.net.URI
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse


@RestController
class ProxyController(
        private val proxyService: ProxyService,
        private val endpointMatcher: EndpointMatcher,
        @Value("\${service.gateway.url}") private val url: String,
        @Value("\${service.gateway.key}") private val serviceGatewayKey: String,
        @Value("\${auth.cookie.name}") private val cookieName: String) {

    val remoteService: URI = URI.create(url)
    val basePath = if (remoteService.path == "/") {
        ""
    } else {
        remoteService.path
    }
    val logger = log()
    val hasServiceGatewayKey = serviceGatewayKey != ""

    @RequestMapping("/**")
    fun proxy(requestEntity: RequestEntity<Any>, @RequestParam params: HashMap<String, String>, request: HttpServletRequest): ResponseEntity<Any> {

        val matches = endpointMatcher.matches(requestEntity.method, requestEntity.url.path)
        if (!matches) {
            logger.warn("Ingen mapping funnet for ${requestEntity.method} -  ${requestEntity.url.path} ")
            return ResponseEntity(HttpStatus.NOT_FOUND.reasonPhrase, HttpStatus.NOT_FOUND)
        }
        val uri = requestEntity.url.run {
            URI(remoteService.scheme, userInfo, remoteService.host, remoteService.port, basePath +path, query, fragment)
        }
        val cookie = WebUtils.getCookie(request, cookieName)


        val headers = requestEntity.headers.toSingleValueMap()

        val nyeHeaders = HttpHeaders()
        headers.forEach {
            nyeHeaders.set(it.key, it.value)
        }
        if (hasServiceGatewayKey) {
            nyeHeaders.set("x-nav-apiKey", serviceGatewayKey)
        }

        if (cookie != null && nyeHeaders["Authorization"] == null) {
            nyeHeaders["Authorization"] = "Bearer ${cookie.value}"
        }

        val forward = RequestEntity(
                requestEntity.body, nyeHeaders,
                requestEntity.method, uri
        )

        return proxyService.call(forward)
    }

    @ExceptionHandler(HttpStatusCodeException::class)
    fun handleErrorRequests(response: HttpServletResponse, e: HttpStatusCodeException) {
        response.status = e.rawStatusCode
        if (e.responseHeaders != null) {
            val contentType = e.responseHeaders!!.contentType
            if (contentType != null) {
                response.contentType = contentType.toString()
            }
        }
        response.outputStream.write(e.responseBodyAsByteArray)
        if (listOf(HttpStatus.GATEWAY_TIMEOUT.value(), HttpStatus.BAD_GATEWAY.value(), HttpStatus.SERVICE_UNAVAILABLE.value()).contains(response.status)) {
            logger.error("Feil ved proxying til backend ${response.status}", e)
        }
    }
}
