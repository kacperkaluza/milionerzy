package com.kaluzaplotecka.milionerzy.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import com.kaluzaplotecka.milionerzy.model.tiles.PropertyTile;

public class Player implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private String id;
    private String username;
    private int money;
    private int position;
    private final List<PropertyTile> ownedProperties;
    private boolean inJail;
    private int jailTurns;

    public Player(String id, String username, int startingMoney) {
        this.id = id;
        this.username = username;
        this.money = startingMoney;
        this.position = 0;
        this.ownedProperties = new ArrayList<>();
        this.inJail = false;
        this.jailTurns = 0;
    }

    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }

    public void setPosition(int position) {
        this.position = position;
    }


    public String getUsername() {
        return username;
    }
    
    public void setName(String name) {
        this.username = name;
    }

    public int getMoney() {
        return money;
    }

    public int getPosition() {
        return position;
    }
    
    public List<PropertyTile> getOwnedProperties() {
        return new ArrayList<>(ownedProperties); // Return copy for safety
    }

    public boolean isInJail() {
        return inJail;
    }
    
    public int getJailTurns() {
        return jailTurns;
    }

    public void addMoney(int amount) {
        this.money += amount;
    }

    /**
     * Deduct money from player. Returns true if player still has non-negative balance after deduction.
     */
    public boolean deductMoney(int amount) {
        this.money -= amount;
        return this.money >= 0;
    }

    public void moveBy(int steps, Board board) {
        int newPos = (this.position + steps) % board.size();
        this.position = newPos;
    }

    public void moveTo(int pos) {
        this.position = pos;
    }

    public void goToJail() {
        this.inJail = true;
        this.jailTurns = 0;
    }

    public void releaseFromJail() {
        this.inJail = false;
        this.jailTurns = 0;
    }
    
    public void incrementJailTurns() {
        this.jailTurns++;
    }

    public void addProperty(PropertyTile p) {
        if (!ownedProperties.contains(p)) {
            ownedProperties.add(p);
        }
    }

    public void removeProperty(PropertyTile p) {
        ownedProperties.remove(p);
    }

    public boolean isBankrupt() {
        return this.money < 0;
    }

    @Override
    public String toString() {
        return "Player{" +
                "username='" + username + '\'' +
                ", money=" + money +
                ", position=" + position +
                ", inJail=" + inJail +
                '}';
    }
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Player player = (Player) o;

        return id != null ? id.equals(player.id) : player.id == null;
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }
}
