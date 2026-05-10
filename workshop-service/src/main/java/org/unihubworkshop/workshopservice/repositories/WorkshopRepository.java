package org.unihubworkshop.workshopservice.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.unihubworkshop.workshopservice.models.Workshop;

import java.util.List;
import java.util.UUID;

@Repository
public interface WorkshopRepository extends JpaRepository<Workshop, UUID>, JpaSpecificationExecutor<Workshop>  {

    List<Workshop> findByHostId(UUID hostId);

    List<Workshop> findByRoom(String room);


    List<Workshop> findByNameContainingIgnoreCase(String name);

    @Query("SELECT s.name FROM Workshop w JOIN w.speakers s WHERE w.id = :workshopId")
    List<String> findSpeakerNamesByWorkshopId(UUID workshopId);

}
