package jp.itigotti;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

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
	private static final Logger LOGGER = System.getLogger(Controller.class.getName());

	@FXML private TextField taskInput;
	@FXML private DatePicker expirationDatePicker;
	@FXML private TableView<TodoItemModel> todoListView;
	@FXML private TableColumn<TodoItemModel, String> taskColumn;
	@FXML private TableColumn<TodoItemModel, LocalDate> expirationColumn;
	@FXML private TableColumn<TodoItemModel, Boolean> isCompletedColumn;
	
	private final TodoListLogic logic = new TodoListLogic();

	private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy/MM/dd");
	
	@FXML
	 private void initialize() {
		todoListView.setItems(logic.getTodoItems());
		logic.setErrorHandler(e -> showError("更新エラー", e));
		todoListView.setEditable(true);

		taskColumn.setCellValueFactory(cellData -> cellData.getValue().taskProperty());

		expirationColumn.setCellValueFactory(cellData -> cellData.getValue().expirationDateProperty());
		expirationColumn.setCellFactory(column -> new TableCell<>() {
			@Override
			protected void updateItem(LocalDate item, boolean empty) {
				super.updateItem(item, empty);
				if(empty || item == null) {
					setText(null);
				} else {
					setText(FORMATTER.format(item));
				}
			}
		});

		isCompletedColumn.setCellValueFactory(cellData -> cellData.getValue().completedProperty());
		isCompletedColumn.setCellFactory(CheckBoxTableCell.forTableColumn(isCompletedColumn));
		isCompletedColumn.setEditable(true);

		try {
			logic.refresh();
		} catch(DataAccessException e) {
			showError("読み込みエラー", e);
		}
	}
	
	@FXML
	private void handleAddAction() {
		try {
			logic.addTodoItem(taskInput.getText(), expirationDatePicker.getValue());
			taskInput.clear();
			expirationDatePicker.setValue(null);
		} catch(IllegalArgumentException e) {
			showAlert(AlertType.ERROR, "入力エラー", e.getMessage());
		} catch(DataAccessException e) {
			showError("登録エラー", e);
		}
	}
	
	@FXML
	private void handleDeleteAction() {
		TodoItemModel selected = todoListView.getSelectionModel().getSelectedItem();
		if(selected == null) {
			showAlert(AlertType.WARNING, "削除エラー", "削除する項目が選択されていません");
			return;
		}

		try {
			logic.removeTodoItem(selected);
		} catch(DataAccessException e) {
			showError("削除エラー", e);
		}
	}

	private void showError(String title, RuntimeException e) {
		LOGGER.log(Level.ERROR, title + ": " + e.getMessage(), e);
		showAlert(AlertType.ERROR, title, e.getMessage());
	}

	private void showAlert(AlertType type, String title, String message) {
		var alert = new Alert(type);
		alert.setTitle(title);
		alert.setHeaderText(null);
		alert.setContentText(message);
		alert.showAndWait();
	}
}
