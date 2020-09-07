package no.nav.syfo.client.enhet

import com.github.kittinunf.fuel.core.ResponseDeserializable
import com.google.gson.Gson

data class BehandlendeEnhet(
    var enhetId: String,
    var navn: String
) {
    class Deserializer : ResponseDeserializable<BehandlendeEnhet> {
        override fun deserialize(content: String): BehandlendeEnhet? = Gson().fromJson(content, BehandlendeEnhet::class.java)
    }
}
