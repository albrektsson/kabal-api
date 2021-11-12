package no.nav.klage.oppgave.clients.kaka

import no.nav.klage.oppgave.clients.kaka.model.request.SaksdataInput
import no.nav.klage.oppgave.domain.klage.Klagebehandling
import no.nav.klage.oppgave.util.getLogger
import no.nav.klage.oppgave.util.getSecureLogger
import org.springframework.stereotype.Service
import java.util.*

@Service
class KakaApiGateway(private val kakaApiClient: KakaApiClient) {

    companion object {
        @Suppress("JAVA_CLASS_ON_COMPANION")
        private val logger = getLogger(javaClass.enclosingClass)
        private val secureLogger = getSecureLogger()
    }

    fun createKvalitetsvurdering(): UUID = kakaApiClient.createKvalitetsvurdering().id

    fun finalizeKlagebehandling(klagebehandling: Klagebehandling) {
        kakaApiClient.finalizeKlagebehandling(klagebehandling.toSaksdataInput())
    }

    private fun Klagebehandling.toSaksdataInput(): SaksdataInput {
        val vedtak = getVedtakOrException()
        return SaksdataInput(
            sakenGjelder = sakenGjelder.partId.value,
            sakstype = type.id,
            tema = tema.id,
            mottattKlageinstans = mottattKlageinstans.toLocalDate(),
            vedtaksinstansEnhet = avsenderEnhetFoersteinstans,
            mottattVedtaksinstans = mottattFoersteinstans,
            utfall = vedtak.utfall!!.id,
            hjemler = vedtak.hjemler.map { it.id },
            kvalitetsvurderingId = kakaKvalitetsvurderingId!!,
            avsluttetAvSaksbehandler = avsluttetAvSaksbehandler!!,
            utfoerendeSaksbehandler = tildeling?.saksbehandlerident!!,
        )
    }
}
