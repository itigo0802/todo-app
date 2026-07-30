package jp.itigotti;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.time.LocalDate;
import java.util.List;
import java.util.function.Consumer;

import javafx.collections.ObservableList;

public class TodoListLogic {
	private static final Logger LOGGER = System.getLogger(TodoListLogic.class.getName());

	private final ObservableList<TodoItemModel> todoItems = javafx.collections.FXCollections.observableArrayList();
	private final TodoDAO dao;

	private Consumer<RuntimeException> errorHandler = e -> LOGGER.log(Level.ERROR, e.getMessage(), e);
	private boolean revertingCompleted;

	public TodoListLogic() {
		this(new TodoDAO());
	}

	public TodoListLogic(TodoDAO dao) {
		this.dao = dao;
	}

	public ObservableList<TodoItemModel> getTodoItems() {
		return todoItems;
	}

	/**
	 * Registers the handler notified about failures that happen outside of a caller's
	 * control flow, such as a persistence error triggered by toggling an item.
	 */
	public void setErrorHandler(Consumer<RuntimeException> errorHandler) {
		this.errorHandler = errorHandler;
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

		dao.create(item);
		setupItemListener(item);
		todoItems.add(item);
	}
	
	
	public void removeTodoItem(TodoItemModel item) {
		dao.delete(item);
		todoItems.remove(item);
	}

	public void refresh() {
		List<TodoItemModel> items = dao.findAll();
		items.forEach(this::setupItemListener);
		todoItems.setAll(items);
	}

	private void setupItemListener(TodoItemModel item) {
		item.completedProperty().addListener((obs, oldVal, newVal) -> {
			if(revertingCompleted) {
				return;
			}

			try {
				dao.update(item);
			} catch(RuntimeException e) {
				revertingCompleted = true;
				try {
					item.setCompleted(oldVal);
				} finally {
					revertingCompleted = false;
				}
				errorHandler.accept(e);
			}
		});
	}
}
