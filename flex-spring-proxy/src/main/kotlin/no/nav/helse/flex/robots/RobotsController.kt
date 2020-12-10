package no.nav.helse.flex.robots

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseBody


@Controller
class RobotsController {
    @RequestMapping("/robots.txt")
    @ResponseBody
    fun robots(): String {
        return """
            User-agent: *
            Disallow: /
            """.trimIndent()
    }
}
