package org.unihubworkshop.paymentservice.mapper;

import org.mapstruct.Mapper;
import org.unihubworkshop.paymentservice.dto.PaymentResponse;
import org.unihubworkshop.paymentservice.models.Payment;

@Mapper(componentModel = "spring")
public interface PaymentMapper {
    PaymentResponse toResponse(Payment payment);
}

