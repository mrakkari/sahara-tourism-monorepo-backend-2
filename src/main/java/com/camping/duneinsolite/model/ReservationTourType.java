package com.camping.duneinsolite.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;
import java.util.UUID;

@Entity
@Table(name = "reservation_tour_types")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ReservationTourType {

    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(name = "reservation_tour_type_id", updatable = false, nullable = false)
    private UUID reservationTourTypeId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reservation_id", nullable = false)
    private Reservation reservation;

    // ── Snapshots from TourType at booking time ──────────
    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "duration")
    private String duration;
    @Column(name = "number_of_nights")
    private Integer numberOfNights;

    // Price snapshotted based on user role at booking time
    @Column(name = "adult_price", nullable = false)
    private Double adultPrice;

    @Column(name = "child_price", nullable = false)
    private Double childPrice;

    @Column(name = "number_of_adults", nullable = false)
    private Integer numberOfAdults;

    @Column(name = "number_of_children", nullable = false)
    private Integer numberOfChildren;

    // Computed — not stored in DB
    @Transient
    public Double getTotalPrice() {
        int nights = numberOfNights != null && numberOfNights > 0 ? numberOfNights : 1;
        return ((numberOfAdults * adultPrice) + (numberOfChildren * childPrice)) * nights;
    }
}