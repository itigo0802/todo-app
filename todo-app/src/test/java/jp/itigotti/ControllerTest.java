package jp.itigotti;

import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.DatePicker;
import javafx.scene.control.DialogPane;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.ApplicationTest;
import org.testfx.framework.junit5.Start;
import org.testfx.util.WaitForAsyncUtils;

import java.nio.file.Path;
import java.time.LocalDate;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(ApplicationExtension.class)
public class ControllerTest extends ApplicationTest {

    @TempDir
    private Path tempDir;

    private Controller controller;
    private TodoDAO dao;

    @Start
    public void start(Stage stage) throws Exception {
        Path testDbPath = tempDir.resolve("test.db");
        String testDbUrl = "jdbc:sqlite:" + testDbPath.toAbsolutePath();
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
    void setUp(FxRobot robot) {
        controller.getTodoItems().clear();

        // ウィンドウ表示直後は、まだOSレベルの入力フォーカスを得ていないことがある。
        // その状態でボタンをロボットでクリックすると、ウィンドウマネージャに
        // 「フォーカスを合わせるだけ」の操作として吸収されてしまい、onActionが
        // 発火しないことがある（TextFieldへのクリックだと、クリック自体が吸収
        // されてもキーボード入力は結局そのフィールドに届くため気づきにくい）。
        // 各テストの本題に入る前に一度ダミーでクリックし、OSフォーカスを
        // ウィンドウに移しておく。
        robot.clickOn("#taskInput");
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

    @Test
    void handleDeleteAction_未選択の場合エラーが表示され何も削除されない(FxRobot robot) throws TimeoutException {
        // UIを経由せず、ロジック層を直接呼び出してデータを準備する。
        // 削除機能のテストにとって「追加」操作は前提条件でしかないため、
        // ここをUI操作で行うとテストの意図がぼやけてしまう。
        robot.interact(() -> controller.getLogic().addTodoItem("削除されないタスク", LocalDate.now()));

        // 何も選択せずに削除ボタンを押す
        robot.clickOn("選択項目を削除");

        // Alert.showAndWait() はFXスレッド上でネストしたイベントループに入って
        // ダイアログを表示するため、robot.clickOn() が「FXイベントキューが空になった」
        // と判断するタイミングと、実際にダイアログが画面に現れるタイミングがずれることが
        // ある。ダイアログが見つかるまで明示的にポーリングして待つ。
        WaitForAsyncUtils.waitFor(5, TimeUnit.SECONDS,
                () -> robot.lookup(".dialog-pane").tryQuery().isPresent());

        // Alertはモーダルダイアログとして別ウィンドウで表示されるが、
        // FxRobotはアクティブな全ウィンドウを対象に検索できるので、
        // ".dialog-pane" というAlert共通のCSSクラスでルックアップできる。
        DialogPane dialogPane = robot.lookup(".dialog-pane").queryAs(DialogPane.class);
        assertEquals("削除するTodoを選択してください", dialogPane.getContentText());

        // Alert.showAndWait() はダイアログが閉じられるまでネストしたイベント
        // ループでブロックし続ける。閉じないまま次のテストに進むとFXスレッドが
        // 詰まるため、OKボタンをクリックして必ず閉じる。
        robot.clickOn("OK");

        assertEquals(1, controller.getTodoItems().size());
    }

    @Test
    void handleDeleteAction_選択したTodoが削除される(FxRobot robot) {
        robot.interact(() -> controller.getLogic().addTodoItem("削除されるタスク", LocalDate.now()));
        TodoItemModel target = controller.getTodoItems().get(0);
        TableView<TodoItemModel> todoListView = controller.getTodoListView();

        // TODO(human): 準備した target を todoListView 上で選択状態にしたうえで、
        // 「選択項目を削除」ボタンをクリックし、削除結果を検証すること。
        //
        // ヒント:
        // - TableView の選択は todoListView.getSelectionModel().select(target) で行える。
        //   ただしJavaFXのUI状態を変更する操作なので、robot.interact(() -> ...) で
        //   FXスレッド上で実行する（上の addTodoItem の呼び出し方が参考になる）。
        // - 削除ボタンのクリックは他のテストと同様 robot.clickOn("選択項目を削除") でよい。
        // - 検証は最低限 controller.getTodoItems() から target が消えたことを確認する。
        //   余力があれば dao.findById(...) などでDB側からも消えていることまで
        //   確認すると、TodoListLogic.removeTodoItem がDAOとリストの両方を
        //   更新していることを裏付けられる（TodoDAOのメソッド一覧を見てみてほしい）。
        robot.interact(() -> todoListView.getSelectionModel().select(target));
        robot.clickOn("選択項目を削除");

        assertFalse(controller.getTodoItems().contains(target));
    }
}
