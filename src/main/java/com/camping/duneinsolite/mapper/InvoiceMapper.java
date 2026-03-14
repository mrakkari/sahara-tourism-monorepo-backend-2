package com.camping.duneinsolite.mapper;

import com.camping.duneinsolite.dto.response.InvoiceItemResponse;
import com.camping.duneinsolite.dto.response.InvoiceResponse;
import com.camping.duneinsolite.model.Invoice;
import com.camping.duneinsolite.model.InvoiceItem;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface InvoiceMapper {

    @Mapping(source = "reservation.reservationId", target = "reservationId")
    @Mapping(source = "user.userId", target = "userId")
    @Mapping(source = "user.name", target = "userName")
    @Mapping(target = "remainingAmount", expression = "java(invoice.getRemainingAmount())")
    InvoiceResponse toResponse(Invoice invoice);

    @Mapping(target = "totalPrice", expression = "java(item.getTotalPrice())")
    InvoiceItemResponse toItemResponse(InvoiceItem item);
}