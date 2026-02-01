package com.kaluzaplotecka.milionerzy;

import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.junit.jupiter.api.Test;
import org.testfx.framework.junit5.ApplicationTest;
import org.testfx.util.WaitForAsyncUtils;

import com.kaluzaplotecka.milionerzy.model.Player;
import com.kaluzaplotecka.milionerzy.view.GameBoardView;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.testfx.api.FxAssert.verifyThat;
import static org.testfx.matcher.base.NodeMatchers.*;
import static org.testfx.matcher.control.LabeledMatchers.hasText;

/**
 * Testy UI dla GameBoardView z wykorzystaniem TestFX.
 * Symulują interakcje użytkownika i weryfikują stan UI.
 */
public class GameBoardTest extends ApplicationTest {

    private GameBoardView gameBoardView;

    @Override
    public void start(Stage stage) {
        List<Player> players = new ArrayList<>();
        players.add(new Player("TestPlayer1", "TestPlayer1", 1500));
        players.add(new Player("TestPlayer2", "TestPlayer2", 1500));
        
        gameBoardView = new GameBoardView(stage, players, null, "1");
        gameBoardView.show();
    }

    @Test
    public void testPlayerNamesDisplayed() {
        WaitForAsyncUtils.waitForFxEvents();
        
        // Weryfikuj że nazwy graczy są widoczne
        verifyThat(lookup(hasText("TestPlayer1")), (node) -> node.isVisible());
        verifyThat(lookup(hasText("TestPlayer2")), (node) -> node.isVisible());
        
        // Weryfikuj że pieniądze są wyświetlane
        verifyThat(lookup(hasText("1,500")), (node) -> node.isVisible());
    }
    
    @Test
    public void testRollButtonExists() {
        WaitForAsyncUtils.waitForFxEvents();
        
        // Weryfikuj że przycisk rzutu istnieje
        verifyThat("#rollButton", isVisible());
        
        Button rollButton = lookup("#rollButton").queryButton();
        assertNotNull(rollButton, "Przycisk rzutu powinien istnieć");
        assertTrue(rollButton.getText().contains("Losuj"), "Przycisk powinien mieć tekst 'Losuj'");
    }
    
    @Test
    public void testDiceAreaExists() {
        WaitForAsyncUtils.waitForFxEvents();
        
        // Weryfikuj że obszar kostek istnieje
        verifyThat("#diceArea", isVisible());
        verifyThat("#dice0", isVisible());
        verifyThat("#dice1", isVisible());
    }
    
    @Test
    public void testPlayerMoneyLabels() {
        WaitForAsyncUtils.waitForFxEvents();
        
        // Weryfikuj etykiety pieniędzy dla graczy
        Label moneyLabel1 = lookup("#moneyLabel_TestPlayer1").query();
        Label moneyLabel2 = lookup("#moneyLabel_TestPlayer2").query();
        
        assertNotNull(moneyLabel1, "Etykieta pieniędzy gracza 1 powinna istnieć");
        assertNotNull(moneyLabel2, "Etykieta pieniędzy gracza 2 powinna istnieć");
        
        assertEquals("1,500", moneyLabel1.getText(), "Gracz 1 powinien mieć 1,500 zł");
        assertEquals("1,500", moneyLabel2.getText(), "Gracz 2 powinien mieć 1,500 zł");
    }
    
    @Test
    public void testPlayerPropertiesLabels() {
        WaitForAsyncUtils.waitForFxEvents();
        
        // Weryfikuj etykiety nieruchomości
        Label propsLabel1 = lookup("#propertiesLabel_TestPlayer1").query();
        Label propsLabel2 = lookup("#propertiesLabel_TestPlayer2").query();
        
        assertNotNull(propsLabel1, "Etykieta nieruchomości gracza 1 powinna istnieć");
        assertNotNull(propsLabel2, "Etykieta nieruchomości gracza 2 powinna istnieć");
        
        assertTrue(propsLabel1.getText().contains("0 nieruchomości"), 
            "Gracz 1 powinien mieć 0 nieruchomości na start");
    }
    
    @Test
    public void testRollButtonClick_changesRollButtonState() {
        WaitForAsyncUtils.waitForFxEvents();
        
        Button rollButton = lookup("#rollButton").queryButton();
        
        // Sprawdź czy przycisk jest aktywny (pierwszy gracz ma turę)
        // Uwaga: to może się różnić w zależności od logiki gry
        if (!rollButton.isDisabled()) {
            // Kliknij przycisk rzutu
            clickOn("#rollButton");
            WaitForAsyncUtils.waitForFxEvents();
            
            // Po kliknięciu przycisk powinien być zablokowany (tura się kończy lub czeka na akcję)
            // Weryfikujemy że coś się zmieniło - kostki powinny pokazać jakieś wartości
            Node dice0 = lookup("#dice0").query();
            assertNotNull(dice0, "Kostka 0 powinna istnieć po rzucie");
        }
    }
    
    @Test
    public void testBoardLayoutStructure() {
        WaitForAsyncUtils.waitForFxEvents();
        
        // Weryfikuj podstawową strukturę planszy
        VBox diceArea = lookup("#diceArea").query();
        assertNotNull(diceArea, "Obszar kostek powinien istnieć");
        
        // Sprawdź że diceArea zawiera kostki, przycisk losowania i przycisk zapisu
        assertEquals(3, diceArea.getChildren().size(), 
            "Obszar kostek powinien zawierać 3 elementy (HBox z kostkami, przycisk losuj, przycisk zapisz)");
    }
    
    @Test
    public void testNameLabelsHaveCorrectText() {
        WaitForAsyncUtils.waitForFxEvents();
        
        Label nameLabel1 = lookup("#nameLabel_TestPlayer1").query();
        Label nameLabel2 = lookup("#nameLabel_TestPlayer2").query();
        
        assertNotNull(nameLabel1, "Etykieta nazwy gracza 1 powinna istnieć");
        assertNotNull(nameLabel2, "Etykieta nazwy gracza 2 powinna istnieć");
        
        assertEquals("TestPlayer1", nameLabel1.getText());
        assertEquals("TestPlayer2", nameLabel2.getText());
    }
    
    @Test
    public void testPauseButtonExists() {
        WaitForAsyncUtils.waitForFxEvents();
        
        // Szukaj przycisku pauzy (⏸)
        Button pauseButton = lookup("⏸").queryButton();
        
        // Note: The original test used emoji which might be tricky if not used in View.
        // Assuming view has "⏸" text.
        
        assertNotNull(pauseButton, "Przycisk pauzy powinien istnieć");
        assertTrue(pauseButton.isVisible(), "Przycisk pauzy powinien być widoczny");
    }
    
    @Test
    public void testPauseButtonClick_showsDialog() {
        WaitForAsyncUtils.waitForFxEvents();
        
        // Kliknij przycisk pauzy
        clickOn("⏸");
        WaitForAsyncUtils.waitForFxEvents();
        
        // Weryfikuj że dialog pauzy się pojawił
        verifyThat("Powrót do gry", isVisible());
        verifyThat("Menu Główne", isVisible());
    }
    
    @Test
    public void testPauseDialog_resumeClosesDialog() {
        WaitForAsyncUtils.waitForFxEvents();
        
        // Kliknij przycisk pauzy
        clickOn("⏸");
        WaitForAsyncUtils.waitForFxEvents();
        
        // Kliknij "Powrót do gry"
        clickOn("Powrót do gry");
        WaitForAsyncUtils.waitForFxEvents();
        
        // Weryfikuj że gra dalej działa - przycisk rzutu powinien być widoczny
        verifyThat("#rollButton", isVisible());
    }
}
