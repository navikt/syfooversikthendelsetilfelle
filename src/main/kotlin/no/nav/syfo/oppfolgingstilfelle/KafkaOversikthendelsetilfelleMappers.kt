package no.nav.syfo.oppfolgingstilfelle

import no.nav.syfo.oppfolgingstilfelle.domain.KOversikthendelsetilfelle
import no.nav.syfo.oppfolgingstilfelle.domain.KSyketilfelledag
import java.time.LocalDateTime

var mapKOversikthendelsetilfelle = { fnr: String, enhetId: String, virksomhetsnummer: String, virksomhetsnavn: String, tidslinje: List<KSyketilfelledag>, gradert: Boolean ->
    KOversikthendelsetilfelle(
            fnr = fnr,
            enhetId = enhetId,
            virksomhetsnummer = virksomhetsnummer,
            virksomhetsnavn = virksomhetsnavn,
            gradert = gradert,
            fom = tidslinje.first().dag,
            tom = tidslinje.last().dag,
            tidspunkt = LocalDateTime.now()
    )
}
