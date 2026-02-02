package com.kaluzaplotecka.milionerzy.view.components;

import javafx.animation.ScaleTransition;
import javafx.scene.control.Button;
import javafx.util.Duration;
import com.kaluzaplotecka.milionerzy.view.utils.UIConstants;

/**
 * Custom reusable button component with gradient background, animations, and sound effects.
 */
public class GameButton extends Button {

    private static final String DEFAULT_FONT_WEIGHT = "bold";
    private static final String DEFAULT_FONT_SIZE = "16px";
    
    // Default Style Properties
    private String fontSize = DEFAULT_FONT_SIZE;
    private String textColor = UIConstants.TEXT_WHITE;
    private String opacity = "1";
    private String padding = "8 16"; 
    private String borderColor = "transparent";
    private String borderWidth = "0";
    private String borderRadius = "30";

    // Default Gradient
    private String gradientStart = UIConstants.PRIMARY_GRADIENT_START;
    private String gradientEnd = UIConstants.PRIMARY_GRADIENT_END;
    
    // Default Shadow Color
    private String shadowColor = "rgba(45, 45, 45, 0.15)";


    /**
     * Base constructor.
     * Sets the text, default blue color, border radius 30, no border, padding "2 4", text size "12".
     *
     * @param text The text to display on the button.
     * @param onClick The action to run when clicked.
     */
    public GameButton(String text, Runnable onClick) {
        super(text);
        
        if (onClick != null) {
            setOnAction(e -> onClick.run());
        }

        initializeStyle();
        setupAnimations();
    }

    /**
     * Constructor with custom size.
     *
     * @param text The text to display.
     * @param width The minimum width of the button.
     * @param height The minimum height of the button.
     * @param fontSize The font size in pixels.
     * @param onClick The action to run when clicked.
     */
    public GameButton(String text, double width, double height, int fontSize, Runnable onClick) {
        this(text, onClick);
        setMinWidth(width);
        setMinHeight(height);
        setTextSize(fontSize);
    }

    /**
     * Constructor with custom font size.
     *
     * @param text The text to display.
     * @param fontSize The font size in pixels.
     * @param onClick The action to run when clicked.
     */
    public GameButton(String text, int fontSize, Runnable onClick) {
        this(text, onClick);
        setTextSize(fontSize);
    }

    /**
     * Customizes the button gradient colors.
     * @param startColorHex Hex code for start color (e.g. "#ff6b6b")
     * @param endColorHex Hex code for end color (e.g. "#ee5a5a")
     */
    public void setGradient(String startColorHex, String endColorHex) {
        this.gradientStart = startColorHex;
        this.gradientEnd = endColorHex;
        updateStyle();
    }

    /**
     * Sets a solid background color.
     * @param colorHex Hex code for the color (e.g. "#ff6b6b")
     */
    public void setColor(String colorHex) {
        this.gradientStart = colorHex;
        this.gradientEnd = colorHex;
        updateStyle();
    }
    
    /**
     * Customizes the shadow color for the drop shadow effect.
     * @param rgbaColor RGBA string for shadow (e.g. "rgba(255, 107, 107, 0.5)")
     */
    public void setShadowColor(String rgbaColor) {
        this.shadowColor = rgbaColor;
        updateStyle();
    }

    /**
     * Sets the font size of the button text.
     * @param size Font size in pixels
     */
    public void setTextSize(int size) {
        this.fontSize = size + "px";
        updateStyle();
    }

    /**
     * Sets the color of the button text.
     * @param colorHex Hex code or color name (e.g. "#FFFFFF", "black")
     */
    public void setTextColor(String colorHex) {
        this.textColor = colorHex;
        updateStyle();
    }

    /**
     * Sets custom padding for the button.
     * @param top Top padding
     * @param right Right padding
     * @param bottom Bottom padding
     * @param left Left padding
     */
    public void setPadding(int top, int right, int bottom, int left) {
        this.padding = String.format("%d %d %d %d", top, right, bottom, left);
        updateStyle();
    }

    /**
     * Sets the border color.
     * @param colorHex Color of the border (e.g. "black", "#000000")
     */
    public void setBorderColor(String colorHex) {
        this.borderColor = colorHex;
        updateStyle();
    }

    /**
     * Sets the border width.
     * @param width Width in pixels
     */
    public void setBorderWidth(int width) {
        this.borderWidth = width + "px";
        updateStyle();
    }

    /**
     * Sets the border radius (corner roundness).
     * @param radius Radius in pixels   
     */
    public void setBorderRadius(int radius) {
        this.borderRadius = String.valueOf(radius);
        updateStyle();
    }

    public void setOpacity(String opacity) {
        this.opacity = opacity;
        updateStyle();
    }
    
    public void setDisabledStyle(boolean disabled) {
        if (disabled) {
            setOpacity("0.5");
        } else {
            setOpacity("1");
        }
        setDisable(disabled);
    }

    private void initializeStyle() {
        // Base setup
        updateStyle();
    }

    private void updateStyle() {
        String style = String.format(
                "-fx-background-color: linear-gradient(to right, %s, %s); " +
                "-fx-opacity: %s; " +
            "-fx-text-fill: %s; " +
            "-fx-border-color: %s; " +
            "-fx-border-width: %s; " +
            "-fx-border-radius: %s; " +
            "-fx-background-radius: %s; " +
            "-fx-padding: %s; " +
            "-fx-font-size: %s; " +
            "-fx-font-weight: %s; " +
            "-fx-cursor: hand; " +
            "-fx-effect: dropshadow(gaussian, %s, 15, 0, 0, 5);",
            gradientStart, gradientEnd, opacity, textColor, borderColor, borderWidth, borderRadius, borderRadius, padding, fontSize, DEFAULT_FONT_WEIGHT, shadowColor
        );
        setStyle(style);
    }

    private void setupAnimations() {
        // Compute hover gradient (swapped direction or slightly lighter)
        // For simplicity, we just swap the gradient equivalent to MainMenu's hover style logic
        
        setOnMouseEntered(e -> {
            if (isDisabled()) return;
            
            setOpacity("0.8");

            ScaleTransition scale = new ScaleTransition(Duration.millis(150), this);
            scale.setToX(1.05);
            scale.setToY(1.05);
            scale.play();
        });

        setOnMouseExited(e -> {
            if (isDisabled()) return;
            
            setOpacity("1");
            ScaleTransition scale = new ScaleTransition(Duration.millis(150), this);
            scale.setToX(1.0);
            scale.setToY(1.0);
            scale.play();
        });

        setOnMousePressed(e -> {
            if (isDisabled()) return;
            
            ScaleTransition scale = new ScaleTransition(Duration.millis(100), this);
            scale.setToX(0.95);
            scale.setToY(0.95);
            scale.play();
        });

        setOnMouseReleased(e -> {
            if (isDisabled()) return;
            
            ScaleTransition scale = new ScaleTransition(Duration.millis(100), this);
            scale.setToX(1.05);
            scale.setToY(1.05);
            scale.play();
        });
    }
}
