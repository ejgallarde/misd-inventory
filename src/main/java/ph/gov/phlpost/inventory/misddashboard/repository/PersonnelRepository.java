package ph.gov.phlpost.inventory.misddashboard.repository;

import ph.gov.phlpost.inventory.misddashboard.model.Personnel;

import java.util.List;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PersonnelRepository extends JpaRepository<Personnel, String> {
    // Spring Data JPA + Cacheable
    @Cacheable("allPersonnel")
    List<Personnel> findAll();

    // For the AJAX search (Pagination is key here)
    Page<Personnel> findByLastNameContainingIgnoreCaseOrFirstNameContainingIgnoreCase(
            String lastName, String firstName, Pageable pageable);
}