package com.kaluzaplotecka.milionerzy;

import java.io.Serializable;
import java.util.List;

public class Board implements Serializable {
    private static final long serialVersionUID = 1L;
    List<Tile> tiles;

    public Board(List<Tile> tiles){
        this.tiles = tiles;
    }

    public Tile getTile(int position){
        if (tiles == null || tiles.isEmpty()) return null;
        int p = ((position % tiles.size()) + tiles.size()) % tiles.size();
        return tiles.get(p);
    }

    public int size(){
        return tiles == null ? 0 : tiles.size();
    }
}