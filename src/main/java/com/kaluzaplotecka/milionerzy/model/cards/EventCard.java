package com.kaluzaplotecka.milionerzy.model.cards;

import java.io.Serializable;

import com.kaluzaplotecka.milionerzy.model.GameState;
import com.kaluzaplotecka.milionerzy.model.Player;
import com.kaluzaplotecka.milionerzy.model.tiles.Tile;

/**
 * Reprezentuje kartę zdarzenia (Szansa lub Kasa Społeczna).
 * 
 * <p>Karty mogą wywoływać różne efekty:
 * <ul>
 *   <li>{@link ActionType#PAY} - gracz płaci określoną kwotę</li>
 *   <li>{@link ActionType#RECEIVE} - gracz otrzymuje określoną kwotę</li>
 *   <li>{@link ActionType#MOVE_TO} - gracz przesuwa się na określone pole</li>
 *   <li>{@link ActionType#GO_TO_JAIL} - gracz idzie do więzienia</li>
 * </ul>
 * 
 * @see com.kaluzaplotecka.milionerzy.model.tiles.ChanceTile
 * @see com.kaluzaplotecka.milionerzy.model.tiles.CommunityChestTile
 */
public class EventCard implements Serializable {
    private static final long serialVersionUID = 1L;
    
    /**
     * Typ akcji wykonywanej przez kartę.
     */
    public enum ActionType { 
        /** Gracz płaci kwotę. */
        PAY, 
        /** Gracz otrzymuje kwotę. */
        RECEIVE, 
        /** Gracz przesuwa się na określone pole. */
        MOVE_TO, 
        /** Gracz idzie do więzienia. */
        GO_TO_JAIL 
    }

    /** Opis karty wyświetlany graczowi. */
    String description;
    
    /** Typ akcji karty. */
    ActionType type;
    
    /** Wartość parametru akcji (kwota lub pozycja pola). */
    int amountOrPosition;

    /**
     * Tworzy nową kartę zdarzenia.
     *
     * @param description opis karty wyświetlany graczowi
     * @param type typ akcji
     * @param amountOrPosition kwota (dla PAY/RECEIVE) lub pozycja pola (dla MOVE_TO)
     */
    public EventCard(String description, ActionType type, int amountOrPosition){
        this.description = description;
        this.type = type;
        this.amountOrPosition = amountOrPosition;
    }

    /**
     * Wykonuje efekt karty na graczu.
     * 
     * <p>Emituje odpowiednie zdarzenia gry w zależności od typu akcji.
     *
     * @param state stan gry
     * @param player gracz, na którym wykonywana jest akcja
     */
    public void execute(GameState state, Player player){
        switch(type){
            case PAY:
                player.deductMoney(amountOrPosition);
                state.fireEvent(new com.kaluzaplotecka.milionerzy.events.GameEvent(
                    com.kaluzaplotecka.milionerzy.events.GameEvent.Type.MONEY_CHANGED,
                    player,
                    -amountOrPosition,
                    player.getUsername() + " płaci " + amountOrPosition
                ));
                break;
            case RECEIVE:
                player.addMoney(amountOrPosition);
                state.fireEvent(new com.kaluzaplotecka.milionerzy.events.GameEvent(
                    com.kaluzaplotecka.milionerzy.events.GameEvent.Type.MONEY_CHANGED,
                    player,
                    amountOrPosition,
                    player.getUsername() + " otrzymuje " + amountOrPosition
                ));
                break;
            case MOVE_TO:
                player.moveTo(amountOrPosition);
                Tile t = state.getBoard().getTile(player.getPosition());
                if (t != null) t.onLand(state, player);
                break;
            case GO_TO_JAIL:
                player.goToJail();
                break;
        }
    }
}
