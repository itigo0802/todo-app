package jp.itigotti;

import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.ButtonType;
import javafx.scene.control.DatePicker;
import javafx.scene.control.DialogPane;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.testfx.framework.junit5.ApplicationTest;
import org.testfx.util.WaitForAsyncUtils;

import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.*;

public class ControllerTest extends ApplicationTest {

    // expirationColumnのセルは、Controller側のdateConverterと同じ"yyyy/MM/dd"書式で表示される。
    // 編集開始のためにセルをダブルクリックする際、表示中のテキストでルックアップする必要がある。
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy/MM/dd");

    @TempDir
    private Path tempDir;

    private Controller controller;
    private TodoDAO dao;

    @Override
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
    void setUp() {
        // TableViewが直接バインドしているObservableListなので、他の箇所と同様に
        // interact()でFXアプリケーションスレッド上から操作する。
        interact(() -> controller.getTodoItems().clear());

        // ウィンドウ表示直後は、まだOSレベルの入力フォーカスを得ていないことがある。
        // その状態でボタンをロボットでクリックすると、ウィンドウマネージャに
        // 「フォーカスを合わせるだけ」の操作として吸収されてしまい、onActionが
        // 発火しないことがある（TextFieldへのクリックだと、クリック自体が吸収
        // されてもキーボード入力は結局そのフィールドに届くため気づきにくい）。
        // 各テストの本題に入る前に一度ダミーでクリックし、OSフォーカスを
        // ウィンドウに移しておく。
        clickOn("#taskInput");
    }

    @AfterEach
    void tearDown() throws Exception {
        if(dao != null) {
            dao.close();
        }
    }

    @Test
    void handleAddAction_正常な入力でタスクが追加される() {
        String taskName = "テストタスク";
        LocalDate dueDate = LocalDate.now().plusDays(1);

        clickOn("#taskInput").write(taskName);

        clickOn("#expirationDatePicker").write(dueDate.toString());

        clickOn("追加");

        assertEquals(1, controller.getTodoItems().size());

        TodoItemModel addedItem = controller.getTodoItems().get(0);
        assertEquals(taskName, addedItem.getTask());
        assertEquals(dueDate, addedItem.getExpirationDate());
    }

    @Test
    void handleAddAction_追加後に入力フィールドがクリアされる() {
        clickOn("#taskInput").write("テストタスク");
        clickOn("#expirationDatePicker").write(LocalDate.now().toString());

        clickOn("追加");

        TextField taskInput = lookup("#taskInput").queryAs(TextField.class);
        assertEquals("", taskInput.getText());

        DatePicker datePicker = lookup("#expirationDatePicker").queryAs(DatePicker.class);
        assertNull(datePicker.getValue());
        assertEquals("", datePicker.getEditor().getText());
    }

    @Test
    void handleAddAction_期限の形式が不正な場合エラーが表示され追加されない() throws TimeoutException {
        clickOn("#taskInput").write("テストタスク");
        clickOn("#expirationDatePicker").write("not-a-date");

        clickOn("追加");

        WaitForAsyncUtils.waitFor(5, TimeUnit.SECONDS,
                () -> lookup(".dialog-pane").tryQuery().isPresent());

        DialogPane dialogPane = lookup(".dialog-pane").queryAs(DialogPane.class);
        assertEquals("期限の形式が正しくありません。yyyy-MM-dd または yyyy/MM/dd で入力してください",
                dialogPane.getContentText());
        clickOn("OK");

        assertEquals(0, controller.getTodoItems().size());
    }

    @Test
    void handleDeleteAction_未選択の場合エラーが表示され何も削除されない() throws TimeoutException {
        clickOn("#taskInput").write("削除されないタスク");
        clickOn("#expirationDatePicker").write(LocalDate.now().toString());
        clickOn("追加");

        // 何も選択せずに削除ボタンを押す
        clickOn("選択項目を削除");

        // Alert.showAndWait() はFXスレッド上でネストしたイベントループに入って
        // ダイアログを表示するため、clickOn() が「FXイベントキューが空になった」
        // と判断するタイミングと、実際にダイアログが画面に現れるタイミングがずれることが
        // ある。ダイアログが見つかるまで明示的にポーリングして待つ。
        WaitForAsyncUtils.waitFor(5, TimeUnit.SECONDS,
                () -> lookup(".dialog-pane").tryQuery().isPresent());

        // Alertはモーダルダイアログとして別ウィンドウで表示されるが、
        // FxRobotはアクティブな全ウィンドウを対象に検索できるので、
        // ".dialog-pane" というAlert共通のCSSクラスでルックアップできる。
        DialogPane dialogPane = lookup(".dialog-pane").queryAs(DialogPane.class);
        assertEquals("削除するTodoを選択してください", dialogPane.getContentText());

        // Alert.showAndWait() はダイアログが閉じられるまでネストしたイベント
        // ループでブロックし続ける。閉じないまま次のテストに進むとFXスレッドが
        // 詰まるため、OKボタンをクリックして必ず閉じる。
        clickOn("OK");

        assertEquals(1, controller.getTodoItems().size());
    }

    @Test
    void handleDeleteAction_確認ダイアログでOKを選ぶと削除される() throws TimeoutException {
        clickOn("#taskInput").write("削除されるタスク");
        clickOn("#expirationDatePicker").write(LocalDate.now().toString());
        clickOn("追加");

        TodoItemModel target = controller.getTodoItems().get(0);
        TableView<TodoItemModel> todoListView = controller.getTodoListView();

        // TableView上でtargetを選択状態にしたうえで削除ボタンを押す。
        // UI状態を変更する操作なのでinteract()でFXスレッド上で実行する。
        interact(() -> todoListView.getSelectionModel().select(target));
        clickOn("選択項目を削除");

        WaitForAsyncUtils.waitFor(5, TimeUnit.SECONDS,
                () -> lookup(".dialog-pane").tryQuery().isPresent());
        // ButtonTypeの表示ラベルは実行環境のロケールに応じてJavaFXの標準リソース
        // バンドルが決める（例: ja_JPなら「キャンセル」、Cやen_USなら"Cancel"）。CI環境と
        // この開発機とでロケールが異なりうるので、決め打ち文字列ではなく
        // ButtonType#getText()（実行時のロケールで解決された文字列）でルックアップする。
        clickOn(ButtonType.OK.getText());

        assertFalse(controller.getTodoItems().contains(target));
    }

    @Test
    void handleDeleteAction_確認ダイアログでキャンセルすると削除されない() throws TimeoutException {
        clickOn("#taskInput").write("削除されないタスク");
        clickOn("#expirationDatePicker").write(LocalDate.now().toString());
        clickOn("追加");

        TodoItemModel target = controller.getTodoItems().get(0);
        TableView<TodoItemModel> todoListView = controller.getTodoListView();

        interact(() -> todoListView.getSelectionModel().select(target));
        clickOn("選択項目を削除");

        WaitForAsyncUtils.waitFor(5, TimeUnit.SECONDS,
                () -> lookup(".dialog-pane").tryQuery().isPresent());
        clickOn(ButtonType.CANCEL.getText());

        assertTrue(controller.getTodoItems().contains(target));
    }

    @Test
    void taskColumnEdit_正常な値に変更するとDBに反映される() {
        String originalTask = "編集前タスク";
        clickOn("#taskInput").write(originalTask);
        clickOn("#expirationDatePicker").write(LocalDate.now().toString());
        clickOn("追加");

        TodoItemModel target = controller.getTodoItems().get(0);

        // セルのダブルクリックで編集モードに入る。編集開始時、TextFieldTableCellは
        // 既存のテキストを選択済みの状態にするため、そのままwrite()すれば上書きできる。
        doubleClickOn(originalTask);
        write("編集後タスク");
        push(KeyCode.ENTER);

        assertEquals("編集後タスク", target.getTask());
        assertEquals("編集後タスク", dao.findAll().get(0).getTask());
    }

    @Test
    void taskColumnEdit_空文字にするとエラーが表示され変更されない() throws TimeoutException {
        String originalTask = "編集前タスク";
        clickOn("#taskInput").write(originalTask);
        clickOn("#expirationDatePicker").write(LocalDate.now().toString());
        clickOn("追加");

        TodoItemModel target = controller.getTodoItems().get(0);

        doubleClickOn(originalTask);
        // 編集開始時にテキストは全選択された状態になっているので、BackSpace一発で空にできる。
        push(KeyCode.BACK_SPACE);
        push(KeyCode.ENTER);

        WaitForAsyncUtils.waitFor(5, TimeUnit.SECONDS,
                () -> lookup(".dialog-pane").tryQuery().isPresent());
        DialogPane dialogPane = lookup(".dialog-pane").queryAs(DialogPane.class);
        assertEquals("タスクが入力されていません", dialogPane.getContentText());
        clickOn(ButtonType.OK.getText());

        assertEquals(originalTask, target.getTask());
    }

    @Test
    void expirationColumnEdit_正常な値に変更するとDBに反映される() {
        LocalDate originalDate = LocalDate.now();
        LocalDate newDate = LocalDate.now().plusDays(3);
        clickOn("#taskInput").write("期限編集テスト");
        clickOn("#expirationDatePicker").write(originalDate.toString());
        clickOn("追加");

        TodoItemModel target = controller.getTodoItems().get(0);

        doubleClickOn(DATE_FORMATTER.format(originalDate));
        write(newDate.toString());
        push(KeyCode.ENTER);

        assertEquals(newDate, target.getExpirationDate());
        assertEquals(newDate, dao.findAll().get(0).getExpirationDate());
    }

    @Test
    void expirationColumnEdit_不正な形式にするとエラーが表示され変更されない() throws TimeoutException {
        LocalDate originalDate = LocalDate.now();
        clickOn("#taskInput").write("期限編集テスト");
        clickOn("#expirationDatePicker").write(originalDate.toString());
        clickOn("追加");

        TodoItemModel target = controller.getTodoItems().get(0);

        doubleClickOn(DATE_FORMATTER.format(originalDate));
        push(KeyCode.BACK_SPACE);
        write("not-a-date");
        push(KeyCode.ENTER);

        WaitForAsyncUtils.waitFor(5, TimeUnit.SECONDS,
                () -> lookup(".dialog-pane").tryQuery().isPresent());
        DialogPane dialogPane = lookup(".dialog-pane").queryAs(DialogPane.class);
        assertEquals("期限の形式が正しくありません。yyyy-MM-dd または yyyy/MM/dd で入力してください",
                dialogPane.getContentText());
        clickOn(ButtonType.OK.getText());

        assertEquals(originalDate, target.getExpirationDate());
    }
}
