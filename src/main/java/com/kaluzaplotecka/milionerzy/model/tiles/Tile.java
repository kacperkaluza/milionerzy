package com.kaluzaplotecka.milionerzy.model.tiles;

import java.io.Serializable;

import com.kaluzaplotecka.milionerzy.model.GameState;
import com.kaluzaplotecka.milionerzy.model.Player;

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
