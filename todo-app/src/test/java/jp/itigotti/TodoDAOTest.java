package jp.itigotti;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

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
    void tearDown() throws Exception {
        try (Connection conn = DriverManager.getConnection(testDbUrl)) {
            conn.createStatement().execute("DROP TABLE IF EXISTS todo_items");
        } catch (SQLException e) {
            System.err.println("テーブル削除時にエラーが発生しました: " + e.getMessage());
        } finally {
            if (dao != null) {
                dao.close();
            }
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

    @Test
    void testFindAll_TODO項目が存在する場合() {
        TodoItemModel item = new TodoItemModel();
        item.setTask("テストタスク");
        item.setExpirationDate(LocalDate.now());
        item.setCompleted(false);

        dao.create(item);
        
        List<TodoItemModel> result = dao.findAll();
        assertNotNull(result, "結果がnullです");
        assertFalse(result.isEmpty(), "結果が空です");
        assertEquals(1, result.size(), "結果のサイズが一致しません");
        assertItemEquals(item, result.get(0));
    }

    @Test
    void testUpdate_TODO項目を更新する() {
        TodoItemModel item = new TodoItemModel();
        item.setTask("テストタスク");
        item.setExpirationDate(LocalDate.now());
        TodoItemModel createdItem = dao.create(item);

        createdItem.setTask("更新されたタスク");
        createdItem.setExpirationDate(LocalDate.now().plusDays(1));
        createdItem.setCompleted(true);
        TodoItemModel result = dao.update(createdItem);

        List<TodoItemModel> allItems = dao.findAll();
        assertItemEquals(createdItem, result);
        assertItemEquals(createdItem, allItems.get(0));
        assertEquals(1, allItems.size(), "TODO項目の数が一致しません");
    }

    @Test
    void testDelete_TODO項目を削除する() {
        TodoItemModel item = new TodoItemModel();
        item.setTask("テストタスク");
        item.setExpirationDate(LocalDate.now());
        TodoItemModel createdItem = dao.create(item);

        dao.delete(createdItem);
        List<TodoItemModel> allItems = dao.findAll();

        assertTrue(allItems.isEmpty(), "TODO項目が削除されていません");
    }

    @Test
    void testCreate_expirationDateがnullの場合IllegalArgumentException() {
        TodoItemModel item = new TodoItemModel();
        item.setTask("テストタスク");
        // expirationDateをnullに設定
        item.setExpirationDate(null);

        assertThrows(IllegalArgumentException.class, () -> dao.create(item),
    "expirationDateがnullの場合はIllegalArgumentExceptionがスローされるべき");
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 3, 10})
    void testInitializeDB_nullExpirationDateが補完される(int nullRowCount) throws Exception {
        Path paramPath = tempDir.resolve("param_test_" + nullRowCount + ".db");
        String paramDbUrl = "jdbc:sqlite:" + paramPath.toString();

        try (Connection conn = DriverManager.getConnection(paramDbUrl)) {
            conn.createStatement().execute(
                "create table if not exists todo_items (" +
                "id integer primary key autoincrement, " +
                "task text not null, " +
                "expiration_date date, " +
                "is_completed boolean not null default false)"
            );
            for (int i = 0; i < nullRowCount; i++) {
                conn.createStatement().execute(
                    "insert into todo_items (task, expiration_date, is_completed) " +
                    "values ('タスク" + i + "', null, false)"
                );
            }
        }

        try (TodoDAO paramDao = new TodoDAO(paramDbUrl)) {
            paramDao.initializeDB();

            List<TodoItemModel> items = paramDao.findAll();
            assertEquals(nullRowCount, items.size(), "アイテム数が一致しない");
            for (TodoItemModel item : items) {
                assertNotNull(item.getExpirationDate(), "expirationDateがnullのままです id=" + item.getId());
            }
        }
    }

    @ParameterizedTest(name = "タスク文字列=\"{0}\"のときnull期限日でcreateが拒否される")
    @ValueSource(strings = {
        "普通のタスク",
        "a",
        "あいうえお",
        "!@#$%^&*()",
        "非常に長いタスク名前前前前前前前前前前前前前前前前前"
    })
    void testCreate_どんなタスク文字列でもnull期限日は拒否される(String task) {
        TodoItemModel item = new TodoItemModel();
        item.setTask(task);
        item.setExpirationDate(null);

        assertThrows(IllegalArgumentException.class, () -> dao.create(item),
            "タスク=\"" + task + "\" でもnull期限日はIllegalArgumentExceptionがスローされるべき");
    }

    
}
