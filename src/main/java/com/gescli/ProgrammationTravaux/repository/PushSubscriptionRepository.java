package com.gescli.ProgrammationTravaux.repository;

import com.gescli.ProgrammationTravaux.entity.PushSubscription;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface PushSubscriptionRepository extends JpaRepository<PushSubscription, String> {
    Optional<PushSubscription> findByEndpoint(String endpoint);
    List<PushSubscription> findByAgentIdAndActifTrue(String agentId);
}
