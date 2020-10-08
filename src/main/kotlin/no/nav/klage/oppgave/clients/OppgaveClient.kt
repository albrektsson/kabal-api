package no.nav.klage.oppgave.clients

import brave.Tracer
import no.nav.klage.oppgave.domain.OppgaveResponse
import no.nav.klage.oppgave.util.getLogger
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono

@Component
class OppgaveClient(
    private val oppgaveWebClient: WebClient,
    private val stsClient: StsClient,
    private val tracer: Tracer,
    @Value("\${spring.application.name}") val applicationName: String
) {

    companion object {
        @Suppress("JAVA_CLASS_ON_COMPANION")
        private val logger = getLogger(javaClass.enclosingClass)
    }

    fun getOppgaver(): OppgaveResponse {
        logger.debug("Fetching oppgaver")

        return oppgaveWebClient.get()
            .uri { uriBuilder ->
                uriBuilder
                    //Try to limit amount when testing
                    .queryParam("opprettetFom", "2020-09-01T07:00:00")
                    .queryParam("statuskategori", "AAPEN")
                    .queryParam("tema", "SYK")
                    .build()
            }
            .header("Authorization", "Bearer ${stsClient.oidcToken()}")
            .header("X-Correlation-ID", tracer.currentSpan().context().traceIdString())
            .header("Nav-Consumer-Id", applicationName)
            .retrieve()
            .bodyToMono<OppgaveResponse>()
            .block() ?: throw RuntimeException("Oppgaver could not be fetched")
    }
}


