package com.kaluzaplotecka.milionerzy;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Random;

public class GameState {
    Board board;
    List<Player> players;
    int currentPlayerIndex;
    Deque<EventCard> chanceDeck;
    Deque<EventCard> communityChestDeck;
    int roundNumber;
    Random rand;

    public GameState(Board board, List<Player> players){
        this.board = board;
        this.players = new ArrayList<>(players);
        this.currentPlayerIndex = 0;
        this.chanceDeck = new ArrayDeque<>();
        this.communityChestDeck = new ArrayDeque<>();
        this.roundNumber = 0;
        this.rand = new Random();
    }

    public Board getBoard(){ return board; }

    public Player getCurrentPlayer(){
        if (players.isEmpty()) return null;
        return players.get(currentPlayerIndex);
    }

    public int rollDice(){
        int d1 = rand.nextInt(6) + 1;
        int d2 = rand.nextInt(6) + 1;
        return d1 + d2;
    }

    public void moveCurrentPlayer(){
        Player p = getCurrentPlayer();
        if (p == null) return;
        if (p.isInJail()){
            p.jailTurns++;
            if (p.jailTurns >= 3){
                p.releaseFromJail();
            }
            nextTurn();
            return;
        }

        int steps = rollDice();
        p.moveBy(steps, board);
        Tile t = board.getTile(p.getPosition());
        if (t != null) t.onLand(this, p);

        if (p.isBankrupt()){
            handleBankruptcy(p);
        }

        nextTurn();
    }

    public void nextTurn(){
        if (players.isEmpty()) return;
        currentPlayerIndex = (currentPlayerIndex + 1) % players.size();
        if (currentPlayerIndex == 0) roundNumber++;
    }

    public void handleBankruptcy(Player p){
        for (PropertyTile prop : new ArrayList<>(p.ownedProperties)){
            prop.owner = null;
            p.removeProperty(prop);
        }

        int removedIndex = players.indexOf(p);
        if (removedIndex >= 0){
            players.remove(removedIndex);
            if (removedIndex <= currentPlayerIndex && currentPlayerIndex > 0) {
                currentPlayerIndex--;
            }
            if (players.isEmpty()) currentPlayerIndex = 0;
            else currentPlayerIndex = currentPlayerIndex % players.size();
        }
    }
}
