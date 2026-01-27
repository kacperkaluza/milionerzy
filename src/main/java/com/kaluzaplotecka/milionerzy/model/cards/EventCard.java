package com.kaluzaplotecka.milionerzy.model.cards;

import java.io.Serializable;

import com.kaluzaplotecka.milionerzy.model.GameState;
import com.kaluzaplotecka.milionerzy.model.Player;
import com.kaluzaplotecka.milionerzy.model.tiles.Tile;

public class EventCard implements Serializable {
    private static final long serialVersionUID = 1L;
    public enum ActionType { PAY, RECEIVE, MOVE_TO, GO_TO_JAIL }

    String description;
    ActionType type;
    int amountOrPosition;

    public EventCard(String description, ActionType type, int amountOrPosition){
        this.description = description;
        this.type = type;
        this.amountOrPosition = amountOrPosition;
    }

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
