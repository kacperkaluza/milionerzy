package com.kaluzaplotecka.milionerzy.view.utils;

public class UIConstants {
    private UIConstants() {} // Prevent instantiation

    // Colors
    public static final String PRIMARY_GRADIENT_START = "#667eea";
    public static final String PRIMARY_GRADIENT_END = "#764ba2";
    public static final String TEXT_PRIMARY = "#2d3436";
    public static final String TEXT_SECONDARY = "#636e72";
    public static final String TEXT_WHITE = "white";
    public static final String SUCCESS_COLOR = "#27ae60";
    public static final String ERROR_COLOR = "#e74c3c";
    
    // Backgrounds
    public static final String BACKGROUND_GRADIENT = "-fx-background-color: linear-gradient(to bottom right, #f5f7fa, #e4e8f0);";
    public static final String GAME_BACKGROUND_GRADIENT = "-fx-background-color: linear-gradient(to bottom, #88bde7, #dbebea);";

    // Styles
    public static final String CARD_STYLE = 
        "-fx-background-color: white; " +
        "-fx-background-radius: 16; " +
        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 20, 0, 0, 5);";
        
    public static final String INPUT_STYLE = 
        "-fx-background-color: #f8f9fa; " +
        "-fx-border-color: #e9ecef; " +
        "-fx-border-width: 2; " +
        "-fx-border-radius: 8; " +
        "-fx-background-radius: 8; " +
        "-fx-padding: 10 15; " +
        "-fx-font-size: 14px;";
        
    public static final String INPUT_FOCUS_STYLE = 
        "-fx-background-color: white; " +
        "-fx-border-color: #667eea; " +
        "-fx-border-width: 2; " +
        "-fx-border-radius: 8; " +
        "-fx-background-radius: 8; " +
        "-fx-padding: 10 15; " +
        "-fx-font-size: 14px;";
}
