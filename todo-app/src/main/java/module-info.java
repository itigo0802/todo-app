module jp.itigotti {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;

    opens jp.itigotti to javafx.fxml;
    exports jp.itigotti;
}
