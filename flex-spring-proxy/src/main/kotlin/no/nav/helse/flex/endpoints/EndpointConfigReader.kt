package no.nav.helse.flex.endpoints

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import no.nav.helse.flex.log
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Lazy
import org.springframework.context.annotation.Profile
import java.io.File
import java.net.URI


@Configuration
class EndpointConfigReader {
    val logger = log()

    @Bean
    fun endpoints(routesYamlUrl: URI): Endpoints {
        try {
            logger.info("Leser routes fra $routesYamlUrl")
            val mapper = ObjectMapper(YAMLFactory())

            fun String.fixPrefix(): String {
                if (this.startsWith("/")) {
                    return this
                }
                return "/$this"
            }

            val lesteEndepunkter = mapper.readValue(File(routesYamlUrl), Endpoints::class.java)

            val korrigert = Endpoints(
                    get = lesteEndepunkter.get.map { it.fixPrefix() },
                    put = lesteEndepunkter.put.map { it.fixPrefix() },
                    post = lesteEndepunkter.post.map { it.fixPrefix() },
                    delete = lesteEndepunkter.delete.map { it.fixPrefix() },
            )

            logger.info("Setter opp med endpoints $korrigert")
            return korrigert
        } catch (e: Exception){
            logger.error("OOOPS", e)
            return Endpoints()
        }
    }


    @Bean
    @Profile(value = ["default"])
    fun routesYamlUrl(): URI {
        return File("routes.yaml").toURI()
    }

}
