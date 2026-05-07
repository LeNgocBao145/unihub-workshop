package org.unihubworkshop.workshopservice.models;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "workshops")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Workshop {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @NotBlank(message = "Workshop name is required")
    @Size(min = 3, max = 255, message = "Workshop name must be between 3 and 255 characters")
    @Column(nullable = false)
    private String name;

    @NotNull(message = "Host ID is required")
    @Column(nullable = false)
    private UUID hostId;

    @NotBlank(message = "Room is required")
    @Size(min = 1, max = 100, message = "Room must be between 1 and 100 characters")
    @Column(nullable = false)
    private String room;

    @Size(max = 500, message = "Room map URL must be less than 500 characters")
    private String roomMap;

    @NotNull(message = "Total slots is required")
    @Positive(message = "Total slots must be positive")
    @Column(nullable = false)
    private Integer totalSlots;

    @NotNull(message = "Available slots is required")
    @PositiveOrZero(message = "Available slots must be zero or positive")
    @Column(nullable = false)
    private Integer availableSlots;

    @Size(max = 1000, message = "Description must be less than 1000 characters")
    @Column(columnDefinition = "TEXT")
    private String description;

    @DecimalMin(value = "0.0", message = "Price must be zero or positive")
    @Digits(integer = 10, fraction = 2, message = "Price must have at most 10 integer digits and 2 decimal places")
    private BigDecimal price = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private WorkshopType type = WorkshopType.FREE;

    private LocalDateTime startAt;

    private LocalDateTime endAt;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        if (availableSlots == null) {
            availableSlots = totalSlots;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        if (availableSlots > totalSlots) {
            availableSlots = totalSlots;
        }
    }

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "workshop_speakers",
            joinColumns = @JoinColumn(name = "workshop_id"),
            inverseJoinColumns = @JoinColumn(name = "speaker_id")
    )
    private List<Speaker> speakers;
}

