package no.nav.syfo.helper

object UserConstants {

    private const val MOCK_AKTORID_PREFIX = "10"

    val BRUKER_FNR = "12345678912"
    val BRUKER_AKTORID = mockAktorId(BRUKER_FNR)

    fun mockAktorId(fnr: String): String {
        return MOCK_AKTORID_PREFIX + fnr
    }
}
