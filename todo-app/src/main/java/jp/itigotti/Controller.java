package jp.itigotti;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Optional;

import javafx.collections.ObservableList;
import javafx.scene.control.*;
import javafx.util.StringConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javafx.fxml.FXML;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.control.cell.TextFieldTableCell;

public class Controller {
    @FXML
    private TextField taskInput;
    @FXML
    private DatePicker expirationDatePicker;
    @FXML
    private TableView<TodoItemModel> todoListView;
    @FXML
    private TableColumn<TodoItemModel, String> taskColumn;
    @FXML
    private TableColumn<TodoItemModel, LocalDate> expirationColumn;
    @FXML
    private TableColumn<TodoItemModel, Boolean> isCompletedColumn;

    private final TodoListLogic logic;
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy/MM/dd");
    private static final DateTimeFormatter[] INPUT_FORMATTERS = {
            DateTimeFormatter.ISO_LOCAL_DATE,
            FORMATTER,
    };
    private static final Logger log = LoggerFactory.getLogger(Controller.class);

    // expirationDatePicker（自由入力欄）と expirationColumn（テーブルの編集セル）の
    // 両方で「yyyy-MM-dd / yyyy/MM/dd を受け付け、表示はyyyy/MM/dd」という同じ変換規則を
    // 使うため、StringConverterを1つのフィールドに集約して使い回す。
    private final StringConverter<LocalDate> dateConverter = new StringConverter<>() {
        @Override
        public String toString(LocalDate date) {
            // date が null の場合、DatePicker/TextFieldTableCell は空欄を表す null を渡してくる。
            // null を FORMATTER.format() に渡すと例外になるので、null なら "" を返す。
            if(date == null) {
                return "";
            } else {
                return date.format(FORMATTER);
            }
        }

        @Override
        public LocalDate fromString(String text) {
            return parseFlexibleDate(text);
        }
    };

    public Controller(TodoDAO dao) {
        this.logic = new TodoListLogic(dao);
    }

    public ObservableList<TodoItemModel> getTodoItems() {
        return logic.getTodoItems();
    }

    public TableView<TodoItemModel> getTodoListView() {
        return todoListView;
    }

    @FXML
    private void initialize() {
        todoListView.setItems(logic.getTodoItems());
        logic.refresh();

        todoListView.setEditable(true);

        taskColumn.setCellValueFactory(cellData -> cellData.getValue().taskProperty());
        taskColumn.setCellFactory(TextFieldTableCell.forTableColumn());
        taskColumn.setEditable(true);

        expirationColumn.setCellValueFactory(cellData -> cellData.getValue().expirationDateProperty());
        expirationColumn.setCellFactory(TextFieldTableCell.forTableColumn(dateConverter));
        expirationColumn.setEditable(true);

        isCompletedColumn.setCellValueFactory(cellData -> cellData.getValue().completedProperty());
        isCompletedColumn.setCellFactory(CheckBoxTableCell.forTableColumn(isCompletedColumn));
        isCompletedColumn.setEditable(true);

        // DatePicker はデフォルトだと、フォーカスが外れた瞬間にロケール依存の書式
        // （例: M/d/yy）でエディタの文字列をパースしようとし、失敗すると入力内容を
        // 空文字に巻き戻してしまう。そのため "2026-08-11" のような ISO 形式で入力すると
        // ボタンを押した時点（＝フォーカスロスト時）でテキストが消えてしまっていた。
        // ここで dateConverter を設定し、INPUT_FORMATTERS で使っている書式を
        // DatePicker自身にも認識させる。
        expirationDatePicker.setConverter(dateConverter);

        // TODO(human): taskColumn・expirationColumnのセルを編集し確定した（Enterやフォーカス
        // ロストで編集を終えた）ときに、入力値を検証してからTodoItemModelへ反映する処理を
        // 実装する。モデルのプロパティ（taskProperty/expirationDateProperty）を更新すれば、
        // 上のTodoListLogic.setupItemListener()が変更を検知して自動的にDBへ保存してくれる
        // （completedPropertyの仕組みと同じ）ので、ここではモデルを正しく更新することだけを
        // 考えればよい。
        //
        // taskColumn.setOnEditCommit(event -> { ... });
        // expirationColumn.setOnEditCommit(event -> { ... });
        //
        // ヒント:
        // - event.getRowValue() で編集対象のTodoItemModel、event.getNewValue() で
        //   編集後の値（Stringまたはparseされた LocalDate）が取れる。
        // - taskColumn側: 空文字・空白のみの入力をどう扱うか（handleAddActionのバリデーション
        //   と同様、許可しない方が自然）。
        // - expirationColumn側: dateConverter#fromString()（=parseFlexibleDate()）は
        //   パースできない文字列に対してnullを返す。event.getNewValue()がnullになりうる
        //   ことに注意。
        // - 不正な入力を弾く場合、item.setTask()/setExpirationDate()を呼ばなければ
        //   プロパティは変化せず、DBへも保存されない。ユーザーに知らせたい場合はAlertを
        //   出す方法もある（handleAddActionのエラーAlertが参考になる）。
        taskColumn.setOnEditCommit(event -> {
            TodoItemModel editItem = event.getRowValue();
            String task = event.getNewValue();

            if(task.isBlank()) {
                log.debug("入力バリデーションエラー タスクが入力されていません");

                var alert = new Alert(AlertType.ERROR);
                alert.setTitle("入力エラー");
                alert.setHeaderText(null);
                alert.setContentText("タスクが入力されていません");
                alert.showAndWait();
                todoListView.refresh();
                return;
            }

            editItem.setTask(task);
        });

        expirationColumn.setOnEditCommit(event -> {
            TodoItemModel editItem = event.getRowValue();
            LocalDate expirationDate = event.getNewValue();

            if (expirationDate == null) {
                log.debug("入力バリデーションエラー 期限の形式が正しくない");

                var alert = new Alert(AlertType.ERROR);
                alert.setTitle("入力エラー");
                alert.setHeaderText(null);
                alert.setContentText("期限の形式が正しくありません。yyyy-MM-dd または yyyy/MM/dd で入力してください");
                alert.showAndWait();
                todoListView.refresh();
                return;
            }

            editItem.setExpirationDate(expirationDate);
        });
    }

    @FXML
    private void handleAddAction() {
        try {
            LocalDate expirationDate = resolveExpirationDate();
            logic.addTodoItem(taskInput.getText(), expirationDate);
            taskInput.clear();
            expirationDatePicker.setValue(null);
        } catch (IllegalArgumentException e) {
            log.debug("入力バリデーションエラー message={}", e.getMessage());

            var alert = new Alert(AlertType.ERROR);
            alert.setTitle("入力エラー");
            alert.setHeaderText(null);
            alert.setContentText(e.getMessage());
            alert.showAndWait();
        }
    }

    private LocalDate resolveExpirationDate() {
        LocalDate selectedDate = expirationDatePicker.getValue();
        if(selectedDate != null) {
            return selectedDate;
        }

        String text = expirationDatePicker.getEditor().getText();
        if (text == null || text.isBlank()) {
            // 何も入力されていない場合はnullを返し、logic.addTodoItem()側の
            // 「期限が入力されていません」というメッセージに委ねる。
            // ここで形式エラーとして扱うと、未入力なだけなのに
            // 「形式が正しくありません」という誤ったメッセージになってしまう。
            return null;
        }

        // ここに到達した時点で、DatePickerはgetValue()がnullを返している。
        // 「追加」ボタンのクリックでフォーカスが移る際、DatePicker自身の
        // StringConverter#fromString()（= parseFlexibleDate()）が既に同じ
        // テキストのパースを試みているはずで、それでもgetValue()がnullという
        // ことは、そのパースが失敗したということ。parseFlexibleDate()は
        // 副作用のない純粋関数なので、同じテキストをもう一度渡しても
        // 結果は変わらない。よってここでは再パースせずに形式エラーとして扱う。
        throw new IllegalArgumentException("期限の形式が正しくありません。yyyy-MM-dd または yyyy/MM/dd で入力してください");
    }

    @FXML
    private void handleDeleteAction() {
        TodoItemModel selected = todoListView.getSelectionModel().getSelectedItem();
        if (selected != null) {
            var confirm = new Alert(AlertType.CONFIRMATION);
            confirm.setTitle("削除の確認");
            confirm.setHeaderText(null);
            confirm.setContentText("「" + selected.getTask() + "」を削除しますか？");

            // ×で閉じた場合はOptional.empty()になるため、OKが明示的に選ばれた場合のみ削除する。
            Optional<ButtonType> result = confirm.showAndWait();
            if (result.isPresent() && result.get() == ButtonType.OK) {
                logic.removeTodoItem(selected);
            }
        } else {
            var alert = new Alert(AlertType.ERROR);
            alert.setTitle("削除エラー");
            alert.setHeaderText(null);
            alert.setContentText("削除するTodoを選択してください");
            alert.showAndWait();
        }
    }

    private LocalDate parseFlexibleDate(String text) {
        for (DateTimeFormatter formatter : INPUT_FORMATTERS) {
            try {
                return LocalDate.parse(text, formatter);
            } catch (DateTimeParseException e) {
                // このフォーマッタでは解釈できなかっただけなので、次のフォーマッタを試す。
                // 全フォーマッタが失敗した場合はループを抜けてnullを返す。
            }
        }
        return null;
    }
}
