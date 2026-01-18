package com.kaluzaplotecka.milionerzy;

import javafx.stage.Stage;
import org.junit.jupiter.api.Test;
import org.testfx.framework.junit5.ApplicationTest;
import org.testfx.util.WaitForAsyncUtils;

import java.util.ArrayList;
import java.util.List;

import static org.testfx.api.FxAssert.verifyThat;
import static org.testfx.matcher.control.LabeledMatchers.hasText;

public class GameBoardTest extends ApplicationTest {

    private GameBoardView gameBoardView;
    private List<Player> players;

    @Override
    public void start(Stage stage) {
        players = new ArrayList<>();
        players.add(new Player("TestPlayer1", 1500));
        players.add(new Player("TestPlayer2", 1500));
        
        gameBoardView = new GameBoardView(stage, players, null, "1");
        gameBoardView.show();
    }

    @Test
    public void testPlayerNamesDisplayed() {
        // Wait for UI to stabilize
        WaitForAsyncUtils.waitForFxEvents();
        
        // Use lookup to find nodes with specific text
        // If these fail to find a node, the test will fail with an EmptyNodeQueryException
        verifyThat(lookup(hasText("TestPlayer1")), (node) -> node.isVisible());
        verifyThat(lookup(hasText("TestPlayer2")), (node) -> node.isVisible());
        
        // Verify money is displayed (using formatted string)
        verifyThat(lookup(hasText("1,500")), (node) -> node.isVisible());
    }
}
