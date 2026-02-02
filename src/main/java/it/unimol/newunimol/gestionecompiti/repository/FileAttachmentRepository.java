package it.unimol.newunimol.gestionecompiti.repository;

import it.unimol.newunimol.gestionecompiti.model.FileAttachment;
import it.unimol.newunimol.gestionecompiti.model.FileEntityType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FileAttachmentRepository extends JpaRepository<FileAttachment, String> {
    
    List<FileAttachment> findByEntityTypeAndEntityId(FileEntityType entityType, String entityId);
   
    List<FileAttachment> findByUploadedBy(String userId);
    
    void deleteByEntityTypeAndEntityId(FileEntityType entityType, String entityId);
}
