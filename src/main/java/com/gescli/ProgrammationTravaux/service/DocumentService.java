package com.gescli.ProgrammationTravaux.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.gescli.ProgrammationTravaux.entity.Document;
import com.gescli.ProgrammationTravaux.repository.DocumentRepository;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class DocumentService {

    private final DocumentRepository documentRepository;

    @Transactional
    public Document createDocument(Document document) {
        return documentRepository.save(document);
    }

    @Transactional(readOnly = true)
    public List<Document> getAllDocuments() {
        return documentRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Document getDocumentById(String id) {
        return documentRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Document non trouvé avec l'ID : " + id));
    }

    @Transactional
    public Document updateDocument(String id, Document documentDetails) {
        Document document = documentRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Document non trouvé avec l'ID : " + id));
        document.setTypeDocument(documentDetails.getTypeDocument());
        document.setNumeroDocument(documentDetails.getNumeroDocument());
        document.setClient(documentDetails.getClient());
        document.setSite(documentDetails.getSite());
        document.setCodeProjet(documentDetails.getCodeProjet());
        document.setCodeClient(documentDetails.getCodeClient());
        document.setDateDocument(documentDetails.getDateDocument());
        return documentRepository.save(document);
    }

    @Transactional
    public void deleteDocument(String id) {
        if (!documentRepository.existsById(id)) {
            throw new EntityNotFoundException("Document non trouvé avec l'ID : " + id);
        }
        documentRepository.deleteById(id);
    }
}
