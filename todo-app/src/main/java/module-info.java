module jp.itigotti.module {
    requires javafx.controls;
    requires javafx.fxml;
    requires transitive javafx.base;
    requires transitive javafx.graphics;
    requires java.sql;
    requires org.xerial.sqlitejdbc;
    requires org.slf4j;
    requires ch.qos.logback.classic;

    opens jp.itigotti to javafx.fxml;

    exports jp.itigotti;
}
