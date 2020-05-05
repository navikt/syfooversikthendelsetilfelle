package no.nav.syfo.oppfolgingstilfelle

import no.nav.syfo.oppfolgingstilfelle.domain.*
import org.amshove.kluent.shouldEqual
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import java.time.LocalDate
import java.time.LocalDateTime

object KSyketilfelleTest : Spek({

    describe("isLatestSykmeldingGradert") {
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

        it("should return true, if $GRADERT_AKTIVITET is present today") {
            val tidslinje = listOf(
                    KSyketilfelledag(
                            dag = LocalDate.now(),
                            prioritertSyketilfellebit = kSyketilfellebit.copy(
                                    tags = listOf(
                                            SYKMELDING,
                                            GRADERT_AKTIVITET
                                    )
                            )
                    )
            )
            val res = tidslinje.isLatestSykmeldingGradert()

            res shouldEqual true
        }

        it("should return true, if $GRADERT_AKTIVITET is present, but not today") {
            val tidslinje = listOf(
                    KSyketilfelledag(
                            dag = LocalDate.now().minusDays(1),
                            prioritertSyketilfellebit = kSyketilfellebit.copy(
                                    tags = listOf(
                                            SYKMELDING,
                                            GRADERT_AKTIVITET
                                    )
                            )
                    )
            )
            val res = tidslinje.isLatestSykmeldingGradert()

            res shouldEqual true
        }

        it("should return false, if $GRADERT_AKTIVITET is not present") {
            val tidslinje = listOf(
                    KSyketilfelledag(
                            dag = LocalDate.now(),
                            prioritertSyketilfellebit = kSyketilfellebit.copy(
                                    tags = listOf(SYKMELDING)
                            )
                    )
            )
            val res = tidslinje.isLatestSykmeldingGradert()

            res shouldEqual false
        }

        it("should return false, if $SYKEPENGESOKNAD is present and $GRADERT_AKTIVITET is not present") {
            val tidslinje = listOf(
                    KSyketilfelledag(
                            dag = LocalDate.now(),
                            prioritertSyketilfellebit = kSyketilfellebit.copy(
                                    tags = listOf(SYKEPENGESOKNAD)
                            )
                    )
            )
            val res = tidslinje.isLatestSykmeldingGradert()

            res shouldEqual false
        }


        it("should return false, if $GRADERT_AKTIVITET is not present") {
            val tidslinje = listOf(
                    KSyketilfelledag(
                            dag = LocalDate.now(),
                            prioritertSyketilfellebit = kSyketilfellebit.copy(
                                    tags = emptyList()
                            )
                    )
            )
            val res = tidslinje.isLatestSykmeldingGradert()

            res shouldEqual false
        }

        it("should return true, if $SYKEPENGESOKNAD and $SYKMELDING with $GRADERT_AKTIVITET is present") {
            val tidslinje = listOf(
                    KSyketilfelledag(
                            dag = LocalDate.now(),
                            prioritertSyketilfellebit = kSyketilfellebit.copy(
                                    tags = listOf(SYKEPENGESOKNAD)
                            )
                    ),
                    KSyketilfelledag(
                            dag = LocalDate.now().minusDays(10),
                            prioritertSyketilfellebit = kSyketilfellebit.copy(
                                    tags = listOf(
                                            SYKMELDING,
                                            GRADERT_AKTIVITET
                                    )
                            )
                    )
            )

            val res = tidslinje.isLatestSykmeldingGradert()
            res shouldEqual true
        }

        it("should return false, if $SYKEPENGESOKNAD and $SYKMELDING without $GRADERT_AKTIVITET is present") {
            val tidslinje = listOf(
                    KSyketilfelledag(
                            dag = LocalDate.now(),
                            prioritertSyketilfellebit = kSyketilfellebit.copy(
                                    tags = listOf(SYKEPENGESOKNAD)
                            )
                    ),
                    KSyketilfelledag(
                            dag = LocalDate.now().minusDays(10),
                            prioritertSyketilfellebit = kSyketilfellebit.copy(
                                    tags = listOf(
                                            SYKMELDING
                                    )
                            )
                    )
            )
            val res = tidslinje.isLatestSykmeldingGradert()

            res shouldEqual false
        }

        it("should return true, if first $SYKMELDING without $GRADERT_AKTIVITET and then $SYKMELDING with $GRADERT_AKTIVITET") {
            val tidslinje = listOf(
                    KSyketilfelledag(
                            dag = LocalDate.now().minusDays(5),
                            prioritertSyketilfellebit = kSyketilfellebit.copy(
                                    tags = listOf(SYKMELDING)
                            )
                    ),
                    KSyketilfelledag(
                            dag = LocalDate.now().plusDays(5),
                            prioritertSyketilfellebit = kSyketilfellebit.copy(
                                    tags = listOf(
                                            SYKMELDING,
                                            GRADERT_AKTIVITET
                                    )
                            )
                    )
            )
            val res = tidslinje.isLatestSykmeldingGradert()

            res shouldEqual true
        }

        it("should return false, if first $SYKMELDING with $GRADERT_AKTIVITET and then $SYKMELDING without $GRADERT_AKTIVITET") {
            val tidslinje = listOf(
                    KSyketilfelledag(
                            dag = LocalDate.now().minusDays(5),
                            prioritertSyketilfellebit = kSyketilfellebit.copy(
                                    tags = listOf(
                                            SYKMELDING,
                                            GRADERT_AKTIVITET
                                    )
                            )
                    ),
                    KSyketilfelledag(
                            dag = LocalDate.now().plusDays(5),
                            prioritertSyketilfellebit = kSyketilfellebit.copy(
                                    tags = listOf(SYKMELDING)
                            )
                    )
            )
            val res = tidslinje.isLatestSykmeldingGradert()

            res shouldEqual false
        }
    }
})
