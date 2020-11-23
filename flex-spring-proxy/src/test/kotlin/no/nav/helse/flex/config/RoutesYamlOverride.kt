package no.nav.helse.flex.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.net.URI

@Configuration
class RoutesYamlOverride {

    @Bean
    fun routesYamlUrl(): URI? {
        return this.javaClass.getResource("/routes.yaml").toURI()
    }
}
