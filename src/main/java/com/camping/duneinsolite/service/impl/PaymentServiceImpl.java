package com.camping.duneinsolite.service.impl;

import com.camping.duneinsolite.config.RabbitMQConfig;
import com.camping.duneinsolite.dto.message.NotificationMessage;
import com.camping.duneinsolite.dto.request.PaymentRequest;
import com.camping.duneinsolite.dto.response.PaymentResponse;
import com.camping.duneinsolite.dto.response.PaymentSummary;
import com.camping.duneinsolite.dto.response.TransactionResponse;
import com.camping.duneinsolite.mapper.TransactionMapper;
import com.camping.duneinsolite.model.Reservation;
import com.camping.duneinsolite.model.Transaction;
import com.camping.duneinsolite.model.enums.NotificationType;
import com.camping.duneinsolite.model.enums.PaymentStatus;
import com.camping.duneinsolite.model.enums.TransactionStatus;
import com.camping.duneinsolite.model.enums.UserRole;
import com.camping.duneinsolite.repository.ReservationRepository;
import com.camping.duneinsolite.repository.TransactionRepository;
import com.camping.duneinsolite.service.NotificationPublisher;
import com.camping.duneinsolite.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class PaymentServiceImpl implements PaymentService {

    private final TransactionRepository transactionRepository;
    private final ReservationRepository reservationRepository;
    private final TransactionMapper transactionMapper;
    private final NotificationPublisher notificationPublisher;

    // ── Standalone payment ────────────────────────────────────────
    @Override
    public PaymentResponse recordPayment(UUID reservationId, PaymentRequest request) {

        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new RuntimeException("Reservation not found: " + reservationId));

        PaymentSummary current = computePaymentSummary(reservation);
        if (current.getPaymentStatus() == PaymentStatus.PAID) {
            throw new RuntimeException(
                    "This reservation is already fully paid. No further payments are required."
            );
        }

        Transaction transaction = buildTransaction(reservation, request);
        Transaction saved = transactionRepository.save(transaction);

        // Compute AFTER saving so the new transaction is included in the sum
        PaymentSummary summary = computePaymentSummary(reservation);

        publishPaymentReceivedInternal(reservation, request.getAmount());

        if (summary.getPaymentStatus() == PaymentStatus.PAID) {
            publishPaymentCompletedInternal(reservation);
        }

        TransactionResponse txResponse = transactionMapper.toResponse(saved);

        PaymentResponse response = new PaymentResponse();
        response.setTransactionId(txResponse.getTransactionId());
        response.setTransactionNumber(txResponse.getTransactionNumber());
        response.setAmount(txResponse.getAmount());
        response.setCurrency(txResponse.getCurrency());
        response.setPaymentMethod(txResponse.getPaymentMethod());
        response.setStatus(txResponse.getStatus());
        response.setTransactionDate(txResponse.getTransactionDate());
        response.setReservationId(reservationId);
        response.setPaymentSummary(summary);

        return response;
    }

    // ── Compute payment summary ───────────────────────────────────
    @Override
    @Transactional(readOnly = true)
    public PaymentSummary computePaymentSummary(Reservation reservation) {

        double originalMain   = reservation.getTotalAmount()       != null ? reservation.getTotalAmount()       : 0.0;
        double originalExtras = reservation.getTotalExtrasAmount() != null ? reservation.getTotalExtrasAmount() : 0.0;
        double originalTotal  = originalMain + originalExtras;

        double totalPaid = transactionRepository
                .sumCompletedAmountByReservationId(reservation.getReservationId());

        double remainingMain;
        double remainingExtras;

        if (totalPaid >= originalMain) {
            remainingMain = 0.0;
            double overflow = totalPaid - originalMain;
            remainingExtras = Math.max(0.0, originalExtras - overflow);
        } else {
            remainingMain   = originalMain - totalPaid;
            remainingExtras = originalExtras;
        }

        double remainingTotal = remainingMain + remainingExtras;

        PaymentStatus paymentStatus;
        if (totalPaid <= 0) {
            paymentStatus = PaymentStatus.UNPAID;
        } else if (remainingTotal > 0) {
            paymentStatus = PaymentStatus.PARTIALLY_PAID;
        } else {
            paymentStatus = PaymentStatus.PAID;
        }

        return PaymentSummary.builder()
                .originalMainAmount(originalMain)
                .originalExtrasAmount(originalExtras)
                .originalTotalAmount(originalTotal)
                .totalPaid(totalPaid)
                .remainingMainAmount(remainingMain)
                .remainingExtrasAmount(remainingExtras)
                .remainingTotal(remainingTotal)
                .paymentStatus(paymentStatus)
                .build();
    }

    // ── Build Transaction entity ──────────────────────────────────
    // public — called from ReservationServiceImpl for inline initial payment
    @Override
    public Transaction buildTransaction(Reservation reservation, PaymentRequest request) {
        String currency = request.getCurrency() != null
                ? request.getCurrency()
                : reservation.getCurrency();

        return Transaction.builder()
                .transactionNumber(generateTransactionNumber())
                .amount(request.getAmount())
                .currency(currency)
                .paymentMethod(request.getPaymentMethod())
                .status(TransactionStatus.COMPLETED)
                .reservation(reservation)
                .build();
    }

    // ── Notifications ─────────────────────────────────────────────
    // @Override — declared in interface so ReservationServiceImpl
    // can call them directly for the inline initial payment case

    @Override
    public void publishPaymentReceivedInternal(Reservation reservation, Double amount) {
        String amountFormatted = String.format("%.2f %s", amount, reservation.getCurrency());

        notificationPublisher.publish(
                RabbitMQConfig.PAYMENT_RECEIVED,
                NotificationMessage.builder()
                        .targetRoles(List.of(UserRole.ADMIN))
                        .type(NotificationType.PAYMENT_RECEIVED)
                        .reservationId(reservation.getReservationId())
                        .title("Paiement reçu")
                        .message("Le groupe \"" + reservation.getGroupName()
                                + "\" a effectué un paiement de " + amountFormatted + ".")
                        .build()
        );

        notificationPublisher.publish(
                RabbitMQConfig.PAYMENT_RECEIVED,
                NotificationMessage.builder()
                        .targetUserId(reservation.getUser().getUserId())
                        .type(NotificationType.PAYMENT_RECEIVED)
                        .reservationId(reservation.getReservationId())
                        .title("Paiement enregistré")
                        .message("Votre paiement de " + amountFormatted
                                + " pour la réservation du groupe \""
                                + reservation.getGroupName() + "\" a bien été enregistré.")
                        .build()
        );
    }

    @Override
    public void publishPaymentCompletedInternal(Reservation reservation) {
        notificationPublisher.publish(
                RabbitMQConfig.PAYMENT_COMPLETED,
                NotificationMessage.builder()
                        .targetRoles(List.of(UserRole.ADMIN))
                        .type(NotificationType.PAYMENT_COMPLETED)
                        .reservationId(reservation.getReservationId())
                        .title("Réservation entièrement payée")
                        .message("Le groupe \"" + reservation.getGroupName()
                                + "\" a réglé la totalité du montant dû.")
                        .build()
        );

        notificationPublisher.publish(
                RabbitMQConfig.PAYMENT_COMPLETED,
                NotificationMessage.builder()
                        .targetUserId(reservation.getUser().getUserId())
                        .type(NotificationType.PAYMENT_COMPLETED)
                        .reservationId(reservation.getReservationId())
                        .title("Paiement complet")
                        .message("Votre réservation pour le groupe \""
                                + reservation.getGroupName()
                                + "\" est entièrement réglée. Merci !")
                        .build()
        );
    }

    private String generateTransactionNumber() {
        long count = transactionRepository.count() + 1;
        return String.format("TXN-%05d", count);
    }
}