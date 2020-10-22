package no.nav.syfo.testutil.generator

import no.nav.syfo.client.syketilfelle.KOppfolgingstilfelle
import no.nav.syfo.oppfolgingstilfelle.domain.KOppfolgingstilfellePeker
import no.nav.syfo.oppfolgingstilfelle.domain.KSyketilfelledag
import no.nav.syfo.testutil.UserConstants.ARBEIDSTAKER_AKTORID
import no.nav.syfo.testutil.UserConstants.VIRKSOMHETSNUMMER
import java.time.LocalDate
import java.time.LocalDateTime

val generateOppfolgingstilfelle =
    KOppfolgingstilfelle(
        ARBEIDSTAKER_AKTORID.aktor,
        VIRKSOMHETSNUMMER,
        emptyList(),
        KSyketilfelledag(
            LocalDate.now().minusDays(1),
            null
        ),
        0,
        false,
        LocalDateTime.now()
    )

val generateOppfolgingstilfellePeker =
    KOppfolgingstilfellePeker(
        ARBEIDSTAKER_AKTORID.aktor,
        VIRKSOMHETSNUMMER
    )
