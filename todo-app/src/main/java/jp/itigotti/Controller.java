package jp.itigotti;

import javafx.fxml.FXML;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;

public class Controller {
	@FXML private TextField taskInput;
	@FXML private ListView<String> todoListView;
	
	private final TodoModel model = new TodoModel();
	
	@FXML
	 private void initialize() {
		todoListView.setItems(model.getTodoItems());
	}
	
	@FXML
	private void handleAddAction() {
		model.addTodoItem(taskInput.getText());
		taskInput.clear();
	}
	
	@FXML
	private void handleDeleteAction() {
		String selected = todoListView.getSelectionModel().getSelectedItem();
		if(selected != null) {
			model.removeTodoItem(selected);
		}
	}
}
