package com.tcs.bank.account.repository;

import com.tcs.bank.account.model.entity.Account;
import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountRepository extends JpaRepository<Account, Long> {

    boolean existsByAccountNumber(String accountNumber);

    boolean existsByAccountNumberAndIdNot(String accountNumber, Long accountId);

    List<Account> findByClient_ClientIdOrderById(Long clientId);

    List<Account> findAll(Sort sort);
}
