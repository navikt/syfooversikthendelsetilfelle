package no.nav.syfo.oppfolgingstilfelle

import no.nav.syfo.oppfolgingstilfelle.domain.KSyketilfellebit
import no.nav.syfo.oppfolgingstilfelle.domain.KSyketilfelledag
import org.amshove.kluent.shouldEqual
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import java.time.LocalDate
import java.time.LocalDateTime

object OppfolgingstilfelleServiceTest : Spek({

    describe("isGradertToday") {
        val kSyketilfellebit = KSyketilfellebit(
                id = "id",
                aktorId = "aktorId",
                orgnummer = "orgnummer",
                opprettet = LocalDateTime.now(),
                inntruffet = LocalDateTime.now(),
                tags = emptyList(),
                ressursId = "ressursId",
                fom = LocalDateTime.now().minusDays(70),
                tom = LocalDateTime.now().plusDays(70)
        )
        val tagsMedGradertAktivitet = listOf(GRADERT_AKTIVITET)
        val tagsUtenGradertAktivitet: List<String> = emptyList()

        it("should return true, if $GRADERT_AKTIVITET is present today") {
            val tidslinje = listOf(
                    KSyketilfelledag(
                            dag = LocalDate.now(),
                            prioritertSyketilfellebit = kSyketilfellebit.copy(
                                    tags = tagsMedGradertAktivitet
                            )
                    )
            )

            val res = isGradertToday(tidslinje)

            res shouldEqual true
        }

        it("should return false, if $GRADERT_AKTIVITET is present, but not today") {
            val tidslinje = listOf(
                    KSyketilfelledag(
                            dag = LocalDate.now().minusDays(1),
                            prioritertSyketilfellebit = kSyketilfellebit.copy(
                                    tags = tagsMedGradertAktivitet
                            )
                    )
            )

            val res = isGradertToday(tidslinje)

            res shouldEqual false
        }

        it("should return false, if $GRADERT_AKTIVITET is not present") {
            val tidslinje = listOf(
                    KSyketilfelledag(
                            dag = LocalDate.now(),
                            prioritertSyketilfellebit = kSyketilfellebit.copy(
                                    tags = tagsUtenGradertAktivitet
                            )
                    )
            )

            val res = isGradertToday(tidslinje)

            res shouldEqual false
        }
    }
})
