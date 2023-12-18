module com.example.avanceradjavaadambarnellslutprojekt {
    requires javafx.controls;
    requires javafx.fxml;

    requires org.controlsfx.controls;
    requires org.kordamp.bootstrapfx.core;
    requires java.net.http;
    requires minimal.json;

    opens com.example.avanceradjavaadambarnellslutprojekt to javafx.fxml;
    exports com.example.avanceradjavaadambarnellslutprojekt;
}