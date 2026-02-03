module com.kaluzaplotecka.milionerzy {
    requires javafx.controls;
    requires javafx.fxml;
    requires transitive javafx.graphics;
    requires javafx.media;


    requires com.almasb.fxgl.all;
    requires com.almasb.fxgl.entity;

    opens com.kaluzaplotecka.milionerzy to javafx.fxml;

    exports com.kaluzaplotecka.milionerzy;
    exports com.kaluzaplotecka.milionerzy.model;
    exports com.kaluzaplotecka.milionerzy.view;
    exports com.kaluzaplotecka.milionerzy.events;
    exports com.kaluzaplotecka.milionerzy.network;
    exports com.kaluzaplotecka.milionerzy.model.tiles;
    exports com.kaluzaplotecka.milionerzy.model.cards;
}