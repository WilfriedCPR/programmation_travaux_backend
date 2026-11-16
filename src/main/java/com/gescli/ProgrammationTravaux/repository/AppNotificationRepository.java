package com.gescli.ProgrammationTravaux.repository;

import com.gescli.ProgrammationTravaux.entity.AppNotification;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface AppNotificationRepository extends JpaRepository<AppNotification, String> {
    List<AppNotification> findTop50ByAgentIdOrderByDateCreationDesc(String agentId);
    Optional<AppNotification> findTopByAgentIdOrderByDateCreationDesc(String agentId);
    long countByAgentIdAndDateLectureIsNull(String agentId);
    List<AppNotification> findByAgentIdAndDateLectureIsNull(String agentId);
}
