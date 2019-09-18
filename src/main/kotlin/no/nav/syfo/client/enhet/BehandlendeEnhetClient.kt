package no.nav.syfo.client.enhet

import com.github.kittinunf.fuel.httpGet
import no.nav.syfo.client.sts.StsRestClient

class BehandlendeEnhetClient(
        private val baseUrl: String,
        private val stsRestClient: StsRestClient
) {

    fun getEnhet(fnr: String, callId: String): BehandlendeEnhet {
        val bearer = stsRestClient.token()

        val (_, _, result) = getBehandlendeEnhetUrl(fnr).httpGet()
                .header(mapOf(
                        "Authorization" to "Bearer $bearer",
                        "Accept" to "application/json",
                        "Nav-Call-Id" to callId,
                        "Nav-Consumer-Id" to "syfooversikthendelsetilfelle"
                ))
                .responseObject(BehandlendeEnhet.Deserializer())

        return result.get()
    }

    private fun getBehandlendeEnhetUrl(bruker: String): String {
        return "$baseUrl/api/$bruker"
    }
}
