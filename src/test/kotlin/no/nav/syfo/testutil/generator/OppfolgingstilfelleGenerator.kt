package no.nav.syfo.testutil.generator

import no.nav.syfo.oppfolgingstilfelle.domain.KOppfolgingstilfellePeker
import no.nav.syfo.testutil.UserConstants.ARBEIDSTAKER_AKTORID
import no.nav.syfo.testutil.UserConstants.VIRKSOMHETSNUMMER

val generateOppfolgingstilfellePeker =
    KOppfolgingstilfellePeker(
        ARBEIDSTAKER_AKTORID.aktor,
        VIRKSOMHETSNUMMER
    )
