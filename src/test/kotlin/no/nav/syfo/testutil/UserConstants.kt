package no.nav.syfo.testutil

import no.nav.syfo.domain.AktorId
import no.nav.syfo.domain.PersonIdentNumber

object UserConstants {

    const val ARBEIDSTAKER_NAME_FIRST = "First"
    const val ARBEIDSTAKER_NAME_MIDDLE = "Middle"
    const val ARBEIDSTAKER_NAME_LAST = "Last"

    val ARBEIDSTAKER_FNR = PersonIdentNumber("12345678912")
    val ARBEIDSTAKER_2_FNR = PersonIdentNumber(ARBEIDSTAKER_FNR.value.replace("2", "1"))
    val ARBEIDSTAKER_AKTORID = AktorId(ARBEIDSTAKER_FNR.value + "01")
    val ARBEIDSTAKER_2_AKTORID = AktorId(ARBEIDSTAKER_2_FNR.value + "01")
    const val NAV_ENHET = "0330"
    const val NAV_ENHET_2 = "0331"
    const val VIRKSOMHETSNUMMER = "123456789"
    const val VIRKSOMHETSNUMMER_ANNET = "123456788"
    const val VIRKSOMHETSNAVN = "Test AS"
}
