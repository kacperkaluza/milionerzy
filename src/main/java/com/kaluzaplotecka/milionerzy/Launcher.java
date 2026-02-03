package com.kaluzaplotecka.milionerzy;

import com.kaluzaplotecka.milionerzy.view.MainMenu;

import javafx.application.Application;

/**
 * Punkt wejścia aplikacji.
 * 
 * <p>Klasa uruchamia aplikację JavaFX poprzez {@link MainMenu}.
 * Wymagana do poprawnego działania modułu JavaFX.
 * 
 * @see MainMenu
 */
public class Launcher {
    
    /**
     * Główna metoda uruchamiająca aplikację.
     *
     * @param args argumenty wiersza poleceń
     */
    static void main(String[] args) {
        Application.launch(MainMenu.class, args);
    }
}
