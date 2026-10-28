package ph.gov.phlpost.inventory.misddashboard.service;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import ph.gov.phlpost.inventory.misddashboard.model.Personnel;
import ph.gov.phlpost.inventory.misddashboard.repository.PersonnelRepository;
import ph.gov.phlpost.inventory.misddashboard.util.TextUtils;

/**
 * Decides who is accountable when property is put in someone's hands.
 *
 * <p>
 * Government property accountability (PAR/ICS) can only be issued to eligible
 * personnel, by default regular employees (asset.accountability.
 * eligible-employment-statuses). Anyone may be the person who uses an asset or
 * drives a vehicle, but when that person is not eligible (Job Order, Contract
 * of Service and so on) the accountability rests with an eligible employee,
 * usually their manager. This class applies that rule in one place for IT
 * assets, fleet vehicles and property custodians.
 */
@Service
public class AccountabilityService {

    /** How far up the manager chain to look before giving up (guards against cycles). */
    private static final int MAX_MANAGER_LEVELS = 10;

    private final PersonnelRepository personnelRepo;
    private final Set<String> eligibleStatuses;

    public AccountabilityService(PersonnelRepository personnelRepo,
            @Value("${asset.accountability.eligible-employment-statuses:Regular}") String eligibleStatusesCsv) {
        this.personnelRepo = personnelRepo;
        this.eligibleStatuses = TextUtils.splitCsv(eligibleStatusesCsv).stream()
                .map(status -> status.toLowerCase(Locale.ROOT))
                .collect(Collectors.toUnmodifiableSet());
    }

    /** How the accountable owner was chosen, for the confirmation message and audit note. */
    public enum Basis {
        /** The assigned person is eligible and accountable themselves. */
        SELF,
        /** The assigned person is not eligible; their (or an upper) manager is. */
        MANAGER,
        /** Chosen explicitly at assignment. */
        CHOSEN
    }

    public record Accountability(String assignedId, String accountableId, Basis basis) {
    }

    /**
     * Resolves the accountable owner for property assigned to {@code assignedId}.
     *
     * @param chosenAccountableId optional explicit choice; must be eligible
     * @throws IllegalArgumentException with a user-facing message when nobody
     *                                  eligible can be found
     */
    public Accountability resolve(String assignedId, String chosenAccountableId) {
        Personnel assigned = find(assignedId, "Assigned employee");

        if (!TextUtils.isBlank(chosenAccountableId)) {
            Personnel chosen = find(chosenAccountableId, "Accountable owner");
            requireEligible(chosen);
            return new Accountability(assigned.getEmployeeID(), chosen.getEmployeeID(),
                    chosen.getEmployeeID().equals(assigned.getEmployeeID()) ? Basis.SELF : Basis.CHOSEN);
        }

        if (isEligible(assigned)) {
            return new Accountability(assigned.getEmployeeID(), assigned.getEmployeeID(), Basis.SELF);
        }

        Set<String> visited = new HashSet<>();
        Personnel current = assigned;
        for (int level = 0; level < MAX_MANAGER_LEVELS; level++) {
            String managerId = current.getManagerID();
            if (TextUtils.isBlank(managerId) || !visited.add(managerId)) {
                break;
            }
            Personnel manager = personnelRepo.findById(managerId).orElse(null);
            if (manager == null) {
                break;
            }
            if (isEligible(manager)) {
                return new Accountability(assigned.getEmployeeID(), manager.getEmployeeID(), Basis.MANAGER);
            }
            current = manager;
        }

        throw new IllegalArgumentException(displayName(assigned) + " is " + statusOf(assigned)
                + " and cannot be the accountable owner, and no eligible manager is on record."
                + " Choose an accountable owner.");
    }

    /** Throws a user-facing IllegalArgumentException unless the employee may be accountable. */
    public void requireEligible(String employeeId, String roleDescription) {
        Personnel person = find(employeeId, roleDescription);
        requireEligible(person);
    }

    /** Lower-cased eligible statuses, for queries. */
    public Set<String> eligibleStatuses() {
        return eligibleStatuses;
    }

    public boolean isEligible(Personnel person) {
        return person != null && eligibleStatuses.contains(statusOf(person).toLowerCase(Locale.ROOT));
    }

    private void requireEligible(Personnel person) {
        if (!isEligible(person)) {
            throw new IllegalArgumentException(displayName(person) + " is " + statusOf(person)
                    + " and cannot be the accountable owner.");
        }
    }

    private Personnel find(String employeeId, String roleDescription) {
        if (TextUtils.isBlank(employeeId)) {
            throw new IllegalArgumentException(roleDescription + " is required.");
        }
        return personnelRepo.findById(employeeId.trim())
                .orElseThrow(() -> new IllegalArgumentException(roleDescription + " was not found."));
    }

    /** Rows that predate the EmploymentStatus column read as the migration default. */
    private static String statusOf(Personnel person) {
        return TextUtils.isBlank(person.getEmploymentStatus()) ? "Regular" : person.getEmploymentStatus().trim();
    }

    private static String displayName(Personnel person) {
        return TextUtils.normalizeBlank(person.getFirstName()) + " " + TextUtils.normalizeBlank(person.getLastName());
    }
}
