package jp.itigotti;

import java.time.LocalDate;

import javafx.collections.ObservableList;

public class TodoListLogic {
	private final ObservableList<TodoItemModel> todoItems = javafx.collections.FXCollections.observableArrayList();
	
	public ObservableList<TodoItemModel> getTodoItems() {
		return todoItems;
	}
	
	public void addTodoItem(String task, LocalDate expirationDate) {
		if(task == null || task.isBlank()) {
			throw new IllegalArgumentException("タスクが入力されていません");
		}

		if(expirationDate == null) {
			throw new IllegalArgumentException("期限が入力されていません");
		}

		todoItems.add(new TodoItemModel(task, expirationDate));
	}
	
	public void removeTodoItem(TodoItemModel item) {
		todoItems.remove(item);
	}
}
