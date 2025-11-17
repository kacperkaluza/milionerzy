module com.kaluzaplotecka.milionerzy {
    requires javafx.controls;
    requires javafx.fxml;

    requires com.almasb.fxgl.all;
    requires com.almasb.fxgl.entity;

    opens com.kaluzaplotecka.milionerzy to javafx.fxml;
    exports com.kaluzaplotecka.milionerzy;
}