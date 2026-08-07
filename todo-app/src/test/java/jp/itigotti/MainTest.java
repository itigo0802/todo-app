package jp.itigotti;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.testfx.framework.junit5.ApplicationTest;
import static org.testfx.api.FxAssert.verifyThat;
import static org.testfx.matcher.base.NodeMatchers.isVisible;

import java.nio.file.Path;

import javafx.stage.Stage;

public class MainTest extends ApplicationTest {

    @TempDir
    Path tempDir;
    
    @Override
    public void start(Stage stage) throws Exception {
        String tempDbUrl = "jdbc:sqlite:" + tempDir.resolve("test.db").toString();

        TodoDAO testDao = new TodoDAO(tempDbUrl);
        testDao.initializeDB();

        Main main = new Main();
        main.setDao(testDao);
        main.start(stage);
    }

    @Test
    void testApplicationLaunch() {
        verifyThat("#taskInput", isVisible());
        verifyThat("#expirationDatePicker", isVisible());
        verifyThat("#todoListView", isVisible());
    }
}
