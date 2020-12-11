package no.nav.helse.flex.selftest


import no.nav.helse.flex.Application
import org.hamcrest.Matchers.equalTo
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
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
import org.springframework.web.client.RestTemplate
import java.net.URI


@RunWith(SpringRunner::class)
@AutoConfigureMockMvc
@SpringBootTest(classes = [Application::class])
class SelfTestControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var restTemplate: RestTemplate

    private lateinit var mockServer: MockRestServiceServer

    @Autowired
    private lateinit var selfTestController: SelfTestController

    @Before
    fun init() {
        mockServer = MockRestServiceServer.createServer(restTemplate)
    }

    @Test
    fun `Tester alive endepunkt`() {
        mockMvc.perform(MockMvcRequestBuilders.get("/internal/isAlive")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.status().is2xxSuccessful)
                .andExpect(content().string(equalTo("Application is alive!")))
                .andReturn()
    }

    @Test
    fun `Tester readiness ok`() {
        selfTestController.ready = false
        mockServer.expect(ExpectedCount.once(),
                requestTo(URI("http://service/is_alive")))
                .andRespond(withStatus(HttpStatus.OK)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"hei\": 123}"))


        mockMvc.perform(MockMvcRequestBuilders.get("/internal/isReady")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andExpect(content().string(equalTo("Application is ready!")))
                .andReturn()

        mockMvc.perform(MockMvcRequestBuilders.get("/internal/isReady")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andExpect(content().string(equalTo("Application is ready!")))
                .andReturn()

        mockServer.verify()

        assertTrue(selfTestController.ready)
    }

    @Test
    fun `Tester readiness feiler`() {
        selfTestController.ready = false

        mockServer.expect(ExpectedCount.once(),
                requestTo(URI("http://service/is_alive")))
                .andRespond(withStatus(HttpStatus.OK)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"hei\": 123}"))


        mockMvc.perform(MockMvcRequestBuilders.get("/internal/isReady")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andExpect(content().string(equalTo("Application is ready!")))
                .andReturn()

        mockServer.verify()
    }

}
