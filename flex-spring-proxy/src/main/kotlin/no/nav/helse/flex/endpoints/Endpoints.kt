package no.nav.helse.flex.endpoints

data class Endpoints(
        val get: List<String> = emptyList(),
        val put: List<String> = emptyList(),
        val post: List<String> = emptyList(),
        val delete: List<String> = emptyList(),
)
