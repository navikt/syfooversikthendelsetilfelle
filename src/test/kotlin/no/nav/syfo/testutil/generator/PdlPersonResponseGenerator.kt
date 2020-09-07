package no.nav.syfo.testutil.generator

import no.nav.syfo.client.pdl.*
import no.nav.syfo.testutil.UserConstants

fun generatePdlPersonNavn(): PdlPersonNavn {
    return PdlPersonNavn(
        fornavn = UserConstants.ARBEIDSTAKER_NAME_FIRST,
        mellomnavn = UserConstants.ARBEIDSTAKER_NAME_MIDDLE,
        etternavn = UserConstants.ARBEIDSTAKER_NAME_LAST
    )
}

fun generatePdlHentPerson(
    pdlPersonNavn: PdlPersonNavn?
): PdlHentPerson {
    return PdlHentPerson(
        hentPerson = PdlPerson(
            navn = listOf(
                pdlPersonNavn ?: generatePdlPersonNavn()
            )
        )
    )
}

val genereatePdlPersonResponse = PdlPersonResponse(
    errors = null,
    data = generatePdlHentPerson(generatePdlPersonNavn())
)
