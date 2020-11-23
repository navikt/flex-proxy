package no.nav.helse.flex.config


import no.nav.helse.flex.cors.CorsInterceptor
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.client.RestTemplate
import org.springframework.web.servlet.config.annotation.InterceptorRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer



@Configuration
class ApplicationConfig(private val corsInterceptor: CorsInterceptor) : WebMvcConfigurer {

    @Bean
    fun restTemplate(): RestTemplate {
        return RestTemplate()
    }

    override fun addInterceptors(registry: InterceptorRegistry) {
        registry.addInterceptor(corsInterceptor)
    }
}
