package no.nav.helse.flex.proxy


import no.nav.helse.flex.TestApplication
import org.hamcrest.Matchers.equalTo
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.context.junit4.SpringRunner
import org.springframework.test.web.client.ExpectedCount
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.match.MockRestRequestMatchers.*
import org.springframework.test.web.client.response.MockRestResponseCreators.withStatus
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.util.LinkedMultiValueMap
import org.springframework.util.MultiValueMap
import org.springframework.web.client.RestTemplate
import java.net.URI
import javax.servlet.http.Cookie


@RunWith(SpringRunner::class)
@AutoConfigureMockMvc
@SpringBootTest(classes = [TestApplication::class])
class ProxyControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var restTemplate: RestTemplate

    private lateinit var mockServer: MockRestServiceServer

    @Before
    fun init() {
        mockServer = MockRestServiceServer.createServer(restTemplate)
    }

    @Test
    fun `Returnerer 404 for api som ikke er eksponert`() {
        mockMvc.perform(MockMvcRequestBuilders.get("/api/erIkkeTilgejngelig")
                .contentType(MediaType.APPLICATION_JSON)
        ).andExpect(MockMvcResultMatchers.status().isNotFound)
                .andExpect(content().string(equalTo("Not Found")))
                .andReturn()
    }

    @Test
    fun `Proxyer GET request til service som returnerer ok`() {

        mockServer.expect(ExpectedCount.once(),
                requestTo(URI("http://service/api/v1/tester/1234?yo=hei")))
                .andExpect(method(HttpMethod.GET))
                .andExpect(headerDoesNotExist("Authorization"))
                .andExpect(header("x-nav-apiKey", "nøkkæl"))
                .andRespond(withStatus(HttpStatus.OK)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"hei\": 123}"))


        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/tester/1234?yo=hei")
                .header(HttpHeaders.ORIGIN, "https://www-gcp.dev.nav.no")
                .contentType(MediaType.APPLICATION_JSON)
        ).andExpect(MockMvcResultMatchers.status().isOk)
                .andExpect(content().string(equalTo("{\"hei\":123}")))
                .andExpect(MockMvcResultMatchers.header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "https://www-gcp.dev.nav.no"))
                .andExpect(MockMvcResultMatchers.header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true"))
                .andExpect(MockMvcResultMatchers.header().string(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON.toString()))
                .andReturn()

        mockServer.verify()
    }

    @Test
    fun `Returnerer ikke andre headere videre fra backend enn contenttype`() {
        mockServer.expect(ExpectedCount.once(),
                requestTo(URI("http://service/api/v1/tester/1234?yo=hei")))
                .andExpect(method(HttpMethod.GET))
                .andExpect(headerDoesNotExist("Authorization"))
                .andExpect(header("x-nav-apiKey", "nøkkæl"))
                .andRespond(withStatus(HttpStatus.OK)
                        .contentType(MediaType.APPLICATION_JSON)
                        .headers(HttpHeaders.writableHttpHeaders(HttpHeaders()
                                .also { it.set("X-Global-Transaction-ID", "15452e785fbbd0972229b1df") }))
                        .body("{\"hei\": 123}"))


        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/tester/1234?yo=hei")
                .header(HttpHeaders.ORIGIN, "https://www-gcp.dev.nav.no")
                .contentType(MediaType.APPLICATION_JSON)
        ).andExpect(MockMvcResultMatchers.status().isOk)
                .andExpect(content().string(equalTo("{\"hei\":123}")))
                .andExpect(MockMvcResultMatchers.header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "https://www-gcp.dev.nav.no"))
                .andExpect(MockMvcResultMatchers.header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true"))
                .andExpect(MockMvcResultMatchers.header().string(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON.toString()))
                .andExpect(MockMvcResultMatchers.header().doesNotExist("X-Global-Transaction-ID"))
                .andReturn()

        mockServer.verify()
    }


    @Test
    fun `Proxyer GET request til service som returnerer 401`() {

        mockServer.expect(ExpectedCount.once(),
                requestTo(URI("http://service/api/v1/tester/1234?yo=hei")))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED))


        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/tester/1234?yo=hei")
                .contentType(MediaType.APPLICATION_JSON)
        ).andExpect(MockMvcResultMatchers.status().isUnauthorized)
                .andExpect(content().bytes(byteArrayOf()))
                .andReturn()

        mockServer.verify()
    }

    @Test
    fun `Proxyer GET request til service som returnerer 500`() {

        mockServer.expect(ExpectedCount.once(),
                requestTo(URI("http://service/api/v1/tester/1234?yo=hei")))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"error\": 123}"))


        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/tester/1234?yo=hei")
                .contentType(MediaType.APPLICATION_JSON)
        ).andExpect(MockMvcResultMatchers.status().isInternalServerError)
                .andExpect(content().string(equalTo("{\"error\": 123}")))
                .andReturn()

        mockServer.verify()
    }

    @Test
    fun `Proxyer POST som flytter over idtoken til auth header`() {

        mockServer.expect(ExpectedCount.once(),
                requestTo(URI("http://service/api/v1/tester/1234/les?yo=hei")))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer ey-hey-ho"))
                .andRespond(withStatus(HttpStatus.OK)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"hei\": 123}"))


        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/tester/1234/les?yo=hei")
                .cookie(Cookie("selvbetjening-idtoken", "ey-hey-ho"))
                .contentType(MediaType.APPLICATION_JSON)
        ).andExpect(MockMvcResultMatchers.status().isOk)
                .andExpect(content().string(equalTo("{\"hei\":123}")))
                .andReturn()

        mockServer.verify()
    }

    @Test
    fun `Setter ikke auth header hvis authheader allerede finnes`() {

        mockServer.expect(ExpectedCount.once(),
                requestTo(URI("http://service/api/v1/tester/1234/les?yo=hei")))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "secret123"))
                .andRespond(withStatus(HttpStatus.OK)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"hei\": 123}"))


        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/tester/1234/les?yo=hei")
                .header("Authorization", "secret123")
                .cookie(Cookie("selvbetjening-idtoken", "ey-hey-ho"))
                .contentType(MediaType.APPLICATION_JSON)
        ).andExpect(MockMvcResultMatchers.status().isOk)
                .andExpect(content().string(equalTo("{\"hei\":123}")))
                .andReturn()

        mockServer.verify()
    }

    @Test
    fun `Options returnerer riktig ved treff `() {

        mockMvc.perform(MockMvcRequestBuilders.options("/api/v1/tester/1234?yo=hei")
                .header(HttpHeaders.ORIGIN, "https://www-gcp.dev.nav.no")

                .contentType(MediaType.APPLICATION_JSON)
        ).andExpect(MockMvcResultMatchers.status().isOk)
                .andExpect(MockMvcResultMatchers.header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "https://www-gcp.dev.nav.no"))
                .andExpect(MockMvcResultMatchers.header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true"))
                .andExpect(MockMvcResultMatchers.header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS, "POST, GET, OPTIONS, DELETE, PUT"))
                .andExpect(MockMvcResultMatchers.header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS, "Content-Type, nav_csrf_protection, x-app-started-timestamp"))
                .andReturn()


    }

    @Test
    fun `Options returnerer 404 ved feil`() {

        mockMvc.perform(MockMvcRequestBuilders.options("/ukjentApi")
                .contentType(MediaType.APPLICATION_JSON)
        ).andExpect(MockMvcResultMatchers.status().isNotFound)
                .andReturn()

    }
}
