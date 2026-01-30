package jp.itigotti;

import java.time.LocalDate;

import javafx.collections.ObservableList;

public class TodoListLogic {
	private final ObservableList<TodoItemModel> todoItems = javafx.collections.FXCollections.observableArrayList();
	
	public ObservableList<TodoItemModel> getTodoItems() {
		return todoItems;
	}
	
	public void addTodoItem(String task, LocalDate expirationDate) {
		if(task != null && !task.isBlank() && expirationDate != null) {
			todoItems.add(new TodoItemModel(task, expirationDate));
		}
	}
	
	public void removeTodoItem(TodoItemModel item) {
		todoItems.remove(item);
	}
}
