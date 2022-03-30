package no.nav.syfo.oppfolgingstilfelle

import no.nav.syfo.oppfolgingstilfelle.domain.KOversikthendelsetilfelle
import no.nav.syfo.oppfolgingstilfelle.domain.KSyketilfelledag
import java.time.LocalDateTime

var mapKOversikthendelsetilfelle = {
    fnr: String,
    navn: String,
    enhetId: String,
    virksomhetsnummer: String,
    virksomhetsnavn: String,
    tidslinje: List<KSyketilfelledag>,
    tidspunkt: LocalDateTime,
    gradert: Boolean
    ->
    KOversikthendelsetilfelle(
        fnr = fnr,
        navn = navn,
        enhetId = enhetId,
        virksomhetsnummer = virksomhetsnummer,
        virksomhetsnavn = virksomhetsnavn,
        gradert = gradert,
        fom = tidslinje.first().dag,
        tom = tidslinje.last().dag,
        tidspunkt = tidspunkt
    )
}
