package com.camping.duneinsolite.model;

import com.camping.duneinsolite.model.enums.ReservationStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "reservations")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Reservation {

    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(name = "reservation_id", updatable = false, nullable = false)
    private UUID reservationId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "source")
    private String source;

    @Column(name = "check_in_date", nullable = false)
    private LocalDate checkInDate;

    @Column(name = "check_out_date", nullable = false)
    private LocalDate checkOutDate;

    @Column(name = "group_name")
    private String groupName;

    @Column(name = "group_leader_name")
    private String groupLeaderName;

    @Column(name = "number_of_adults", nullable = false)
    @Builder.Default
    private Integer numberOfAdults = 0;

    @Column(name = "number_of_children", nullable = false)
    @Builder.Default
    private Integer numberOfChildren = 0;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private ReservationStatus status = ReservationStatus.PENDING;

    @Column(name = "rejection_reason", columnDefinition = "TEXT")
    private String rejectionReason;

    @Column(name = "total_amount")
    private Double totalAmount;

    @Column(name = "currency", length = 3)
    @Builder.Default
    private String currency = "USD";

    @Column(name = "promo_code")
    private String promoCode;
    @Column(name = "total_extras_amount")
    @Builder.Default
    private Double totalExtrasAmount = 0.0;

    @Column(name = "demande_special", columnDefinition = "TEXT")
    private String demandeSpecial;

    // ──────────────────────────────────────────────
    // RELATIONS — ReservationTourTypes (One-to-Many)
    // replaces the old @ManyToMany with TourType
    // ──────────────────────────────────────────────

    @OneToMany(mappedBy = "reservation", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ReservationTourType> tourTypes = new ArrayList<>();

    // ──────────────────────────────────────────────
    // RELATIONS — Participants (One-to-Many)
    // ──────────────────────────────────────────────

    @OneToMany(mappedBy = "reservation", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Participant> participants = new ArrayList<>();

    // ──────────────────────────────────────────────
    // RELATIONS — ReservationExtras (One-to-Many)
    // ──────────────────────────────────────────────

    @OneToMany(mappedBy = "reservation", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ReservationExtra> extras = new ArrayList<>();

    // ──────────────────────────────────────────────
    // RELATIONS — Invoices (One-to-Many)
    // ──────────────────────────────────────────────

    @OneToMany(mappedBy = "reservation", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Invoice> invoices = new ArrayList<>();

    // ──────────────────────────────────────────────
    // RELATIONS — Transactions (One-to-Many)
    // ──────────────────────────────────────────────

    @OneToMany(mappedBy = "reservation", cascade = CascadeType.ALL, orphanRemoval = false)
    @Builder.Default
    private List<Transaction> transactions = new ArrayList<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    // ──────────────────────────────────────────────
    // HELPER METHODS
    // ──────────────────────────────────────────────

    public void addTourType(ReservationTourType tourType) {
        tourTypes.add(tourType);
        tourType.setReservation(this);
    }

    public void removeTourType(ReservationTourType tourType) {
        tourTypes.remove(tourType);
        tourType.setReservation(null);
    }

    public void addParticipant(Participant participant) {
        participants.add(participant);
        participant.setReservation(this);
    }

    public void removeParticipant(Participant participant) {
        participants.remove(participant);
        participant.setReservation(null);
    }

    public void addExtra(ReservationExtra extra) {
        extras.add(extra);
        extra.setReservation(this);
    }

    public void removeExtra(ReservationExtra extra) {
        extras.remove(extra);
        extra.setReservation(null);
    }

    public void addInvoice(Invoice invoice) {
        invoices.add(invoice);
        invoice.setReservation(this);
    }

    public void addTransaction(Transaction transaction) {
        transactions.add(transaction);
        transaction.setReservation(this);
    }

    public Double calculateTotalExtrasAmount() {
        return extras.stream()
                .mapToDouble(ReservationExtra::getTotalPrice)
                .sum();
    }

    public Double calculateTotalTourTypesAmount() {
        return tourTypes.stream()
                .mapToDouble(ReservationTourType::getTotalPrice)
                .sum();
    }
}