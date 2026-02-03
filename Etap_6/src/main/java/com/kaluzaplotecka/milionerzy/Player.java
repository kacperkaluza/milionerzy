package com.kaluzaplotecka.milionerzy;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Player implements Serializable {
    private static final long serialVersionUID = 1L;
    String username;
    int money;
    int position;
    List<PropertyTile> ownedProperties;
    boolean inJail;
    int jailTurns;

    public Player(String username, int startingMoney) {
        this.username = username;
        this.money = startingMoney;
        this.position = 0;
        this.ownedProperties = new ArrayList<>();
        this.inJail = false;
        this.jailTurns = 0;
    }

    public String getUsername() { return username; }
    public int getMoney() { return money; }
    public int getPosition() { return position; }
    public boolean isInJail() { return inJail; }

    public void addMoney(int amount){
        this.money += amount;
    }

    /**
     * Deduct money from player. Returns true if player still has non-negative balance after deduction.
     */
    public boolean deductMoney(int amount){
        this.money -= amount;
        return this.money >= 0;
    }

    public void moveBy(int steps, Board board){
        int newPos = (this.position + steps) % board.size();
        this.position = newPos;
    }

    public void moveTo(int pos){
        this.position = pos;
    }

    public void goToJail(){
        this.inJail = true;
        this.jailTurns = 0;
        // Assuming jail position is handled by Board/GameState
    }

    public void releaseFromJail(){
        this.inJail = false;
        this.jailTurns = 0;
    }

    public void addProperty(PropertyTile p){
        if (!ownedProperties.contains(p)) ownedProperties.add(p);
    }

    public void removeProperty(PropertyTile p){
        ownedProperties.remove(p);
    }

    public boolean isBankrupt(){
        return this.money < 0;
    }
}
