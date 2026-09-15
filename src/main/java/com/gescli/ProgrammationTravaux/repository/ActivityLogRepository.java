package com.gescli.ProgrammationTravaux.repository;

import com.gescli.ProgrammationTravaux.entity.ActivityLog;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.time.LocalDateTime;

public interface ActivityLogRepository extends JpaRepository<ActivityLog, String> {
    List<ActivityLog> findTop100ByOrderByDateCreationDesc();
    List<ActivityLog> findTop50ByAgentIdOrderByDateCreationDesc(String agentId);
    Optional<ActivityLog> findFirstByAgentIdAndDevisIdAndTypeInAndDateCreationBetweenOrderByDateCreationAsc(
            String agentId, String devisId, List<String> types, LocalDateTime start, LocalDateTime end);
}
