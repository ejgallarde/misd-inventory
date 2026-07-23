package ph.gov.phlpost.inventory.misddashboard.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ph.gov.phlpost.inventory.misddashboard.model.AssetDocument;
import java.util.List;

public interface AssetDocumentRepository extends JpaRepository<AssetDocument, Integer> {

    // Finds all documents attached to a specific asset
    List<AssetDocument> findByReferenceTypeAndReferenceId(String referenceType, String referenceId);

    // Optional: Find a specific type of document for an asset (e.g., just the
    // "Appendix 71")
    List<AssetDocument> findByReferenceTypeAndReferenceIdAndDocumentCategory(
            String referenceType, String referenceId, String documentCategory);
}