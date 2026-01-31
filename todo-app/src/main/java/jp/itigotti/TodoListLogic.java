package jp.itigotti;

import java.time.LocalDate;

import javafx.collections.ObservableList;

public class TodoListLogic {
	private final ObservableList<TodoItemModel> todoItems = javafx.collections.FXCollections.observableArrayList();
	private final TodoDAO dao = new TodoDAO();

	public ObservableList<TodoItemModel> getTodoItems() {
		todoItems.addAll(dao.findAll());
		return todoItems;
	}
	
	public void addTodoItem(String task, LocalDate expirationDate) {
		if(task == null || task.isBlank()) {
			throw new IllegalArgumentException("タスクが入力されていません");
		}

		if(expirationDate == null) {
			throw new IllegalArgumentException("期限が入力されていません");
		}

		TodoItemModel item = new TodoItemModel();
		item.setTask(task);
		item.setExpirationDate(expirationDate);

		dao.create(item);
	}
	
	public void removeTodoItem(TodoItemModel item) {
		dao.delete(item);
	}

	public void refresh() {
		todoItems.clear();
		todoItems.addAll(dao.findAll());
	}
}

