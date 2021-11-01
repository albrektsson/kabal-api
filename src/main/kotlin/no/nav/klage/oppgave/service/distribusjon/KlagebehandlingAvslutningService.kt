package no.nav.klage.oppgave.service.distribusjon

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import no.nav.klage.oppgave.clients.kabaldocument.KabalDocumentGateway
import no.nav.klage.oppgave.domain.kafka.EventType
import no.nav.klage.oppgave.domain.kafka.KafkaEvent
import no.nav.klage.oppgave.domain.kafka.KlagevedtakFattet
import no.nav.klage.oppgave.domain.klage.Klagebehandling
import no.nav.klage.oppgave.domain.klage.KlagebehandlingAggregatFunctions.setAvsluttet
import no.nav.klage.oppgave.domain.kodeverk.Rolle
import no.nav.klage.oppgave.repositories.KafkaEventRepository
import no.nav.klage.oppgave.service.KlagebehandlingService
import no.nav.klage.oppgave.util.getLogger
import no.nav.klage.oppgave.util.getSecureLogger
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.*

@Service
@Transactional
class KlagebehandlingAvslutningService(
    private val kafkaEventRepository: KafkaEventRepository,
    private val klagebehandlingService: KlagebehandlingService,
    private val applicationEventPublisher: ApplicationEventPublisher,
    private val kabalDocumentGateway: KabalDocumentGateway
) {

    companion object {
        @Suppress("JAVA_CLASS_ON_COMPANION")
        private val logger = getLogger(javaClass.enclosingClass)
        private val secureLogger = getSecureLogger()
        private val objectMapper = ObjectMapper().registerModule(JavaTimeModule())

    }

    @Transactional
    fun avsluttKlagebehandling(klagebehandlingId: UUID, gammelFlyt: Boolean): Klagebehandling {
        val klagebehandling = klagebehandlingService.getKlagebehandlingForUpdateBySystembruker(klagebehandlingId)
        val vedtak = klagebehandling.getVedtakOrException()

        val journalpostId = if (gammelFlyt) {
            (vedtak.brevmottakere.find { it.rolle == Rolle.PROSESSFULLMEKTIG }
                ?: vedtak.brevmottakere.find { it.rolle == Rolle.KLAGER })!!.journalpostId
        } else {
            kabalDocumentGateway.getJournalpostIdForHovedadressat(klagebehandling.getVedtakOrException().dokumentEnhetId!!)!!
        }

        val eventId = UUID.randomUUID()

        val fattet = KlagevedtakFattet(
            transactionId = eventId,
            kildeReferanse = klagebehandling.kildeReferanse,
            kilde = klagebehandling.kildesystem.navn,
            utfall = vedtak.utfall!!,
            vedtaksbrevReferanse = journalpostId,
            kabalReferanse = vedtak.id.toString()
        )

        kafkaEventRepository.save(
            KafkaEvent(
                id = eventId,
                klagebehandlingId = klagebehandlingId,
                kilde = klagebehandling.kildesystem.navn,
                kildeReferanse = klagebehandling.kildeReferanse,
                jsonPayload = fattet.toJson(),
                type = EventType.KLAGE_VEDTAK
            )
        )

        val event = klagebehandling.setAvsluttet(VedtakDistribusjonService.SYSTEMBRUKER)
        applicationEventPublisher.publishEvent(event)

        return klagebehandling
    }

    private fun Any.toJson(): String = objectMapper.writeValueAsString(this)

}