package no.nav.klage.oppgave.service

import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import no.nav.klage.oppgave.config.ElasticsearchServiceConfiguration
import no.nav.klage.oppgave.domain.KlagebehandlingerSearchCriteria
import no.nav.klage.oppgave.domain.elasticsearch.EsKlagebehandling
import no.nav.klage.oppgave.domain.elasticsearch.EsKlagebehandling.Status.IKKE_TILDELT
import no.nav.klage.oppgave.domain.kodeverk.Tema
import no.nav.klage.oppgave.domain.kodeverk.Type
import no.nav.klage.oppgave.repositories.EsKlagebehandlingRepository
import no.nav.klage.oppgave.repositories.InnloggetSaksbehandlerRepository
import org.assertj.core.api.Assertions.assertThat
import org.elasticsearch.index.query.QueryBuilders
import org.junit.jupiter.api.MethodOrderer
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestMethodOrder
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.ImportAutoConfiguration
import org.springframework.boot.autoconfigure.data.elasticsearch.ElasticsearchDataAutoConfiguration
import org.springframework.boot.autoconfigure.elasticsearch.ElasticsearchRestClientAutoConfiguration
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.elasticsearch.core.ElasticsearchRestTemplate
import org.springframework.data.elasticsearch.core.SearchHits
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder
import org.springframework.data.elasticsearch.core.query.Query
import org.springframework.test.context.ActiveProfiles
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import java.time.LocalDate
import java.time.LocalDateTime


@ActiveProfiles("local")
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
@Testcontainers
@SpringBootTest(classes = [ElasticsearchServiceConfiguration::class])
@ImportAutoConfiguration(
    ElasticsearchRestClientAutoConfiguration::class,
    ElasticsearchDataAutoConfiguration::class
)
class FortroligElasticsearchServiceTest {

    companion object {
        @Container
        @JvmField
        val esContainer: TestElasticsearchContainer = TestElasticsearchContainer.instance
    }

    @MockkBean(relaxed = true)
    lateinit var innloggetSaksbehandlerRepository: InnloggetSaksbehandlerRepository

    @Autowired
    lateinit var service: ElasticsearchService

    @Autowired
    lateinit var esTemplate: ElasticsearchRestTemplate

    @Autowired
    lateinit var repo: EsKlagebehandlingRepository

    @Test
    @Order(1)
    fun `es is running`() {
        assertThat(esContainer.isRunning).isTrue
        service.recreateIndex()
    }

    @Test
    @Order(2)
    fun `index has been created by service`() {

        val indexOps = esTemplate.indexOps(IndexCoordinates.of("klagebehandling"))
        assertThat(indexOps.exists()).isTrue()
    }

    @Test
    @Order(3)
    fun `lagrer fem oppgaver for senere tester`() {

        val klagebehandling1 = EsKlagebehandling(
            id = "1001L",
            versjon = 1L,
            tildeltEnhet = "4219",
            tema = Tema.OMS.id,
            type = Type.KLAGE.id,
            tildeltSaksbehandlerident = null,
            innsendt = LocalDate.of(2019, 10, 1),
            mottattFoersteinstans = LocalDate.of(2019, 11, 1),
            mottattKlageinstans = LocalDateTime.of(2019, 12, 1, 0, 0),
            frist = LocalDate.of(2020, 12, 1),
            hjemler = listOf(),
            created = LocalDateTime.now(),
            modified = LocalDateTime.now(),
            kilde = "K9",
            temaNavn = Tema.OMS.name,
            typeNavn = Type.KLAGE.name,
            status = IKKE_TILDELT,
            sakenGjelderFnr = "123",
        )
        val klagebehandling2 =
            EsKlagebehandling(
                id = "1002L",
                versjon = 1L,
                tildeltEnhet = "4219",
                tema = Tema.OMS.id,
                type = Type.KLAGE.id,
                tildeltSaksbehandlerident = null,
                innsendt = LocalDate.of(2018, 10, 1),
                mottattFoersteinstans = LocalDate.of(2018, 11, 1),
                mottattKlageinstans = LocalDateTime.of(2018, 12, 1, 0, 0),
                frist = LocalDate.of(2019, 12, 1),
                hjemler = listOf(),
                created = LocalDateTime.now(),
                modified = LocalDateTime.now(),
                kilde = "K9",
                temaNavn = Tema.OMS.name,
                typeNavn = Type.KLAGE.name,
                status = IKKE_TILDELT,
                sakenGjelderFnr = "123",
                fortrolig = true
            )
        val klagebehandling3 = EsKlagebehandling(
            id = "1003L",
            versjon = 1L,
            tildeltEnhet = "4219",
            tema = Tema.OMS.id,
            type = Type.KLAGE.id,
            tildeltSaksbehandlerident = null,
            innsendt = LocalDate.of(2019, 10, 1),
            mottattFoersteinstans = LocalDate.of(2019, 11, 1),
            mottattKlageinstans = LocalDateTime.of(2019, 12, 1, 0, 0),
            frist = LocalDate.of(2020, 12, 1),
            hjemler = listOf(),
            created = LocalDateTime.now(),
            modified = LocalDateTime.now(),
            kilde = "K9",
            temaNavn = Tema.OMS.name,
            typeNavn = Type.KLAGE.name,
            status = IKKE_TILDELT,
            sakenGjelderFnr = "123",
            strengtFortrolig = true
        )
        val klagebehandling4 =
            EsKlagebehandling(
                id = "1004L",
                versjon = 1L,
                tildeltEnhet = "4219",
                tema = Tema.OMS.id,
                type = Type.KLAGE.id,
                tildeltSaksbehandlerident = null,
                innsendt = LocalDate.of(2018, 10, 1),
                mottattFoersteinstans = LocalDate.of(2018, 11, 1),
                mottattKlageinstans = LocalDateTime.of(2018, 12, 1, 0, 0),
                frist = LocalDate.of(2019, 12, 1),
                hjemler = listOf(),
                created = LocalDateTime.now(),
                modified = LocalDateTime.now(),
                kilde = "K9",
                temaNavn = Tema.OMS.name,
                typeNavn = Type.KLAGE.name,
                status = IKKE_TILDELT,
                sakenGjelderFnr = "123",
                egenAnsatt = true
            )
        val klagebehandling5 =
            EsKlagebehandling(
                id = "1005L",
                versjon = 1L,
                tildeltEnhet = "4219",
                tema = Tema.OMS.id,
                type = Type.KLAGE.id,
                tildeltSaksbehandlerident = null,
                innsendt = LocalDate.of(2018, 10, 1),
                mottattFoersteinstans = LocalDate.of(2018, 11, 1),
                mottattKlageinstans = LocalDateTime.of(2018, 12, 1, 0, 0),
                frist = LocalDate.of(2019, 12, 1),
                hjemler = listOf(),
                created = LocalDateTime.now(),
                modified = LocalDateTime.now(),
                kilde = "K9",
                temaNavn = Tema.OMS.name,
                typeNavn = Type.KLAGE.name,
                status = IKKE_TILDELT,
                sakenGjelderFnr = "123",
                egenAnsatt = true,
                fortrolig = true
            )
        val klagebehandling6 =
            EsKlagebehandling(
                id = "1006L",
                versjon = 1L,
                tildeltEnhet = "4219",
                tema = Tema.OMS.id,
                type = Type.KLAGE.id,
                tildeltSaksbehandlerident = null,
                innsendt = LocalDate.of(2018, 10, 1),
                mottattFoersteinstans = LocalDate.of(2018, 11, 1),
                mottattKlageinstans = LocalDateTime.of(2018, 12, 1, 0, 0),
                frist = LocalDate.of(2019, 12, 1),
                hjemler = listOf(),
                created = LocalDateTime.now(),
                modified = LocalDateTime.now(),
                kilde = "K9",
                temaNavn = Tema.OMS.name,
                typeNavn = Type.KLAGE.name,
                status = IKKE_TILDELT,
                sakenGjelderFnr = "123",
                egenAnsatt = true,
                strengtFortrolig = true
            )
        repo.save(klagebehandling1)
        repo.save(klagebehandling2)
        repo.save(klagebehandling3)
        repo.save(klagebehandling4)
        repo.save(klagebehandling5)
        repo.save(klagebehandling6)

        val query: Query = NativeSearchQueryBuilder()
            .withQuery(QueryBuilders.matchAllQuery())
            .build()
        val searchHits: SearchHits<EsKlagebehandling> = esTemplate.search(query, EsKlagebehandling::class.java)
        assertThat(searchHits.totalHits).isEqualTo(6L)
    }

    @Test
    @Order(4)
    fun `Saksbehandler with no special rights will only see normal klagebehandlinger`() {
        every { innloggetSaksbehandlerRepository.kanBehandleEgenAnsatt() } returns false
        every { innloggetSaksbehandlerRepository.kanBehandleFortrolig() } returns false
        every { innloggetSaksbehandlerRepository.kanBehandleStrengtFortrolig() } returns false
        val klagebehandlinger: List<EsKlagebehandling> =
            service.findByCriteria(
                KlagebehandlingerSearchCriteria(
                    temaer = listOf(Tema.OMS),
                    offset = 0,
                    limit = 10
                )
            ).searchHits.map { it.content }
        assertThat(klagebehandlinger.size).isEqualTo(1L)
        assertThat(klagebehandlinger.map { it.id }).containsExactlyInAnyOrder("1001L")
    }

    @Test
    @Order(5)
    fun `Saksbehandler with egen ansatt rights will only see normal klagebehandlinger and those for egen ansatte, but not egen ansatte that are fortrolig or strengt fortrolig`() {
        every { innloggetSaksbehandlerRepository.kanBehandleEgenAnsatt() } returns true
        every { innloggetSaksbehandlerRepository.kanBehandleFortrolig() } returns false
        every { innloggetSaksbehandlerRepository.kanBehandleStrengtFortrolig() } returns false
        val klagebehandlinger: List<EsKlagebehandling> =
            service.findByCriteria(
                KlagebehandlingerSearchCriteria(
                    temaer = listOf(Tema.OMS),
                    offset = 0,
                    limit = 10
                )
            ).searchHits.map { it.content }
        assertThat(klagebehandlinger.size).isEqualTo(2L)
        assertThat(klagebehandlinger.map { it.id }).containsExactlyInAnyOrder("1001L", "1004L")
    }

    @Test
    @Order(6)
    fun `Saksbehandler with fortrolig rights will see normal klagebehandlinger and fortrolige klagebehandlinger, including the combo fortrolig and egen ansatt`() {
        every { innloggetSaksbehandlerRepository.kanBehandleEgenAnsatt() } returns false
        every { innloggetSaksbehandlerRepository.kanBehandleFortrolig() } returns true
        every { innloggetSaksbehandlerRepository.kanBehandleStrengtFortrolig() } returns false
        val klagebehandlinger: List<EsKlagebehandling> =
            service.findByCriteria(
                KlagebehandlingerSearchCriteria(
                    temaer = listOf(Tema.OMS),
                    offset = 0,
                    limit = 10
                )
            ).searchHits.map { it.content }
        assertThat(klagebehandlinger.size).isEqualTo(3L)
        assertThat(klagebehandlinger.map { it.id }).containsExactlyInAnyOrder("1001L", "1002L", "1005L")
    }

    @Test
    @Order(7)
    fun `Saksbehandler with strengt fortrolig rights will only see strengt fortrolige klagebehandlinger, and those that are strengt fortrolige and egen ansatte`() {
        every { innloggetSaksbehandlerRepository.kanBehandleEgenAnsatt() } returns false
        every { innloggetSaksbehandlerRepository.kanBehandleFortrolig() } returns false
        every { innloggetSaksbehandlerRepository.kanBehandleStrengtFortrolig() } returns true
        val klagebehandlinger: List<EsKlagebehandling> =
            service.findByCriteria(
                KlagebehandlingerSearchCriteria(
                    temaer = listOf(Tema.OMS),
                    offset = 0,
                    limit = 10
                )
            ).searchHits.map { it.content }
        assertThat(klagebehandlinger.size).isEqualTo(2L)
        assertThat(klagebehandlinger.map { it.id }).containsExactlyInAnyOrder("1003L", "1006L")
    }

}