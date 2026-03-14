package com.camping.duneinsolite.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;
import java.util.UUID;

@Entity
@Table(name = "extras")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Extra {

    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(name = "extra_id", updatable = false, nullable = false)
    private UUID extraId;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    // For timed extras e.g. "1h30", "2h"
    @Column(name = "duration")
    private String duration;

    @Column(name = "unit_price", nullable = false)
    private Double unitPrice;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;
}