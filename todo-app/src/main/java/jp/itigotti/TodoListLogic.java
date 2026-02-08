package jp.itigotti;

import java.time.LocalDate;
import java.util.List;

import javafx.collections.ObservableList;

public class TodoListLogic {
	private final ObservableList<TodoItemModel> todoItems = javafx.collections.FXCollections.observableArrayList();
	private final TodoDAO dao = new TodoDAO();

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

		TodoItemModel item = new TodoItemModel();
		item.setTask(task);
		item.setExpirationDate(expirationDate);
		item.setCompleted(false);

		if(dao.create(item) != null) {
			setupItemListener(item);
			todoItems.add(item);
		}
	}
	
	
	public void removeTodoItem(TodoItemModel item) {
		if(dao.delete(item)) {
			todoItems.remove(item);
		}
	}

	public void refresh() {
		todoItems.clear();
		List<TodoItemModel> items = dao.findAll();
		items.forEach(this::setupItemListener);
		todoItems.addAll(items);
	}

	private void setupItemListener(TodoItemModel item) {
		item.completedProperty().addListener((obs, oldVal, newVal) -> {
			dao.update(item);
		});
	}
}
