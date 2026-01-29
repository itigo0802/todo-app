package jp.itigotti;

import javafx.collections.ObservableList;

public class TodoModel {
	private final ObservableList<String> todoItems = javafx.collections.FXCollections.observableArrayList();
	
	public ObservableList<String> getTodoItems() {
		return todoItems;
	}
	
	public void addTodoItem(String item) {
		if(item != null && !item.isEmpty()) {
			todoItems.add(item);
		}
	}
	
	public void removeTodoItem(String item) {
		todoItems.remove(item);
	}
}
