package no.nav.helse.flex.cors

import no.nav.helse.flex.endpoints.EndpointMatcher
import no.nav.helse.flex.log
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.stereotype.Component
import org.springframework.web.servlet.HandlerInterceptor
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

@Component
class CorsInterceptor(
        @Value("\${allowed.origins}") private val allowedOrigins: String,
        private val endpointMatcher: EndpointMatcher
) : HandlerInterceptor {

    val logger = log()

    private val allowedOriginsList: List<String> = allowedOrigins.split(",");

    override fun preHandle(request: HttpServletRequest, response: HttpServletResponse, handler: Any): Boolean {
        val origin = request.getHeader(HttpHeaders.ORIGIN)
        response.setHeader(HttpHeaders.CACHE_CONTROL, "no-cache, no-store, must-revalidate")
        if (allowedOriginsList.contains(origin)) {
            response.setHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, origin)
            response.setHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true")
        }
        if (request.method == HttpMethod.OPTIONS.name) {
            if (endpointMatcher.matches(request.requestURI)) {
                response.setHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS, "POST, GET, OPTIONS, DELETE, PUT")
                response.setHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS, "Content-Type, nav_csrf_protection, x-app-started-timestamp")
            } else{
                logger.warn("Ingen mapping funnet for ${request.method} -  ${request.requestURI}")
                response.status = 404
            }
        }
        return true
    }

}
