package no.nav.klage.oppgave.service

import no.nav.klage.oppgave.api.view.VedtakFullfoerInput
import no.nav.klage.oppgave.api.view.VedtakSlettVedleggInput
import no.nav.klage.oppgave.api.view.VedtakVedleggInput
import no.nav.klage.oppgave.domain.klage.Klagebehandling
import no.nav.klage.oppgave.domain.klage.KlagebehandlingAggregatFunctions.setGrunnInVedtak
import no.nav.klage.oppgave.domain.klage.KlagebehandlingAggregatFunctions.setHjemlerInVedtak
import no.nav.klage.oppgave.domain.klage.KlagebehandlingAggregatFunctions.setMellomlagerIdOgOpplastetInVedtak
import no.nav.klage.oppgave.domain.klage.KlagebehandlingAggregatFunctions.setUtfallInVedtak
import no.nav.klage.oppgave.domain.klage.Vedtak
import no.nav.klage.oppgave.domain.kodeverk.Grunn
import no.nav.klage.oppgave.domain.kodeverk.Hjemmel
import no.nav.klage.oppgave.domain.kodeverk.Utfall
import no.nav.klage.oppgave.exceptions.MissingTilgangException
import no.nav.klage.oppgave.exceptions.UtfallNotSetException
import no.nav.klage.oppgave.exceptions.VedtakFinalizedException
import no.nav.klage.oppgave.exceptions.VedtakNotFoundException
import no.nav.klage.oppgave.util.AttachmentValidator
import no.nav.klage.oppgave.util.getLogger
import no.nav.klage.oppgave.util.getSecureLogger
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.*

@Service
@Transactional
class VedtakService(
    private val klagebehandlingService: KlagebehandlingService,
    private val applicationEventPublisher: ApplicationEventPublisher,
    private val attachmentValidator: AttachmentValidator,
    private val tilgangService: TilgangService,
    private val fileApiService: FileApiService
) {

    companion object {
        @Suppress("JAVA_CLASS_ON_COMPANION")
        private val logger = getLogger(javaClass.enclosingClass)
        private val secureLogger = getSecureLogger()
    }

    @Transactional(readOnly = true)
    fun getVedtak(klagebehandling: Klagebehandling): Vedtak {
        return klagebehandling.getVedtakOrException()
    }

    fun setUtfall(
        klagebehandling: Klagebehandling,
        utfall: Utfall?,
        utfoerendeSaksbehandlerIdent: String
    ): Klagebehandling {
        val event =
            klagebehandling.setUtfallInVedtak(utfall, utfoerendeSaksbehandlerIdent)
        applicationEventPublisher.publishEvent(event)
        return klagebehandling
    }

    fun setGrunn(
        klagebehandling: Klagebehandling,
        vedtakId: UUID,
        grunn: Grunn?,
        utfoerendeSaksbehandlerIdent: String
    ): Klagebehandling {
        val event =
            klagebehandling.setGrunnInVedtak(grunn, utfoerendeSaksbehandlerIdent)
        applicationEventPublisher.publishEvent(event)
        return klagebehandling
    }

    fun setHjemler(
        klagebehandling: Klagebehandling,
        hjemler: Set<Hjemmel>,
        utfoerendeSaksbehandlerIdent: String
    ): Klagebehandling {
        val event =
            klagebehandling.setHjemlerInVedtak(hjemler, utfoerendeSaksbehandlerIdent)
        applicationEventPublisher.publishEvent(event)
        return klagebehandling
    }

    /*
    fun setJournalpostIdOgOpplastet(
        klagebehandling: Klagebehandling,
        vedtakId: UUID,
        journalpostId: String?,
        utfoerendeSaksbehandlerIdent: String
    ): Klagebehandling {
        val event =
            klagebehandling.setJournalpostIdOgOpplastetInVedtak(journalpostId, utfoerendeSaksbehandlerIdent)
        applicationEventPublisher.publishEvent(event)
        return klagebehandling
    }
     */

    private fun setMellomlagerIdOgOpplastet(
        klagebehandling: Klagebehandling,
        mellomlagerId: String?,
        utfoerendeSaksbehandlerIdent: String
    ): Klagebehandling {
        val event =
            klagebehandling.setMellomlagerIdOgOpplastetInVedtak(mellomlagerId, utfoerendeSaksbehandlerIdent)
        applicationEventPublisher.publishEvent(event)
        return klagebehandling
    }

    fun slettFilTilknyttetVedtak(
        klagebehandlingId: UUID,
        input: VedtakSlettVedleggInput,
        innloggetIdent: String
    ): Klagebehandling {
        val klagebehandling = klagebehandlingService.getKlagebehandlingForUpdate(
            klagebehandlingId,
            input.klagebehandlingVersjon
        )

        //TODO: Burde man sjekket tilgang til EnhetOgTema, ikke bare enhet?
        tilgangService.verifyInnloggetSaksbehandlersTilgangTilEnhet(klagebehandling.tildeling!!.enhet!!)

        val vedtak = klagebehandling.getVedtakOrException()

        if (vedtak.mellomlagerId == null) {
            return klagebehandling
        }
        slettMellomlagretDokument(
            klagebehandling,
            vedtak,
            innloggetIdent
        )
        return klagebehandling
    }

    fun knyttVedtaksFilTilVedtak(
        klagebehandlingId: UUID,
        input: VedtakVedleggInput,
        innloggetIdent: String
    ): Klagebehandling {
        val klagebehandling = klagebehandlingService.getKlagebehandlingForUpdate(
            klagebehandlingId,
            input.klagebehandlingVersjon
        )

        tilgangService.verifyInnloggetSaksbehandlersTilgangTilEnhet(klagebehandling.tildeling!!.enhet!!)
        attachmentValidator.validateAttachment(input.vedlegg)
        if (klagebehandling.avsluttetAvSaksbehandler != null) throw VedtakFinalizedException("Klagebehandlingen er avsluttet")

        val vedtak = klagebehandling.getVedtakOrException()
        if (vedtak.mellomlagerId != null) {
            slettMellomlagretDokument(
                klagebehandling,
                vedtak,
                innloggetIdent
            )
        }
        val mellomlagerId = fileApiService.uploadDocument(input.vedlegg)
        return setMellomlagerIdOgOpplastet(
            klagebehandling,
            mellomlagerId,
            innloggetIdent
        )
    }

    fun ferdigstillVedtak(
        klagebehandlingId: UUID,
        input: VedtakFullfoerInput,
        innloggetIdent: String
    ): Klagebehandling {
        val klagebehandling = klagebehandlingService.getKlagebehandlingForUpdate(
            klagebehandlingId = klagebehandlingId,
            klagebehandlingVersjon = input.klagebehandlingVersjon,
            ignoreCheckSkrivetilgang = true
        )
        verifyTilgangTilAaFerdigstilleVedtak(klagebehandling, innloggetIdent)
        if (klagebehandling.avsluttetAvSaksbehandler != null) throw VedtakFinalizedException("Klagebehandlingen er avsluttet")

        val vedtak = klagebehandling.getVedtakOrException()
        //Sjekker om fil er lastet opp til mellomlager
        if (vedtak.mellomlagerId == null) {
            throw VedtakNotFoundException("Vennligst last opp vedtaksdokument på nytt")
        }
        //Forretningsmessig krav før vedtak kan ferdigstilles
        if (vedtak.utfall == null) throw UtfallNotSetException("Utfall på vedtak ${vedtak.id} er ikke satt")

        //Her settes en markør som så brukes async i kallet klagebehandlingRepository.findByAvsluttetIsNullAndAvsluttetAvSaksbehandlerIsNotNull
        return klagebehandlingService.markerKlagebehandlingSomAvsluttetAvSaksbehandler(klagebehandling, innloggetIdent)
    }

    private fun verifyTilgangTilAaFerdigstilleVedtak(
        klagebehandling: Klagebehandling,
        innloggetIdent: String
    ) {
        if (klagebehandling.medunderskriver?.saksbehandlerident != innloggetIdent) {
            secureLogger.error(
                "{} prøvde å fullføre vedtak for klagebehandling {}, men er ikke medunderskriver.",
                innloggetIdent,
                klagebehandling.id
            )
            throw MissingTilgangException("Vedtak kan kun ferdigstilles av medunderskriver")
        }
    }

    fun slettMellomlagretDokument(
        klagebehandling: Klagebehandling,
        vedtak: Vedtak,
        utfoerendeSaksbehandlerIdent: String
    ): Klagebehandling {
        if (vedtak.mellomlagerId != null) {
            fileApiService.deleteDocument(vedtak.mellomlagerId!!)
        }
        return setMellomlagerIdOgOpplastet(
            klagebehandling,
            null,
            utfoerendeSaksbehandlerIdent
        )
    }
}
