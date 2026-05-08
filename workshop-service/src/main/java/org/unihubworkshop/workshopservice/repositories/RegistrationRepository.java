package org.unihubworkshop.workshopservice.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.unihubworkshop.workshopservice.models.Registration;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface RegistrationRepository extends JpaRepository<Registration, UUID> {
    Optional<Registration> findByUserIdAndWorkshop_Id(
            UUID userId,
            UUID workshopId
    );
}

