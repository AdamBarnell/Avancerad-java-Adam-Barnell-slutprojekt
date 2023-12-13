module com.example.avanceradjavaadambarnellslutprojekt {
    requires javafx.controls;
    requires javafx.fxml;

    requires org.controlsfx.controls;
    requires org.kordamp.bootstrapfx.core;

    opens com.example.avanceradjavaadambarnellslutprojekt to javafx.fxml;
    exports com.example.avanceradjavaadambarnellslutprojekt;
}