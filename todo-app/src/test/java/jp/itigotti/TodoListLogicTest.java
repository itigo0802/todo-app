package jp.itigotti;

import java.nio.file.Path;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;

public class TodoListLogicTest {

    @TempDir
    Path tempDir;

    private TodoDAO dao;

    private TodoDAO createTestDAO() {
        Path testDbPath = tempDir.resolve("test.db");
        String testDbUrl = "jdbc:sqlite:" + testDbPath.toString();
        dao = new TodoDAO(testDbUrl);
        dao.initializeDB();
        return dao;
    }

    @AfterEach
    void tearDown() throws Exception {
        if (dao != null) {
            dao.close();
        }
    }

    @Test
    void addTodoItem_追加できる() {
        TodoDAO dao = createTestDAO();

        TodoListLogic logic = new TodoListLogic(dao);

        logic.addTodoItem("テストタスク", LocalDate.now());

        assertEquals(1, logic.getTodoItems().size());
        assertEquals("テストタスク", logic.getTodoItems().get(0).getTask());
    }

    @Test
    void refresh_保存済みデータを読み込める() {
        TodoDAO dao = createTestDAO();
        TodoItemModel item = new TodoItemModel();
        item.setTask("保存済みタスク");
        item.setExpirationDate(LocalDate.now());
        dao.create(item);

        TodoListLogic logic = new TodoListLogic(dao);
        logic.refresh();

        assertEquals(1, logic.getTodoItems().size());
        assertEquals("保存済みタスク", logic.getTodoItems().get(0).getTask());
    }

    @Test
    void completedProperty_変更時にDBが更新される() {
        TodoDAO dao = createTestDAO();
        TodoItemModel item = new TodoItemModel();
        item.setTask("テストタスク");
        item.setExpirationDate(LocalDate.now());
        dao.create(item);

        TodoListLogic logic = new TodoListLogic(dao);
        logic.refresh();

        TodoItemModel loadedItem = logic.getTodoItems().get(0);
        loadedItem.setCompleted(true);

        assertTrue(dao.findAll().get(0).isCompleted());
    }

    @Test
    void refresh_複数回実行しても同じアイテムを正しく保持できる() {
        TodoDAO dao = createTestDAO();
        TodoItemModel item = new TodoItemModel();
        item.setTask("テストタスク");
        item.setExpirationDate(LocalDate.now());
        dao.create(item);

        TodoListLogic logic = new TodoListLogic(dao);
        logic.refresh();

        TodoItemModel loadedItem = logic.getTodoItems().get(0);
        loadedItem.setCompleted(true);

        logic.refresh();

        assertEquals(1, logic.getTodoItems().size());
        assertTrue(logic.getTodoItems().get(0).isCompleted());
    }

    @Test
    void addTodoItem_expirationDateがnullの場合IllegalArgumentException() {
        TodoDAO dao = createTestDAO();
        String task = "テストタスク";

        assertThrows(IllegalArgumentException.class, () -> {
            new TodoListLogic(dao).addTodoItem(task, null);
        });
    }

    // dao.delete()がfalseを返すケースをMockitoで再現し、todoItemsから削除されないことを検証する。
    // 実DB経由（存在しないidを使う方法）でも再現できるが、DBの都合と無関係にロジックの分岐だけを
    // 確実にテストできるモック方式を選んだ。
    @Test
    void removeTodoItem_DB削除に失敗した場合リストから削除されない() {
        TodoDAO mock = Mockito.mock(TodoDAO.class);
        Mockito.when(mock.delete(any())).thenReturn(false);
        TodoListLogic logic = new TodoListLogic(mock);
        TodoItemModel item = new TodoItemModel();
        logic.getTodoItems().add(item);

        logic.removeTodoItem(item);
        assertEquals(1, logic.getTodoItems().size());
    }
}
