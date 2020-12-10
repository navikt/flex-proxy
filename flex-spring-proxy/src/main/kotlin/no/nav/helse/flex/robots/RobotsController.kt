package no.nav.helse.flex.robots

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.ResponseBody


@Controller
class RobotsController {
    @RequestMapping("/robots.txt", method = [RequestMethod.GET], produces = ["text/plain"])
    @ResponseBody
    fun robots(): String {
        return """
            User-agent: *
            Disallow: /
            """.trimIndent()
    }
}
