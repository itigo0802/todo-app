module jp.itigotti.module {
    requires javafx.controls;
    requires javafx.fxml;
    requires transitive javafx.base;
    requires transitive javafx.graphics;
    requires java.sql;
    requires org.xerial.sqlitejdbc;

    opens jp.itigotti to javafx.fxml;

    exports jp.itigotti;
}
