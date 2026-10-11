package com.tcs.bank.account.service;

import com.tcs.bank.account.messaging.ClientLifecycleEvent;

public interface ClientProjectionService {

    void apply(ClientLifecycleEvent event);
}
