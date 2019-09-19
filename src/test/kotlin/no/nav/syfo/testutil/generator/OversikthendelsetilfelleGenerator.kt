package no.nav.syfo.testutil.generator

import no.nav.syfo.oppfolgingstilfelle.domain.KOversikthendelsetilfelle
import no.nav.syfo.testutil.UserConstants.ARBEIDSTAKER_FNR
import no.nav.syfo.testutil.UserConstants.NAV_ENHET
import no.nav.syfo.testutil.UserConstants.VIRKSOMHETSNUMMER
import java.time.LocalDate
import java.time.LocalDateTime

val generateOversikthendelsetilfelle =
        KOversikthendelsetilfelle(
                fnr = ARBEIDSTAKER_FNR,
                enhetId = NAV_ENHET,
                virksomhetsnummer = VIRKSOMHETSNUMMER,
                gradert = false,
                fom = LocalDate.now().minusDays(56),
                tom = LocalDate.now().plusDays(16),
                tidspunkt = LocalDateTime.now()
        )
