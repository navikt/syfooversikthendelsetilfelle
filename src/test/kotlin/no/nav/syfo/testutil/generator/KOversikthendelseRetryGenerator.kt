package no.nav.syfo.testutil.generator

import no.nav.syfo.oppfolgingstilfelle.retry.KOppfolgingstilfelleRetry
import no.nav.syfo.testutil.UserConstants.ARBEIDSTAKER_AKTORID
import no.nav.syfo.testutil.UserConstants.VIRKSOMHETSNUMMER
import java.time.LocalDateTime

val generateKOppfolgingstilfelleRetry =
    KOppfolgingstilfelleRetry(
        created = LocalDateTime.now(),
        retryTime = LocalDateTime.now(),
        retriedCount = 0,
        aktorId = ARBEIDSTAKER_AKTORID.aktor,
        orgnummer = VIRKSOMHETSNUMMER
    ).copy()
