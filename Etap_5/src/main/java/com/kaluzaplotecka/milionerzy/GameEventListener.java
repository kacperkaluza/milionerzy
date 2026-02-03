package com.kaluzaplotecka.milionerzy;

/**
 * Interfejs nasłuchiwacza zdarzeń gry (wzorzec Observer).
 * Implementuj ten interfejs, aby reagować na zdarzenia w grze.
 */
@FunctionalInterface
public interface GameEventListener {
    /**
     * Wywoływane gdy wystąpi zdarzenie w grze.
     * @param event zdarzenie gry
     */
    void onGameEvent(GameEvent event);
}
