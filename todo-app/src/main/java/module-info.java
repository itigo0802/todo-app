module jp.itigotti.module {
    requires javafx.controls;
    requires javafx.fxml;
    requires transitive javafx.base;
    requires transitive javafx.graphics;
    requires java.sql;

    opens jp.itigotti to javafx.fxml;
    exports jp.itigotti;
}
