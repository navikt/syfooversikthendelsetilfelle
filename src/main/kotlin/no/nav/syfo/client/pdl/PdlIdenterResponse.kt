package no.nav.syfo.client.pdl

data class PdlIdenterResponse(
        val errors: List<PdlError>?,
        val data: PdlHentIdenter?
)

data class PdlHentIdenter(
        val hentIdenter: PdlIdenter?
)

data class PdlIdenter(
        val identer: List<PdlIdent>
)

data class PdlIdent(
        val ident: String,
        val historisk: Boolean,
        val type: String
)


enum class IdentType {
    FOLKEREGISTERIDENT,
    NPID,
    AKTORID
}

fun PdlHentIdenter.aktorId(): String? {
    val identer = this.hentIdenter?.identer

    return if (identer.isNullOrEmpty()) {
        null
    } else {
        identer.first {
            it.type == IdentType.AKTORID.name
        }.ident
    }
}
