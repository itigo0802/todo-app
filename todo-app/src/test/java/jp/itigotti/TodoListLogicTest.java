package jp.itigotti;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.ArrayList;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class TodoListLogicTest {

    @Mock private TodoDAO dao;

    private TodoListLogic logic;

    @BeforeEach
    void setUp() {
        logic = new TodoListLogic(dao);
    }

    @Test
    void addTodoItemPropagatesPersistenceFailure() {
        when(dao.create(any())).thenThrow(new DataAccessException("DBへの登録に失敗しました"));

        assertThrows(DataAccessException.class, () -> logic.addTodoItem("task", LocalDate.now()));
        assertTrue(logic.getTodoItems().isEmpty());
    }

    @Test
    void removeTodoItemPropagatesPersistenceFailure() {
        TodoItemModel item = new TodoItemModel();
        when(dao.findAll()).thenReturn(List.of(item));
        logic.refresh();

        doThrow(new DataAccessException("DBからの削除に失敗しました")).when(dao).delete(item);

        assertThrows(DataAccessException.class, () -> logic.removeTodoItem(item));
        assertEquals(List.of(item), logic.getTodoItems());
    }

    @Test
    void refreshPropagatesPersistenceFailure() {
        when(dao.findAll()).thenThrow(new DataAccessException("DBの読み込みに失敗しました"));

        assertThrows(DataAccessException.class, () -> logic.refresh());
    }

    @Test
    void failedCompletionUpdateIsRevertedAndReported() {
        TodoItemModel item = new TodoItemModel();
        when(dao.findAll()).thenReturn(List.of(item));
        logic.refresh();

        List<RuntimeException> reported = new ArrayList<>();
        logic.setErrorHandler(reported::add);
        when(dao.update(item)).thenThrow(new DataAccessException("DBへの更新に失敗しました"));

        item.setCompleted(true);

        assertFalse(item.isCompleted());
        assertEquals(1, reported.size());
        assertEquals("DBへの更新に失敗しました", reported.get(0).getMessage());
    }
}
