package com.gescli.ProgrammationTravaux.repository;

import com.gescli.ProgrammationTravaux.entity.Document;
import com.gescli.ProgrammationTravaux.entity.TypeDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface DocumentRepository extends JpaRepository<Document, String> {

    @Query("SELECT d FROM Document d WHERE d.planning.id = :planningId AND d.typeDocument = :type")
    List<Document> findByPlanning_IdAndTypeDocument(@Param("planningId") String planningId,
                                                     @Param("type") TypeDocument type);
}