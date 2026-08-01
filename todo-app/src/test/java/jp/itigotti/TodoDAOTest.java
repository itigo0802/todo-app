package jp.itigotti;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class TodoDAOTest {

    @TempDir
    Path tempDir;

    private TodoDAO dao;
    private String testDbUrl;

    @BeforeEach
    void setUp() throws SQLException {
        Path testDbPath = tempDir.resolve("test.db");
        testDbUrl = "jdbc:sqlite:" + testDbPath.toString();

        dao = new TodoDAO(testDbUrl);
        dao.initializeDB();
    }

    @AfterEach
    void tearDown() throws SQLException {
        try(Connection conn = DriverManager.getConnection(testDbUrl)) {
            conn.createStatement().execute("DROP TABLE IF EXISTS todo_items");
        } catch(SQLException e) {
            System.err.println("テーブル削除時にエラーが発生しました: " + e.getMessage());
        }
    }

    private TodoItemModel createTestItem(int id, String task, LocalDate expirationDate, boolean completed) {
        TodoItemModel item = new TodoItemModel();
        item.setId(id);
        item.setTask(task);
        item.setExpirationDate(expirationDate);
        item.setCompleted(completed);
        return item;
    }

    private void assertItemEquals(TodoItemModel expected, TodoItemModel actual) {
        assertNotNull(expected, "expectedがnullです");
        assertNotNull(actual, "actualがnullです");

        assertEquals(expected.getId(), actual.getId(), "IDが一致しない");
        assertEquals(expected.getTask(), actual.getTask(), "タスクが一致しない");
        assertEquals(expected.getExpirationDate(), actual.getExpirationDate(), "期限が一致しない");
        assertEquals(expected.isCompleted(), actual.isCompleted(), "完了状態が一致しない");
    }

    @Test
    void testInitializeDB() throws SQLException {
        try(Connection conn = DriverManager.getConnection(testDbUrl)) {
            var result = conn.createStatement()
                .executeQuery("SELECT name FROM sqlite_master WHERE type='table' AND name='todo_items'");
            assertTrue(result.next(), "todo_itemsテーブルが存在しない");
        }
    }

    @Test
    void testCreate() {
        TodoItemModel item = new TodoItemModel();
        item.setTask("テストタスク");
        item.setExpirationDate(LocalDate.now());

        TodoItemModel result = dao.create(item);

        assertNotNull(result, "結果がnullです");
        assertNotNull(result.getId(), "IDがnullです");

        TodoItemModel expected = createTestItem(result.getId(), "テストタスク", LocalDate.now(), false);
        assertItemEquals(expected, result);
    }

    @Test
    void testFindAll_空のリストを返す() {
        List<TodoItemModel> result = dao.findAll();

        assertNotNull(result, "結果がnullです");
        assertTrue(result.isEmpty(), "結果が空でありません");
    }

}
