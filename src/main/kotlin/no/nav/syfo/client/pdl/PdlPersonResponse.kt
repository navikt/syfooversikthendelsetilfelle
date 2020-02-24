package no.nav.syfo.client.pdl

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
    val nameList = this.hentPerson?.navn
    if (nameList.isNullOrEmpty()) {
        return null
    }
    nameList[0].let {
        val firstName = it.fornavn.lowerCapitalize()
        val middleName = it.mellomnavn
        val surName = it.etternavn.lowerCapitalize()

        return if (middleName.isNullOrBlank()) {
            "$firstName $surName"
        } else {
            "$firstName ${middleName.lowerCapitalize()} $surName"
        }
    }
}

fun PdlError.errorMessage(): String {
    return "${this.message} with code: ${extensions.code} and classification: ${extensions.classification}"
}
