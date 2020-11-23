package no.nav.helse.flex.endpoints

import org.springframework.http.HttpMethod
import org.springframework.stereotype.Component
import org.springframework.util.AntPathMatcher

@Component
class EndpointMatcher(private val endpoints: Endpoints) {

    private val all: List<String> = ArrayList<String>().also{
        it.addAll(endpoints.get)
        it.addAll(endpoints.delete)
        it.addAll(endpoints.post)
        it.addAll(endpoints.put)
    }

    fun matches(method: HttpMethod?, path: String): Boolean {
        val antPathMatcher = AntPathMatcher()

        val endpoints = when (method) {
            HttpMethod.GET -> endpoints.get
            HttpMethod.PUT -> endpoints.put
            HttpMethod.POST -> endpoints.post
            HttpMethod.DELETE -> endpoints.delete
            else -> return false
        }
        return endpoints.any { antPathMatcher.match(it, path) }
    }

    fun matches(path: String): Boolean {
        val antPathMatcher = AntPathMatcher()

        return all.any { antPathMatcher.match(it, path) }
    }
}
