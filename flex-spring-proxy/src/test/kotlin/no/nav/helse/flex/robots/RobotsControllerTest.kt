package no.nav.helse.flex.robots


import no.nav.helse.flex.TestApplication
import org.hamcrest.Matchers.equalTo
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpHeaders
import org.springframework.test.context.junit4.SpringRunner
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content


@RunWith(SpringRunner::class)
@AutoConfigureMockMvc
@SpringBootTest(classes = [TestApplication::class])
class RobotsControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc


    @Test
    fun `Tester robots endepunkt`() {
        mockMvc.perform(MockMvcRequestBuilders.get("/robots.txt"))
                .andExpect(MockMvcResultMatchers.status().is2xxSuccessful)
                .andExpect(content().string(equalTo("User-agent: *\nDisallow: /")))
                .andExpect(MockMvcResultMatchers.header().string(HttpHeaders.CONTENT_TYPE, "text/plain;charset=UTF-8"))
                .andReturn()
    }

}
