package org.unihubworkshop.workshopservice.repositories;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.unihubworkshop.workshopservice.models.Registration;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface RegistrationRepository extends JpaRepository<Registration, UUID> {
    Page<Registration> findByUserId(UUID userId, Pageable pageable);
    
    Optional<Registration> findByWorkshopIdAndUserId(UUID workshopId, UUID userId);
}
