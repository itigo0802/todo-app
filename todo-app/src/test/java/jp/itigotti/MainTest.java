package jp.itigotti;

import org.junit.jupiter.api.Test;
import org.testfx.framework.junit5.ApplicationTest;
import static org.testfx.api.FxAssert.verifyThat;
import static org.testfx.matcher.base.NodeMatchers.isVisible;

import javafx.stage.Stage;

public class MainTest extends ApplicationTest {
    
    @Override
    public void start(Stage stage) throws Exception {
        Main main = new Main();
        main.init();
        main.start(stage);
    }

    @Test
    void testApplicationLaunch() {
        verifyThat("#taskInput", isVisible());
        verifyThat("#expirationDatePicker", isVisible());
        verifyThat("#todoListView", isVisible());
    }
}
