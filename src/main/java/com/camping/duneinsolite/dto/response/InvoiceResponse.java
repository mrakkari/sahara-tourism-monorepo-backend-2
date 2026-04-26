package com.camping.duneinsolite.dto.response;

import com.camping.duneinsolite.model.enums.Currency;
import com.camping.duneinsolite.model.enums.InvoiceStatus;
import com.camping.duneinsolite.model.enums.InvoiceType;
import com.camping.duneinsolite.model.enums.PaymentStatus;
import lombok.Data;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Data
public class InvoiceResponse {
    private UUID invoiceId;
    private String invoiceNumber;
    private InvoiceType invoiceType;
    private LocalDate invoiceDate;
    private LocalDate dueDate;
    private Double totalAmount;
    private Double paidAmount;
    private Double remainingAmount;
    private InvoiceStatus status;
    private PaymentStatus paymentStatus;
    private Currency currency;
    private UUID reservationId;
    private UUID userId;
    private String userName;
    private List<InvoiceItemResponse> items;
}