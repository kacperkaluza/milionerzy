package com.kaluzaplotecka.milionerzy;

public class Tile {
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
