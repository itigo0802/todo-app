package jp.itigotti;

import javafx.fxml.FXML;
import javafx.scene.control.DatePicker;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;

public class Controller {
	@FXML private TextField taskInput;
	@FXML private DatePicker expirationDatePicker;
	@FXML private ListView<TodoItemModel> todoListView;
	
	private final TodoListLogic logic = new TodoListLogic();
	
	@FXML
	 private void initialize() {
		todoListView.setItems(logic.getTodoItems());

		todoListView.setCellFactory(param -> new TodoCell());
	}
	
	@FXML
	private void handleAddAction() {
		logic.addTodoItem(taskInput.getText(), expirationDatePicker.getValue());
		taskInput.clear();
		expirationDatePicker.setValue(null);
	}
	
	@FXML
	private void handleDeleteAction() {
		TodoItemModel selected = todoListView.getSelectionModel().getSelectedItem();
		if(selected != null) {
			logic.removeTodoItem(selected);
		}
	}
}
