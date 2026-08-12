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

        expirationColumn.setCellValueFactory(cellData -> cellData.getValue().expirationDateProperty());
        expirationColumn.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(LocalDate item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(FORMATTER.format(item));
                }
            }
        });

        isCompletedColumn.setCellValueFactory(cellData -> cellData.getValue().completedProperty());
        isCompletedColumn.setCellFactory(CheckBoxTableCell.forTableColumn(isCompletedColumn));
        isCompletedColumn.setEditable(true);

        // DatePicker はデフォルトだと、フォーカスが外れた瞬間にロケール依存の書式
        // （例: M/d/yy）でエディタの文字列をパースしようとし、失敗すると入力内容を
        // 空文字に巻き戻してしまう。そのため "2026-08-11" のような ISO 形式で入力すると
        // ボタンを押した時点（＝フォーカスロスト時）でテキストが消えてしまっていた。
        // ここで独自の StringConverter を設定し、INPUT_FORMATTERS で使っている書式を
        // DatePicker自身にも認識させる。
        expirationDatePicker.setConverter(new StringConverter<LocalDate>() {
            @Override
            public String toString(LocalDate date) {
                // date が null の場合、DatePicker は空欄を表す null を渡してくる。
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

            // TODO(human): confirm.showAndWait() を呼び、ユーザーがOKボタンを押した場合だけ
            // logic.removeTodoItem(selected) を呼ぶように実装する。
            //
            // ヒント:
            // - Alert#showAndWait() は Optional<ButtonType> を返す。デフォルトの
            //   AlertType.CONFIRMATION には ButtonType.OK と ButtonType.CANCEL が
            //   自動で用意される（ダイアログを×で閉じた場合はOptional.empty()になる）。
            // - Optional の中身を確認する書き方は複数ある。例えば
            //   result.isPresent() && result.get() == ButtonType.OK のような素朴な判定でも、
            //   result.filter(bt -> bt == ButtonType.OK).isPresent() のような書き方でもよい。
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
