package com.rolecall.payment.repository;

import com.rolecall.payment.entity.PaymentTransaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PaymentTransactionRepository extends JpaRepository<PaymentTransaction, UUID> {

    Page<PaymentTransaction> findByUserId(UUID userId, Pageable pageable);

    Optional<PaymentTransaction> findByStripeSessionId(String stripeSessionId);
}
