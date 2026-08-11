package jp.itigotti;

import java.time.LocalDate;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javafx.collections.ObservableList;

public class TodoListLogic {
    private final ObservableList<TodoItemModel> todoItems = javafx.collections.FXCollections.observableArrayList();
    private final TodoDAO dao;
    private static final Logger log = LoggerFactory.getLogger(TodoListLogic.class);

    public TodoListLogic(TodoDAO dao) {
        this.dao = dao;
    }

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
        } else {
            log.warn("Todoの登録がDBに反映されませんでした task={}, expirationDate={}", StringUtil.truncateForLog(task), expirationDate);
        }
    }
    
    
    public void removeTodoItem(TodoItemModel item) {
        if(dao.delete(item)) {
            todoItems.remove(item);
        } else {
            log.warn("Todoの削除がDBに反映されませんでした id={}", item.getId());
        }
    }

    public void refresh() {
        todoItems.clear();
        List<TodoItemModel> items = dao.findAll();
        items.forEach(this::setupItemListener);
        todoItems.addAll(items);
    }

    // 二重登録の判定・記録はTodoItemModel自身に持たせている（詳細はissue #35）。
    // 以前は外部のWeakHashMap<TodoItemModel, Boolean>で管理していたが、キーであるTodoItemModelの
    // equals()/hashCode()が可変フィールド(id)に依存しているため、挿入後にidが変わる呼び出し順序が
    // あるとエントリが迷子になりうる脆さがあった。「このインスタンスに既にリスナーを付けたか」は
    // 本質的にオブジェクト単位の話なので、インスタンス自身に状態を持たせる方が安全。
    private void setupItemListener(TodoItemModel item) {
        if (item.isListenerAdded()) {
            return;
        }
        item.completedProperty().addListener((obs, oldVal, newVal) -> {
            try {
                dao.update(item);
            } catch (RuntimeException e) {
                log.error("完了状態の更新に失敗しました id={}", item.getId(), e);
            }
        });
        item.markListenerAdded();
    }
}

