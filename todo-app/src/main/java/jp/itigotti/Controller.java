package jp.itigotti;

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
		logic.refresh();
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
	}
	
	@FXML
	private void handleAddAction() {
		try {
			logic.addTodoItem(taskInput.getText(), expirationDatePicker.getValue());
			taskInput.clear();
			expirationDatePicker.setValue(null);
		} catch(IllegalArgumentException e) {
			var alert = new Alert(AlertType.ERROR);
			alert.setTitle("入力エラー");
			alert.setHeaderText(null);
			alert.setContentText(e.getMessage());
			alert.showAndWait();
		}
	}
	
	@FXML
	private void handleDeleteAction() {
		TodoItemModel selected = todoListView.getSelectionModel().getSelectedItem();
		if(selected != null) {
			logic.removeTodoItem(selected);
		}
	}
}
