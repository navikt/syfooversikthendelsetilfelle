package no.nav.syfo.testutil.generator

import no.nav.syfo.oppfolgingstilfelle.domain.KOppfolgingstilfelle
import no.nav.syfo.oppfolgingstilfelle.domain.KSyketilfelledag
import no.nav.syfo.testutil.UserConstants.VIRKSOMHETSNUMMER
import java.time.LocalDate
import java.time.LocalDateTime

val generateOppfolgingstilfelle =
        KOppfolgingstilfelle(
                "",
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
