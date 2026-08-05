package ph.gov.phlpost.inventory.misddashboard.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ph.gov.phlpost.inventory.misddashboard.model.Document;

import java.util.List;

public interface DocumentRepository extends JpaRepository<Document, Integer> {

    List<Document> findByReferenceTypeAndReferenceId(String referenceType, String referenceId);
}