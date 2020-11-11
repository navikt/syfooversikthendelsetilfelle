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
    val navn: List<PdlPersonNavn>,
    val adressebeskyttelse: List<Adressebeskyttelse>?
)

data class PdlPersonNavn(
    val fornavn: String,
    val mellomnavn: String?,
    val etternavn: String
)

data class Adressebeskyttelse(
    val gradering: Gradering
)

enum class Gradering {
    STRENGT_FORTROLIG_UTLAND,
    STRENGT_FORTROLIG,
    FORTROLIG,
    UGRADERT
}

fun PdlHentPerson.fullName(): String? {
    val nameList = this.hentPerson?.navn
    if (nameList.isNullOrEmpty()) {
        return null
    }
    nameList.first().let {
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

fun PdlHentPerson?.isKode6(): Boolean {
    val adressebeskyttelse = this?.hentPerson?.adressebeskyttelse
    return if (adressebeskyttelse.isNullOrEmpty()) {
        false
    } else {
        return adressebeskyttelse.any {
            it.isKode6()
        }
    }
}

fun Adressebeskyttelse.isKode6(): Boolean {
    return this.gradering == Gradering.STRENGT_FORTROLIG || this.gradering == Gradering.STRENGT_FORTROLIG_UTLAND
}

fun PdlError.errorMessage(): String {
    return "${this.message} with code: ${extensions.code} and classification: ${extensions.classification}"
}
