package com.camping.duneinsolite.dto.request;

import com.camping.duneinsolite.model.enums.PaymentMethod;
import jakarta.validation.constraints.*;
import lombok.Data;
import java.util.UUID;

@Data
public class TransactionRequest {

    @NotNull(message = "Reservation ID is required")
    private UUID reservationId;

    // Optional — can be linked to an invoice
    private UUID invoiceId;

    @NotNull(message = "Amount is required")
    @Positive(message = "Amount must be positive")
    private Double amount;

    @NotBlank(message = "Currency is required")
    @Size(min = 3, max = 3, message = "Currency must be 3 characters")
    private String currency;

    @NotNull(message = "Payment method is required")
    private PaymentMethod paymentMethod;
}