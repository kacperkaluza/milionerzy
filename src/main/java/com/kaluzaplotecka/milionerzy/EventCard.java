package com.kaluzaplotecka.milionerzy;

import java.io.Serializable;

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
                break;
            case RECEIVE:
                player.addMoney(amountOrPosition);
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
