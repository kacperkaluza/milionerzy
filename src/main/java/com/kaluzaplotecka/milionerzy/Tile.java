package com.kaluzaplotecka.milionerzy;

import java.io.Serializable;

public class Tile implements Serializable {
    private static final long serialVersionUID = 1L;
    int position;
    String name;

    public Tile(int position, String name){
        this.position = position;
        this.name = name;
    }

    public int getPosition(){ return position; }

    public void onLand(GameState state, Player player){
        // Type of action when a player lands on this tile
    }
}
