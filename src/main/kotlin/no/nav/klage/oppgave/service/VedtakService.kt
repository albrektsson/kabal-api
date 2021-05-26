package no.nav.klage.oppgave.service

import no.nav.klage.oppgave.api.mapper.KlagebehandlingMapper
import no.nav.klage.oppgave.api.view.VedleggView
import no.nav.klage.oppgave.api.view.VedtakFullfoerInput
import no.nav.klage.oppgave.api.view.VedtakVedleggInput
import no.nav.klage.oppgave.clients.joark.JoarkClient
import no.nav.klage.oppgave.clients.saf.graphql.Journalstatus.FERDIGSTILT
import no.nav.klage.oppgave.clients.saf.graphql.SafGraphQlClient
import no.nav.klage.oppgave.clients.saf.rest.ArkivertDokument
import no.nav.klage.oppgave.domain.klage.Klagebehandling
import no.nav.klage.oppgave.domain.klage.KlagebehandlingAggregatFunctions.setGrunnInVedtak
import no.nav.klage.oppgave.domain.klage.KlagebehandlingAggregatFunctions.setHjemlerInVedtak
import no.nav.klage.oppgave.domain.klage.KlagebehandlingAggregatFunctions.setJournalpostIdInVedtak
import no.nav.klage.oppgave.domain.klage.KlagebehandlingAggregatFunctions.setUtfallInVedtak
import no.nav.klage.oppgave.domain.klage.KlagebehandlingAggregatFunctions.setVedtakFerdigstiltIJoark
import no.nav.klage.oppgave.domain.klage.Vedtak
import no.nav.klage.oppgave.domain.kodeverk.Grunn
import no.nav.klage.oppgave.domain.kodeverk.Hjemmel
import no.nav.klage.oppgave.domain.kodeverk.Utfall
import no.nav.klage.oppgave.exceptions.JournalpostFinalizationException
import no.nav.klage.oppgave.exceptions.JournalpostNotFoundException
import no.nav.klage.oppgave.exceptions.UtfallNotSetException
import no.nav.klage.oppgave.exceptions.VedtakFinalizedException
import no.nav.klage.oppgave.util.AttachmentValidator
import no.nav.klage.oppgave.util.getLogger
import no.nav.klage.oppgave.util.getSecureLogger
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile
import java.util.*

@Service
@Transactional
class VedtakService(
    private val klagebehandlingService: KlagebehandlingService,
    private val applicationEventPublisher: ApplicationEventPublisher,
    private val attachmentValidator: AttachmentValidator,
    private val joarkClient: JoarkClient,
    private val dokumentService: DokumentService,
    private val safClient: SafGraphQlClient,
    private val tilgangService: TilgangService,
    private val klagebehandlingMapper: KlagebehandlingMapper
) {

    companion object {
        @Suppress("JAVA_CLASS_ON_COMPANION")
        private val logger = getLogger(javaClass.enclosingClass)
        private val secureLogger = getSecureLogger()
    }

    @Transactional(readOnly = true)
    fun getVedtak(klagebehandling: Klagebehandling, vedtakId: UUID): Vedtak {
        return klagebehandling.getVedtak(vedtakId)
    }

    fun setUtfall(
        klagebehandling: Klagebehandling,
        vedtakId: UUID,
        utfall: Utfall?,
        utfoerendeSaksbehandlerIdent: String
    ): Vedtak {
        val event =
            klagebehandling.setUtfallInVedtak(vedtakId, utfall, utfoerendeSaksbehandlerIdent)
        applicationEventPublisher.publishEvent(event)
        return klagebehandling.getVedtak(vedtakId)
    }

    fun setGrunn(
        klagebehandling: Klagebehandling,
        vedtakId: UUID,
        grunn: Grunn?,
        utfoerendeSaksbehandlerIdent: String
    ): Vedtak {
        val event =
            klagebehandling.setGrunnInVedtak(vedtakId, grunn, utfoerendeSaksbehandlerIdent)
        applicationEventPublisher.publishEvent(event)
        return klagebehandling.getVedtak(vedtakId)
    }

    fun setHjemler(
        klagebehandling: Klagebehandling,
        vedtakId: UUID,
        hjemler: Set<Hjemmel>,
        utfoerendeSaksbehandlerIdent: String
    ): Vedtak {
        val event =
            klagebehandling.setHjemlerInVedtak(vedtakId, hjemler, utfoerendeSaksbehandlerIdent)
        applicationEventPublisher.publishEvent(event)
        return klagebehandling.getVedtak(vedtakId)
    }

    fun setJournalpostId(
        klagebehandling: Klagebehandling,
        vedtakId: UUID,
        journalpostId: String,
        utfoerendeSaksbehandlerIdent: String
    ): Vedtak {
        val event =
            klagebehandling.setJournalpostIdInVedtak(vedtakId, journalpostId, utfoerendeSaksbehandlerIdent)
        applicationEventPublisher.publishEvent(event)
        return klagebehandling.getVedtak(vedtakId)

    }

    fun markerVedtakSomFerdigstilt(
        klagebehandling: Klagebehandling,
        vedtakId: UUID,
        utfoerendeSaksbehandlerIdent: String
    ): Vedtak {
        val event =
            klagebehandling.setVedtakFerdigstiltIJoark(vedtakId, utfoerendeSaksbehandlerIdent)
        applicationEventPublisher.publishEvent(event)
        return klagebehandling.getVedtak(vedtakId)
    }

    @Transactional
    fun knyttVedtaksFilTilVedtak(
        klagebehandlingId: UUID,
        vedtakId: UUID,
        input: VedtakVedleggInput,
        innloggetIdent: String
    ): VedleggView? {
        val klagebehandling = klagebehandlingService.getKlagebehandlingForUpdate(
            klagebehandlingId,
            input.klagebehandlingVersjon
        )

        tilgangService.verifySaksbehandlersTilgangTilEnhet(klagebehandling.tildeltEnhet!!)

        val vedtak = klagebehandling.getVedtak(vedtakId)

        if (vedtak.ferdigstiltIJoark != null) throw VedtakFinalizedException("Vedtak med id $vedtakId er ferdigstilt")

        addFileToVedtak(
            klagebehandling,
            vedtak,
            input.vedlegg,
            innloggetIdent
        )

        return getVedleggView(
            klagebehandling,
            vedtakId,
            innloggetIdent
        )
    }


    private fun addFileToVedtak(
        klagebehandling: Klagebehandling,
        vedtak: Vedtak,
        vedlegg: MultipartFile,
        utfoerendeSaksbehandlerIdent: String
    ): Vedtak {
        attachmentValidator.validateAttachment(vedlegg)
        val journalpostId = joarkClient.createJournalpost(klagebehandling, vedlegg, klagebehandling.tildeltEnhet!!)

        return setJournalpostId(
            klagebehandling,
            vedtak.id,
            journalpostId,
            utfoerendeSaksbehandlerIdent
        )
    }

    fun getVedleggView(
        klagebehandling: Klagebehandling,
        vedtakId: UUID,
        utfoerendeSaksbehandlerIdent: String
    ): VedleggView? {
        val vedtak = klagebehandling.getVedtak(vedtakId)
        if (vedtak.journalpostId == null) throw JournalpostNotFoundException("Vedtak med id $vedtakId er ikke journalført")
        val mainDokument = dokumentService.getMainDokument(vedtak.journalpostId!!)
        val mainDokumentName = dokumentService.getMainDokumentTitle(vedtak.journalpostId!!)
        return klagebehandlingMapper.mapArkivertDokumentToVedleggView(
            mainDokument,
            mainDokumentName
        )
    }

    fun getVedleggArkivertDokument(
        klagebehandling: Klagebehandling,
        vedtakId: UUID,
        utfoerendeSaksbehandlerIdent: String
    ): ArkivertDokument {
        val vedtak = klagebehandling.getVedtak(vedtakId)
        if (vedtak.journalpostId == null) throw JournalpostNotFoundException("Vedtak med id $vedtakId er ikke journalført")
        return dokumentService.getMainDokument(vedtak.journalpostId!!)
    }

    fun ferdigstillVedtak(
        klagebehandlingId: UUID,
        vedtakId: UUID,
        input: VedtakFullfoerInput,
        innloggetIdent: String
    ) {
        val klagebehandling = klagebehandlingService.getKlagebehandlingForUpdate(
            klagebehandlingId,
            input.klagebehandlingVersjon
        )
        val vedtak = klagebehandling.getVedtak(vedtakId)
        if (vedtak.ferdigstiltIJoark != null) throw VedtakFinalizedException("Vedtak med id $vedtakId er allerede ferdigstilt")
        if (vedtak.journalpostId == null) throw JournalpostNotFoundException("Vedtak med id $vedtakId er ikke journalført")
        if (vedtak.utfall != null) throw UtfallNotSetException("Utfall på vedtak $vedtakId er ikke satt")

        ferdigstillJournalpost(
            klagebehandling,
            vedtak,
            innloggetIdent,
            klagebehandling.tildeltEnhet!!
        )
        if (klagebehandling.vedtak.all { it.ferdigstiltIJoark != null }) {
            klagebehandlingService.markerKlagebehandlingSomAvsluttetAvSaksbehandler(klagebehandling, innloggetIdent)
        }
    }

    private fun ferdigstillJournalpost(
        klagebehandling: Klagebehandling,
        vedtak: Vedtak,
        utfoerendeSaksbehandlerIdent: String,
        journalfoerendeEnhet: String
    ): Vedtak? {
        return try {
            val journalpost = safClient.getJournalpost(vedtak.journalpostId!!)
                ?: throw JournalpostNotFoundException("Journalpost med id ${vedtak.journalpostId} finnes ikke")
            if (journalpost.journalstatus != FERDIGSTILT) {
                joarkClient.finalizeJournalpost(vedtak.journalpostId!!, journalfoerendeEnhet)
            }
            markerVedtakSomFerdigstilt(klagebehandling, vedtak.id, utfoerendeSaksbehandlerIdent)

        } catch (e: Exception) {
            logger.warn("Kunne ikke ferdigstille journalpost ${vedtak.journalpostId}")
            throw JournalpostFinalizationException("Klarte ikke å journalføre vedtak")
        }
    }
}
