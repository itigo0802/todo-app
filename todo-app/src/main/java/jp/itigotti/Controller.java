package jp.itigotti;

import javafx.fxml.FXML;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;

public class Controller {
	@FXML private TextField taskInput;
	@FXML private ListView<String> todoListView;
	
	private TodoModel model = new TodoModel();
	
	@FXML
	public void initialize() {
		todoListView.setItems(model.getTodoItems());
	}
	
	@FXML
	public void handleAddAction() {
		model.addTodoItem(taskInput.getText());
		taskInput.clear();
	}
	
	@FXML
	public void handleDeleteAction() {
		String selected = todoListView.getSelectionModel().getSelectedItem();
		model.removeTodoItem(selected);
	}
}
