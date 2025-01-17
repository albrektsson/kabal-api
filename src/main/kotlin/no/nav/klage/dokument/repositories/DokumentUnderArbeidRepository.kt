package no.nav.klage.dokument.repositories

import no.nav.klage.dokument.domain.dokumenterunderarbeid.DokumentId
import no.nav.klage.dokument.domain.dokumenterunderarbeid.DokumentUnderArbeid
import no.nav.klage.dokument.domain.dokumenterunderarbeid.HovedDokument
import no.nav.klage.dokument.domain.dokumenterunderarbeid.PersistentDokumentId
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.transaction.annotation.Transactional
import java.util.*

@Transactional
interface HovedDokumentRepository : JpaRepository<HovedDokument, DokumentId> {

    fun findByBehandlingIdOrderByCreated(behandlingId: UUID): SortedSet<HovedDokument>

    fun findByPersistentDokumentId(persistentDokumentId: PersistentDokumentId): HovedDokument?

    fun findByPersistentDokumentIdOrVedleggPersistentDokumentId(
        persistentDokumentId: PersistentDokumentId,
        vedleggPersistentDokumentId: PersistentDokumentId
    ): HovedDokument?

    fun findByVedleggPersistentDokumentId(persistentDokumentId: PersistentDokumentId): HovedDokument?

    fun findByMarkertFerdigNotNullAndFerdigstiltNull(): List<HovedDokument>
}

fun HovedDokumentRepository.findByPersistentDokumentIdOrVedleggPersistentDokumentId(persistentDokumentId: PersistentDokumentId): HovedDokument? =
    this.findByPersistentDokumentIdOrVedleggPersistentDokumentId(persistentDokumentId, persistentDokumentId)

fun HovedDokumentRepository.findDokumentUnderArbeidByPersistentDokumentIdOrVedleggPersistentDokumentId(
    persistentDokumentId: PersistentDokumentId
): DokumentUnderArbeid? =
    this.findByPersistentDokumentIdOrVedleggPersistentDokumentId(persistentDokumentId, persistentDokumentId)
        ?.findDokumentUnderArbeidByPersistentDokumentId(persistentDokumentId)

fun HovedDokumentRepository.getDokumentUnderArbeidByPersistentDokumentIdOrVedleggPersistentDokumentId(
    persistentDokumentId: PersistentDokumentId
): DokumentUnderArbeid =
    findDokumentUnderArbeidByPersistentDokumentIdOrVedleggPersistentDokumentId(persistentDokumentId)!!
