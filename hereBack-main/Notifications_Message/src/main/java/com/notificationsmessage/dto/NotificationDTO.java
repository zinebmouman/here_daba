package com.notificationsmessage.dto;
import lombok.*;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NotificationDTO {
    private Long id;
    private String type;
    private String message;
    private LocalDateTime dateEnvoi;
    private String vendeurId;  // ID Firebase du vendeur
    private Long produitId;
    private String status;

}