package com.gescli.ProgrammationTravaux.repository;

import com.gescli.ProgrammationTravaux.entity.AppNotification;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface AppNotificationRepository extends JpaRepository<AppNotification, String> {
    List<AppNotification> findTop50ByAgentIdOrderByDateCreationDesc(String agentId);
    long countByAgentIdAndDateLectureIsNull(String agentId);
    List<AppNotification> findByAgentIdAndDateLectureIsNull(String agentId);
}
