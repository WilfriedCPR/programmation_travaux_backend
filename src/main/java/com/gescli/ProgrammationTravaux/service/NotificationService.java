package com.gescli.ProgrammationTravaux.service;

import com.gescli.ProgrammationTravaux.dto.NotificationDTO;
import com.gescli.ProgrammationTravaux.entity.AppNotification;
import com.gescli.ProgrammationTravaux.repository.AppNotificationRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationService {
    private final AppNotificationRepository repository;
    private final VapidPushService pushService;

    @Transactional
    public void notifyAgent(String agentId, String titre, String message, String type, String lien) {
        if (agentId == null || agentId.isBlank()) return;
        AppNotification n = new AppNotification();
        n.setAgentId(agentId);
        n.setTitre(titre);
        n.setMessage(message);
        n.setType(type);
        n.setLien(lien);
        repository.save(n);
        // Le push sans payload provoque ensuite une lecture de /latest par le service worker.
        // Il doit donc être envoyé après le commit afin que la nouvelle notification soit visible.
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override public void afterCommit() { pushService.notifyAgent(agentId); }
            });
        } else {
            pushService.notifyAgent(agentId);
        }
    }

    @Transactional(readOnly = true)
    public List<NotificationDTO> list(String agentId) {
        return repository.findTop50ByAgentIdOrderByDateCreationDesc(agentId).stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public NotificationDTO latest(String agentId) {
        return repository.findTopByAgentIdOrderByDateCreationDesc(agentId)
                .map(this::toDto)
                .orElse(null);
    }

    @Transactional(readOnly = true)
    public long unreadCount(String agentId) {
        return repository.countByAgentIdAndDateLectureIsNull(agentId);
    }

    @Transactional
    public void markRead(String agentId, String id) {
        AppNotification n = repository.findById(id).orElseThrow(() -> new EntityNotFoundException("Notification introuvable"));
        if (!agentId.equals(n.getAgentId())) throw new IllegalArgumentException("Notification non autorisée");
        if (n.getDateLecture() == null) {
            n.setDateLecture(LocalDateTime.now());
            repository.save(n);
        }
    }

    @Transactional
    public void markAllRead(String agentId) {
        var unread = repository.findByAgentIdAndDateLectureIsNull(agentId);
        LocalDateTime now = LocalDateTime.now();
        unread.forEach(n -> n.setDateLecture(now));
        repository.saveAll(unread);
    }

    private NotificationDTO toDto(AppNotification e) {
        NotificationDTO dto = new NotificationDTO();
        dto.setId(e.getId());
        dto.setTitre(e.getTitre());
        dto.setMessage(e.getMessage());
        dto.setType(e.getType());
        dto.setLien(e.getLien());
        dto.setDateCreation(e.getDateCreation());
        dto.setLue(e.getDateLecture() != null);
        return dto;
    }
}
