package com.kaluzaplotecka.milionerzy;
import java.util.Deque;
import java.util.List;

public class GameState {
    Board board;
    List<Player> players;
    int currentPlayerIndex;
    Deque<EventCard> chanceDeck;
    Deque<EventCard> communityChestDeck;
    int roundNumber;

}
