package com.gescli.ProgrammationTravaux.repository;

import com.gescli.ProgrammationTravaux.entity.Operation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OperationRepository extends JpaRepository<Operation, String> {
    List<Operation> findByDemandeMaterielId(String demandeMaterielId);
    List<Operation> findByAgentId(String agentId);
}
