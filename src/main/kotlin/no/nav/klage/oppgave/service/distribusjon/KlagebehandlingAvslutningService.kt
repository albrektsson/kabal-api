package no.nav.klage.oppgave.service.distribusjon

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import no.nav.klage.oppgave.clients.kabaldocument.KabalDocumentGateway
import no.nav.klage.oppgave.domain.kafka.*
import no.nav.klage.oppgave.domain.klage.BehandlingAggregatFunctions.setAvsluttet
import no.nav.klage.oppgave.domain.klage.Klagebehandling
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
        private val objectMapperBehandlingEvents = ObjectMapper().registerModule(JavaTimeModule()).configure(
            SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        const val SYSTEMBRUKER = "SYSTEMBRUKER" //TODO ??
        const val SYSTEM_JOURNALFOERENDE_ENHET = "9999"

    }

    @Transactional
    fun avsluttKlagebehandling(klagebehandlingId: UUID): Klagebehandling {
        val klagebehandling = klagebehandlingService.getKlagebehandlingForUpdateBySystembruker(klagebehandlingId)

        val journalpostId = klagebehandling.currentDelbehandling().hovedAdressatJournalpostId

        val eventId = UUID.randomUUID()

        val vedtakFattet = KlagevedtakFattet(
            eventId = eventId,
            kildeReferanse = klagebehandling.kildeReferanse,
            kilde = klagebehandling.kildesystem.navn,
            utfall = ExternalUtfall.valueOf(klagebehandling.currentDelbehandling().utfall!!.name),
            vedtaksbrevReferanse = journalpostId,
            kabalReferanse = klagebehandling.currentDelbehandling().id.toString()
        )
        kafkaEventRepository.save(
            KafkaEvent(
                id = UUID.randomUUID(),
                klagebehandlingId = klagebehandlingId,
                kilde = klagebehandling.kildesystem.navn,
                kildeReferanse = klagebehandling.kildeReferanse,
                jsonPayload = vedtakFattet.toJson(),
                type = EventType.KLAGE_VEDTAK
            )
        )

        val behandlingEvent = BehandlingEvent(
            eventId = eventId,
            kildeReferanse = klagebehandling.kildeReferanse,
            kilde = klagebehandling.kildesystem.navn,
            kabalReferanse = klagebehandling.currentDelbehandling().id.toString(),
            type = BehandlingEventType.KLAGEBEHANDLING_AVSLUTTET,
            detaljer = BehandlingDetaljer(
                klagebehandlingAvsluttet = KlagebehandlingAvsluttetDetaljer(
                    avsluttet = klagebehandling.avsluttetAvSaksbehandler!!,
                    utfall = ExternalUtfall.valueOf(klagebehandling.currentDelbehandling().utfall!!.name),
                    journalpostReferanser = listOfNotNull(journalpostId) //TODO: Må endres når dokumenter i arbeid branchen merges inn og tas i bruk
                )
            )
        )
        kafkaEventRepository.save(
            KafkaEvent(
                id = UUID.randomUUID(),
                klagebehandlingId = klagebehandlingId,
                kilde = klagebehandling.kildesystem.navn,
                kildeReferanse = klagebehandling.kildeReferanse,
                jsonPayload = objectMapperBehandlingEvents.writeValueAsString(behandlingEvent),
                type = EventType.BEHANDLING_EVENT
            )
        )

        val event = klagebehandling.setAvsluttet(SYSTEMBRUKER)
        applicationEventPublisher.publishEvent(event)

        return klagebehandling
    }

    private fun Any.toJson(): String = objectMapper.writeValueAsString(this)

}