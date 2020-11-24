package no.nav.helse.flex.proxy

import no.nav.helse.flex.endpoints.EndpointMatcher
import no.nav.helse.flex.log
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.*
import org.springframework.util.LinkedMultiValueMap
import org.springframework.util.MultiValueMap
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.client.HttpStatusCodeException
import org.springframework.web.util.WebUtils
import java.net.URI
import java.util.*
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse
import kotlin.collections.HashMap


@RestController
class ProxyController(
        private val proxyService: ProxyService,
        private val endpointMatcher: EndpointMatcher,
        @Value("\${service.gateway.url}") private val url: String,
        @Value("\${service.gateway.key}") private val serviceGatewayKey: String,
        @Value("\${auth.cookie.name}") private val cookieName: String) {

    private final val remoteService: URI = URI.create(url)
    private val basePath: String = if (remoteService.path == "/") {
        ""
    } else {
        remoteService.path
    }
    private val logger = log()
    private val hasServiceGatewayKey = serviceGatewayKey != ""

    @RequestMapping("/**")
    fun proxy(requestEntity: RequestEntity<Any>, @RequestParam params: HashMap<String, String>, request: HttpServletRequest): ResponseEntity<Any> {

        val matches = endpointMatcher.matches(requestEntity.method, requestEntity.url.path)
        if (!matches) {
            logger.warn("Ingen mapping funnet for ${requestEntity.method} -  ${requestEntity.url.path} ")
            return ResponseEntity(HttpStatus.NOT_FOUND.reasonPhrase, HttpStatus.NOT_FOUND)
        }
        val uri = requestEntity.url.run {
            URI(remoteService.scheme, userInfo, remoteService.host, remoteService.port, basePath + path, query, fragment)
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

        val responseEntity = proxyService.call(forward)

        val newHeaders: MultiValueMap<String, String> = LinkedMultiValueMap()
        responseEntity.headers.contentType?.let {
            newHeaders.set("Content-type", it.toString());
        }

        return ResponseEntity(responseEntity.body, newHeaders, responseEntity.statusCode);
    }

    @ExceptionHandler(HttpStatusCodeException::class)
    fun handleHttpStatusCodeException(response: HttpServletResponse, e: HttpStatusCodeException) {
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

    @ExceptionHandler(Exception::class)
    fun handleException(response: HttpServletResponse, e: Exception) {
        logger.error("Feil ved proxying til backend", e)
        response.status = 500
        response.contentType = MediaType.TEXT_PLAIN_VALUE
        response.outputStream.write(HttpStatus.INTERNAL_SERVER_ERROR.reasonPhrase.toByteArray())
    }
}
