package no.nav.klage.oppgave.clients.axsys

import no.nav.klage.kodeverk.klageenheter
import no.nav.klage.oppgave.domain.saksbehandler.Enhet
import no.nav.klage.oppgave.domain.saksbehandler.SaksbehandlerIdent
import no.nav.klage.oppgave.gateway.AxsysGateway
import org.springframework.stereotype.Service

@Service
@Deprecated("Vi henter data fra Azure for dette nå")
class DefaultAxsysGateway(
    private val axsysClient: AxsysClient,
    private val tilgangerMapper: TilgangerMapper
) : AxsysGateway {

    override fun getEnheterForSaksbehandler(ident: String): List<Enhet> =
        tilgangerMapper.mapTilgangerToEnheter(axsysClient.getTilgangerForSaksbehandler(ident))
            .filter { enhet -> enhet.enhetId in klageenheter.map { it.navn } }

    override fun getSaksbehandlereIEnhet(enhetNr: String): List<SaksbehandlerIdent> {
        return axsysClient.getSaksbehandlereIEnhet(enhetNr).map { SaksbehandlerIdent(it.appIdent) }
    }
}