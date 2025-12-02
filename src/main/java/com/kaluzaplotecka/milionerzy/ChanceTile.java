package com.kaluzaplotecka.milionerzy;

public class ChanceTile extends Tile {
    public ChanceTile(int position, String name){
        super(position, name);
    }

    @Override
    public void onLand(GameState state, Player player){
        if (state == null || player == null) return;
        state.executeChanceCardFor(player);
    }
}
