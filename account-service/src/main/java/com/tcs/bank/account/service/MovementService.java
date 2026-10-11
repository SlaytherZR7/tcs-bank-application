package com.tcs.bank.account.service;

import com.tcs.bank.account.model.dto.CreateMovementRequest;
import com.tcs.bank.account.model.dto.MovementResponse;
import com.tcs.bank.account.model.dto.UpdateMovementRequest;
import java.util.List;

public interface MovementService {

    List<MovementResponse> findAll(String accountNumber);

    MovementResponse findById(Long movementId);

    MovementResponse create(CreateMovementRequest request);

    MovementResponse replace(Long movementId, UpdateMovementRequest request);

    void compensate(Long movementId);
}
