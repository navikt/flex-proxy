package no.nav.syfo.spinnsyn

import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpHeaders
import org.springframework.http.RequestEntity
import org.springframework.http.ResponseEntity
import org.springframework.util.CollectionUtils
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.RestTemplate
import org.springframework.web.client.exchange
import org.springframework.web.util.WebUtils
import java.net.URI
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse


@RestController
class SpinnsynVeilederControllerController(
        private val restTemplate: RestTemplate,
        @Value("\${service.gateway.url}") private val url: String,
        @Value("\${auth.cookie.name}") private val cookiename: String) {

    val remoteService: URI = URI.create(url)


    @GetMapping("/api/v1/veileder/vedtak")
    fun proxy(requestEntity: RequestEntity<Any>, @RequestParam params: HashMap<String, String>, request: HttpServletRequest): ResponseEntity<Any> {
        val uri = requestEntity.url.run {
            URI(remoteService.scheme, userInfo, remoteService.host, remoteService.port, path, query, fragment)
        }
        val cookie = WebUtils.getCookie(request, cookiename)


        val headers = requestEntity.headers.toSingleValueMap()

        val nyeHeaders = HttpHeaders()
        headers.forEach{
            nyeHeaders.set(it.key, it.value)
        }

        if (cookie != null) {
            nyeHeaders["Authorization"] = "Bearer $cookie"
        }

        val forward = RequestEntity(
                requestEntity.body, nyeHeaders,
                requestEntity.method, uri
        )

        return restTemplate.exchange(forward)
    }

    @ExceptionHandler(HttpClientErrorException::class)
    fun handleErrorRequests(response: HttpServletResponse, e: HttpClientErrorException) {
        response.status = e.rawStatusCode
        if (e.responseHeaders != null) {
            val contentType = e.responseHeaders!!.contentType
            if (contentType != null) {
                response.contentType = contentType.toString()
            }
        }
        response.outputStream.write(e.responseBodyAsByteArray)
    }
}
