package com.kaluzaplotecka.milionerzy;

public class CommunityChestTile extends Tile {
    public CommunityChestTile(int position, String name){
        super(position, name);
    }

    @Override
    public void onLand(GameState state, Player player){
        if (state == null || player == null) return;
        state.executeCommunityChestCardFor(player);
    }
}
