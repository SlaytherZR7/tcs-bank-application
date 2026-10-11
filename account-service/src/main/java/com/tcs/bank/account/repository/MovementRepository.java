package com.tcs.bank.account.repository;

import com.tcs.bank.account.model.entity.Movement;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MovementRepository extends JpaRepository<Movement, Long> {

    List<Movement> findAllByOrderByMovementDateAscIdAsc();

    List<Movement> findByAccount_AccountNumberOrderByMovementDateAscIdAsc(String accountNumber);

    List<Movement> findByAccount_IdAndMovementDateBetweenOrderByMovementDateAscIdAsc(
            Long accountId, LocalDateTime from, LocalDateTime to);

    boolean existsByCorrectionOf_Id(Long movementId);
}
