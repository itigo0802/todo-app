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

    // TODO(human): removeTodoItem()で、dao.delete()がfalseを返した場合に
    // todoItems（ObservableList）からアイテムが削除されないことを検証するテストを書く。
    //
    // TodoListLogic.removeTodoItem()の実装（再掲）:
    //   public void removeTodoItem(TodoItemModel item) {
    //       if(dao.delete(item)) {
    //           todoItems.remove(item);
    //       } else {
    //           log.warn(...);
    //       }
    //   }
    //
    // dao.delete()にfalseを返させる方法として、大きく2通り考えられる：
    //
    //   (A) 実DAO + 存在しないidを使う方法（このファイルの他のテストと同じ、実DBに対する
    //       real object を使うスタイル）。TodoDAO.delete()はSQLのDELETEが0件しか
    //       更新できなかった場合にfalseを返すので、DBに存在しないidを持つTodoItemModelを
    //       logic.getTodoItems()に直接addしてからremoveTodoItem()を呼べば、
    //       新しい依存を増やさずに再現できる。
    //
    //   (B) Mockito でTodoDAOをモック化する方法。
    //       pom.xmlには mockito-junit-jupiter が依存として入っているが、実はこのプロジェクトの
    //       テストではまだ一度も使われていない。Mockito.mock(TodoDAO.class) で偽のTodoDAOを
    //       作り、when(mockDao.delete(any())).thenReturn(false) のように「呼ばれたらfalseを返す」
    //       ことを宣言してTodoListLogicに渡す、というやり方。実際のDB操作を一切介さずに
    //       「dao.delete()がfalseを返したケース」をピンポイントで再現できる。
    //
    // どちらでも要件（削除に失敗したらリストから消えないこと）は検証できる。(A)は今の
    // ファイルの書き方に馴染むが、(B)は「DBの都合とは無関係に、ロジックの分岐だけを
    // 確実にテストする」という単体テスト的なアプローチで、今後DAOの失敗理由が増えても
    // テストコード側は影響を受けにくい。どちらを選んでも良いので、選んだ理由も含めて
    // 実装してみてください。
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
