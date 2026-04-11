package com.gescli.ProgrammationTravaux.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.gescli.ProgrammationTravaux.entity.Operation;
import com.gescli.ProgrammationTravaux.repository.OperationRepository;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class OperationService {

    private final OperationRepository operationRepository;

    @Transactional
    public Operation save(Operation operation) {
        return operationRepository.save(operation);
    }

    @Transactional(readOnly = true)
    public List<Operation> getAll() {
        return operationRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Operation getById(String id) {
        return operationRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Operation non trouvée avec l'ID : " + id));
    }

    @Transactional
    public void delete(String id) {
        operationRepository.deleteById(id);
    }
}
