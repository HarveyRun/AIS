package com.shixianwen.realtime;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
@RequiredArgsConstructor
public class RealtimePublisher {
    private final RealtimeWebSocketHandler handler;

    public void afterCommit(Long userId, String type, Object payload) {
        Runnable publish = () -> handler.send(userId, type, payload);
        afterCommit(publish);
    }

    public void afterCommitToAdmins(String type, Object payload) {
        Runnable publish = () -> handler.sendAdmins(type, payload);
        afterCommit(publish);
    }

    public void afterCommitToAllUsers(String type, Object payload) {
        Runnable publish = () -> handler.sendAllUsers(type, payload);
        afterCommit(publish);
    }

    public boolean isUserOnline(Long userId) {
        return handler.isUserOnline(userId);
    }

    private void afterCommit(Runnable publish) {
        if (!TransactionSynchronizationManager.isActualTransactionActive()) {
            publish.run();
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                publish.run();
            }
        });
    }
}
