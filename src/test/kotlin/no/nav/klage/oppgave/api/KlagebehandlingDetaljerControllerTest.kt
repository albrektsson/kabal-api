package no.nav.klage.oppgave.api

import com.fasterxml.jackson.databind.ObjectMapper
import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import no.finn.unleash.Unleash
import no.nav.klage.oppgave.api.controller.KlagebehandlingDetaljerController
import no.nav.klage.oppgave.api.mapper.KlagebehandlingMapper
import no.nav.klage.oppgave.api.view.KlagebehandlingMedunderskriveridentInput
import no.nav.klage.oppgave.domain.klage.*
import no.nav.klage.oppgave.domain.kodeverk.*
import no.nav.klage.oppgave.repositories.InnloggetSaksbehandlerRepository
import no.nav.klage.oppgave.service.KlagebehandlingEditableFieldsFacade
import no.nav.klage.oppgave.service.KlagebehandlingService
import no.nav.klage.oppgave.util.AuditLogger
import no.nav.klage.oppgave.util.getLogger
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.put
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

@WebMvcTest(KlagebehandlingDetaljerController::class)
@ActiveProfiles("local")
class KlagebehandlingDetaljerControllerTest {

    @Autowired
    lateinit var mockMvc: MockMvc

    @Autowired
    lateinit var mapper: ObjectMapper

    @MockkBean
    lateinit var klagebehandlingService: KlagebehandlingService

    @MockkBean
    lateinit var klagebehandlingMapper: KlagebehandlingMapper

    @MockkBean
    lateinit var innloggetSaksbehandlerRepository: InnloggetSaksbehandlerRepository

    @MockkBean
    lateinit var auditLogger: AuditLogger

    @MockkBean
    lateinit var editableFieldsFacade: KlagebehandlingEditableFieldsFacade

    @MockkBean
    lateinit var unleash: Unleash

    companion object {
        @Suppress("JAVA_CLASS_ON_COMPANION")
        private val logger = getLogger(javaClass.enclosingClass)
    }

    private val klagebehandlingId = UUID.randomUUID()

    private val klagebehandling = Klagebehandling(
        versjon = 2L,
        klager = Klager(partId = PartId(type = PartIdType.PERSON, value = "23452354")),
        sakenGjelder = SakenGjelder(
            partId = PartId(type = PartIdType.PERSON, value = "23452354"),
            skalMottaKopi = false
        ),
        tema = Tema.OMS,
        type = Type.KLAGE,
        frist = LocalDate.now(),
        hjemler = mutableSetOf(
            Hjemmel.FTL_8_7
        ),
        created = LocalDateTime.now(),
        modified = LocalDateTime.now(),
        mottattKlageinstans = LocalDateTime.now(),
        kildesystem = Fagsystem.K9,
        mottakId = UUID.randomUUID(),
        vedtak = Vedtak(
            utfall = Utfall.AVVIST,
            hjemler = mutableSetOf(
                Hjemmel.FTL
            )
        ),
        medunderskriver = MedunderskriverTildeling(
            saksbehandlerident = "C78901",
            tidspunkt = LocalDateTime.now()
        )
    )

    @BeforeEach
    fun setup() {
        every { innloggetSaksbehandlerRepository.getInnloggetIdent() } returns "B54321"
    }

    @Test
    fun `putMedunderskriverident with correct input should return ok`() {
        every { klagebehandlingService.setMedunderskriverIdent(any(), any(), any(), any()) } returns klagebehandling

        val input = KlagebehandlingMedunderskriveridentInput(
            "A12345",
            1L
        )

        mockMvc.put("/klagebehandlinger/$klagebehandlingId/detaljer/medunderskriverident") {
            contentType = MediaType.APPLICATION_JSON
            content = mapper.writeValueAsString(input)
            accept = MediaType.APPLICATION_JSON
        }.andExpect {
            status { isOk() }
        }
    }

    @Test
    fun `putMedunderskriverident with incorrect input should return 400 error`() {
        mockMvc.put("/klagebehandlinger/$klagebehandlingId/detaljer/medunderskriverident") {
        }.andExpect {
            status { is4xxClientError() }
        }
    }

}