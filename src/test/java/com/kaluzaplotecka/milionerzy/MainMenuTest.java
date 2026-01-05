package com.kaluzaplotecka.milionerzy;

import javafx.stage.Stage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.api.FxAssert;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;
import org.testfx.matcher.control.LabeledMatchers;

@ExtendWith(ApplicationExtension.class)
class MainMenuTest {

    @Start
    public void start(Stage stage) {
        new MainMenu().start(stage);
    }

    @Test
    void shouldHaveCorrectTitle() {
        // Retrieve the label with the specific text
        // Note: For robust tests, we usually add IDs to nodes. 
        // For this example, we'll lookup by text content if possible or just check presence.
        
        // Verifying that a button with specific text exists
        FxAssert.verifyThat(".button", LabeledMatchers.hasText("🎮  Stwórz Grę"));
    }
}
