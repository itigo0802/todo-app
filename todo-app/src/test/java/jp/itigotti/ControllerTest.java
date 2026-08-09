package jp.itigotti;

import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.DatePicker;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.ApplicationTest;
import org.testfx.framework.junit5.Start;

import java.nio.file.Path;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class ControllerTest extends ApplicationTest {

    @TempDir
    private Path tempDir;

    private Controller controller;
    private TodoDAO dao;

    @Start
    public void start(Stage stage) throws Exception {
        Path testDbPath = tempDir.resolve("test.db");
        String testDbUrl = "jdbc:sqlite:" + testDbPath.toAbsolutePath().toString();
        dao = new TodoDAO(testDbUrl);
        dao.initializeDB();

        controller = new Controller(dao);
        FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/jp/itigotti/scene.fxml")
        );
        loader.setControllerFactory(clazz -> controller);
        VBox root = loader.load();

        Scene scene = new Scene(root);
        stage.setScene(scene);
        stage.show();
    }

    @BeforeEach
    void setUp() {
        controller.getTodoItems().clear();
    }

    @AfterEach
    void tearDown() throws Exception {
        if(dao != null) {
            dao.close();
        }
    }

    @Test
    void handleAddAction_正常な入力でタスクが追加される(FxRobot robot) {
        String taskName = "テストタスク";
        LocalDate dueDate = LocalDate.now().plusDays(1);

        robot.clickOn("#taskInput").write(taskName);

        robot.clickOn("#expirationDatePicker").write(dueDate.toString());

        robot.clickOn("追加");

        assertEquals(1, controller.getTodoItems().size());

        TodoItemModel addedItem = controller.getTodoItems().get(0);
        assertEquals(taskName, addedItem.getTask());
        assertEquals(dueDate, addedItem.getExpirationDate());
    }

    @Test
    void handleAddAction_追加後に入力フィールドがクリアされる(FxRobot robot) {
        robot.clickOn("#taskInput").write("テストタスク");
        robot.clickOn("#expirationDatePicker").write(LocalDate.now().toString());

        robot.clickOn("追加");

        TextField taskInput = robot.lookup("#taskInput").queryAs(TextField.class);
        assertEquals("", taskInput.getText());

        DatePicker datePicker = robot.lookup("#expirationDatePicker").queryAs(DatePicker.class);
        assertNull(datePicker.getValue());
    }
}
