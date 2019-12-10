package no.nav.syfo.client.pdl

import com.github.kittinunf.fuel.core.ResponseDeserializable
import com.google.gson.Gson
import no.nav.syfo.util.lowerCapitalize

data class PdlPersonResponse(
        val errors: List<PdlError>?,
        val data: PdlHentPerson?
)

data class PdlError(
        val message: String,
        val locations: List<PdlErrorLocation>,
        val path: List<String>?,
        val extensions: PdlErrorExtension
)

data class PdlErrorLocation(
        val line: Int?,
        val column: Int?
)

data class PdlErrorExtension(
        val code: String?,
        val classification: String
)

data class PdlHentPerson(
        val hentPerson: PdlPerson?
)

data class PdlPerson(
        val navn: List<PdlPersonNavn>
)

data class PdlPersonNavn(
        val fornavn: String,
        val mellomnavn: String?,
        val etternavn: String
)

fun PdlHentPerson.fullName(): String? {
    val name = this.hentPerson?.navn?.get(0)
    name?.let {
        val firstName = name.fornavn.lowerCapitalize()
        val middleName = name.mellomnavn
        val surName = name.etternavn.lowerCapitalize()

        val fullName = if (middleName.isNullOrBlank()) {
            "$firstName $surName"
        } else {
            "$firstName ${middleName.lowerCapitalize()} $surName"
        }
        return fullName
    }
    return null
}
