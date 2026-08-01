package ph.gov.phlpost.inventory.misddashboard.repository;

import ph.gov.phlpost.inventory.misddashboard.model.Personnel;

import java.util.List;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PersonnelRepository extends JpaRepository<Personnel, String> {
        // Spring Data JPA + Cacheable
        @Cacheable("allPersonnel")
        @EntityGraph(attributePaths = "baseLocation")
        List<Personnel> findAll();

        // For the AJAX search (Pagination is key here)
        Page<Personnel> findByLastNameContainingIgnoreCaseOrFirstNameContainingIgnoreCase(
                        String lastName, String firstName, Pageable pageable);

        @Query("""
                        SELECT personnel FROM Personnel personnel
                                    WHERE LOWER(TRIM(personnel.jobTitle)) IN (
                                                    LOWER(TRIM(:jobTitle)),
                                                    LOWER(CONCAT(TRIM(:jobTitle), ' I')),
                                                    LOWER(CONCAT(TRIM(:jobTitle), ' II')),
                                                    LOWER(CONCAT(TRIM(:jobTitle), ' III')))
                            AND (LOWER(personnel.lastName) LIKE LOWER(CONCAT('%', :query, '%'))
                                OR LOWER(personnel.firstName) LIKE LOWER(CONCAT('%', :query, '%')))
                        """)
        Page<Personnel> searchByJobTitle(@Param("jobTitle") String jobTitle, @Param("query") String query,
                        Pageable pageable);
}