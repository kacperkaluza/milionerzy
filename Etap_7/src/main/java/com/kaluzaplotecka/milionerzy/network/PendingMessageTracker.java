package com.kaluzaplotecka.milionerzy.network;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Śledzi wiadomości oczekujące na potwierdzenie ACK.
 * Automatycznie ponawia wysyłanie przy timeout i wywołuje callbacki.
 */
public class PendingMessageTracker {
    
    private static final long DEFAULT_TIMEOUT_MS = 5000;  // 5 sekund
    private static final int MAX_RETRIES = 3;
    
    /**
     * Informacje o oczekującej wiadomości.
     */
    private static class PendingMessage {
        final GameMessage message;
        int retryCount;
        ScheduledFuture<?> timeoutTask;
        
        PendingMessage(GameMessage message) {
            this.message = message;
            this.retryCount = 0;
        }
    }
    
    private final Map<String, PendingMessage> pendingMessages = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler;
    
    // Callbacki
    private Consumer<GameMessage> resendCallback;      // wywoływane gdy trzeba ponowić wysłanie
    private Consumer<GameMessage> ackCallback;         // wywoływane po otrzymaniu ACK
    private Consumer<GameMessage> nackCallback;        // wywoływane po otrzymaniu NACK
    private Consumer<GameMessage> timeoutCallback;     // wywoływane po przekroczeniu max prób
    
    public PendingMessageTracker() {
        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "PendingMessageTracker");
            t.setDaemon(true);
            return t;
        });
    }
    
    /**
     * Rozpoczyna śledzenie wiadomości wymagającej ACK.
     * @param message wiadomość do śledzenia
     */
    public void track(GameMessage message) {
        if (message == null || !message.requiresAck()) {
            return;
        }
        
        PendingMessage pending = new PendingMessage(message);
        
        // Zaplanuj sprawdzenie timeout
        pending.timeoutTask = scheduler.schedule(
            () -> checkTimeout(message.getMessageId()),
            DEFAULT_TIMEOUT_MS,
            TimeUnit.MILLISECONDS
        );
        
        pendingMessages.put(message.getMessageId(), pending);
    }
    
    /**
     * Potwierdza otrzymanie wiadomości (ACK).
     * @param messageId ID wiadomości do potwierdzenia
     */
    public void acknowledge(String messageId) {
        PendingMessage pending = pendingMessages.remove(messageId);
        if (pending != null) {
            // Anuluj zaplanowany timeout
            if (pending.timeoutTask != null) {
                pending.timeoutTask.cancel(false);
            }
            
            // Wywołaj callback
            if (ackCallback != null) {
                ackCallback.accept(pending.message);
            }
        }
    }
    
    /**
     * Odrzuca wiadomość (NACK).
     * @param messageId ID wiadomości
     * @param reason powód odrzucenia
     */
    public void reject(String messageId, String reason) {
        PendingMessage pending = pendingMessages.remove(messageId);
        if (pending != null) {
            // Anuluj zaplanowany timeout
            if (pending.timeoutTask != null) {
                pending.timeoutTask.cancel(false);
            }
            
            // Wywołaj callback
            if (nackCallback != null) {
                nackCallback.accept(pending.message);
            }
        }
    }
    
    /**
     * Sprawdza czy upłynął timeout dla wiadomości.
     */
    private void checkTimeout(String messageId) {
        PendingMessage pending = pendingMessages.get(messageId);
        if (pending == null) {
            return;  // Już potwierdzone lub usunięte
        }
        
        pending.retryCount++;
        
        if (pending.retryCount >= MAX_RETRIES) {
            // Przekroczono maksymalną liczbę prób
            pendingMessages.remove(messageId);
            
            if (timeoutCallback != null) {
                timeoutCallback.accept(pending.message);
            }
        } else {
            // Spróbuj ponownie
            if (resendCallback != null) {
                resendCallback.accept(pending.message);
            }
            
            // Zaplanuj kolejne sprawdzenie
            pending.timeoutTask = scheduler.schedule(
                () -> checkTimeout(messageId),
                DEFAULT_TIMEOUT_MS,
                TimeUnit.MILLISECONDS
            );
        }
    }
    
    /**
     * Sprawdza czy wiadomość jest w trakcie oczekiwania na ACK.
     */
    public boolean isPending(String messageId) {
        return pendingMessages.containsKey(messageId);
    }
    
    /**
     * Zwraca liczbę oczekujących wiadomości.
     */
    public int getPendingCount() {
        return pendingMessages.size();
    }
    
    /**
     * Zatrzymuje tracker i anuluje wszystkie oczekujące zadania.
     */
    public void shutdown() {
        scheduler.shutdownNow();
        pendingMessages.clear();
    }
    
    // === Settery dla callbacków ===
    
    public void setResendCallback(Consumer<GameMessage> callback) {
        this.resendCallback = callback;
    }
    
    public void setAckCallback(Consumer<GameMessage> callback) {
        this.ackCallback = callback;
    }
    
    public void setNackCallback(Consumer<GameMessage> callback) {
        this.nackCallback = callback;
    }
    
    public void setTimeoutCallback(Consumer<GameMessage> callback) {
        this.timeoutCallback = callback;
    }
}
