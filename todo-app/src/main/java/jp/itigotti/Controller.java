package jp.itigotti;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

import javafx.collections.ObservableList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.DatePicker;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
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
			DateTimeFormatter.ofPattern("yyyy/MM/dd"),
	};
	private static final Logger log = LoggerFactory.getLogger(Controller.class);

	public Controller(TodoDAO dao) {
		this.logic = new TodoListLogic(dao);
	}

	public ObservableList<TodoItemModel> getTodoItems() {
		return logic.getTodoItems();
	}

	public TodoListLogic getLogic() {
		return logic;
	}

	public TextField getTaskInput() {
		return taskInput;
	}

	public DatePicker getExpirationDatePicker() {
		return expirationDatePicker;
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
	}

	@FXML
	private void handleAddAction() {
		try {
			LocalDate expirationDate = resolveExpirationDate();
			logic.addTodoItem(taskInput.getText(), expirationDate);
			taskInput.clear();
			expirationDatePicker.setValue(null);
			expirationDatePicker.getEditor().clear();
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

		String text =expirationDatePicker.getEditor().getText();
		for (DateTimeFormatter formatter : INPUT_FORMATTERS) {
			try {
				LocalDate parsedDate = LocalDate.parse(text, formatter);
				expirationDatePicker.setValue(parsedDate);
				return parsedDate;
			} catch (DateTimeParseException e) {
				// 次のフォーマットを試す
			}
		}

		throw new IllegalArgumentException("期限の形式が正しくありません。yyyy-MM-dd または yyyy/MM/dd で入力してください");
	}

	@FXML
	private void handleDeleteAction() {
		TodoItemModel selected = todoListView.getSelectionModel().getSelectedItem();
		if (selected != null) {
			logic.removeTodoItem(selected);
		} else {
			var alert = new Alert(AlertType.ERROR);
			alert.setTitle("削除エラー");
			alert.setHeaderText(null);
			alert.setContentText("削除するTodoを選択してください");
			alert.showAndWait();
		}
	}
}
