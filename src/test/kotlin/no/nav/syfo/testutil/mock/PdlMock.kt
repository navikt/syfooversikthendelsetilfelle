package no.nav.syfo.testutil.mock

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.ktor.application.*
import io.ktor.features.*
import io.ktor.jackson.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import no.nav.syfo.client.pdl.*
import no.nav.syfo.testutil.UserConstants
import no.nav.syfo.testutil.getRandomPort

class PdlMock(gradering: Gradering? = null) {
    private val port = getRandomPort()
    val url = "http://localhost:$port"
    val pdlPersonResponse = generatePdlPersonResponse(gradering)
    val server = mockPdlServer(port, pdlPersonResponse)

    private fun mockPdlServer(
        port: Int,
        pdlPersonResponse: PdlPersonResponse
    ): NettyApplicationEngine {
        return embeddedServer(
            factory = Netty,
            port = port
        ) {
            install(ContentNegotiation) {
                jackson {
                    registerKotlinModule()
                    registerModule(JavaTimeModule())
                    configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
                }
            }
            routing {
                post {
                    call.respond(pdlPersonResponse)
                }
            }
        }
    }
}

fun generatePdlPersonResponse(gradering: Gradering? = null) = PdlPersonResponse(
    errors = null,
    data = generatePdlHentPerson(
        generatePdlPersonNavn(),
        generateAdressebeskyttelse(gradering = gradering)
    )
)

fun generatePdlPersonNavn(): PdlPersonNavn {
    return PdlPersonNavn(
        fornavn = UserConstants.ARBEIDSTAKER_NAME_FIRST,
        mellomnavn = UserConstants.ARBEIDSTAKER_NAME_MIDDLE,
        etternavn = UserConstants.ARBEIDSTAKER_NAME_LAST
    )
}

fun generateAdressebeskyttelse(
    gradering: Gradering? = null
): Adressebeskyttelse {
    return Adressebeskyttelse(
        gradering = gradering ?: Gradering.UGRADERT
    )
}

fun generatePdlHentPerson(
    pdlPersonNavn: PdlPersonNavn?,
    adressebeskyttelse: Adressebeskyttelse? = null
): PdlHentPerson {
    return PdlHentPerson(
        hentPerson = PdlPerson(
            navn = listOf(
                pdlPersonNavn ?: generatePdlPersonNavn()
            ),
            adressebeskyttelse = listOf(
                adressebeskyttelse ?: generateAdressebeskyttelse()
            )
        )
    )
}
