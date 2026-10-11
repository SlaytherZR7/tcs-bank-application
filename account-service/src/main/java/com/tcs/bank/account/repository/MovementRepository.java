package com.tcs.bank.account.repository;

import com.tcs.bank.account.model.entity.Movement;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MovementRepository extends JpaRepository<Movement, Long> {

    List<Movement> findAllByOrderByMovementDateAscIdAsc();

    List<Movement> findByAccount_AccountNumberOrderByMovementDateAscIdAsc(String accountNumber);

    @Query("""
            SELECT m FROM Movement m
            WHERE m.account.client.clientId = :clientId
              AND m.movementDate BETWEEN :from AND :to
            ORDER BY m.account.id, m.movementDate, m.id
            """)
    List<Movement> findClientMovementsBetween(
            @Param("clientId") Long clientId,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to);

    @Query("""
            SELECT m FROM Movement m
            WHERE m.account.id IN :accountIds
              AND m.movementDate <= :to
              AND NOT EXISTS (
                  SELECT 1 FROM Movement later
                  WHERE later.account = m.account
                    AND later.movementDate <= :to
                    AND (later.movementDate > m.movementDate
                         OR (later.movementDate = m.movementDate AND later.id > m.id)))
            """)
    List<Movement> findLastMovementsUpTo(
            @Param("accountIds") Collection<Long> accountIds,
            @Param("to") LocalDateTime to);

    boolean existsByCorrectionOf_Id(Long movementId);
}
