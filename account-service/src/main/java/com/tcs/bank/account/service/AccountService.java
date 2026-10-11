package com.tcs.bank.account.service;

import com.tcs.bank.account.model.dto.AccountResponse;
import com.tcs.bank.account.model.dto.CreateAccountRequest;
import com.tcs.bank.account.model.dto.PatchAccountRequest;
import com.tcs.bank.account.model.dto.UpdateAccountRequest;
import java.util.List;

public interface AccountService {

    List<AccountResponse> findAll(Long clientId);

    AccountResponse findById(Long accountId);

    AccountResponse create(CreateAccountRequest request);

    AccountResponse replace(Long accountId, UpdateAccountRequest request);

    AccountResponse patch(Long accountId, PatchAccountRequest request);

    void softDelete(Long accountId);
}
