package com.camping.duneinsolite.repository;

import com.camping.duneinsolite.model.Transaction;
import com.camping.duneinsolite.model.enums.TransactionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, UUID> {
    List<Transaction> findByReservationReservationId(UUID reservationId);
    List<Transaction> findByInvoiceInvoiceId(UUID invoiceId);
    List<Transaction> findByStatus(TransactionStatus status);
}