package jp.itigotti;

import java.io.File;
import java.net.URISyntaxException;
import java.sql.Connection;
import java.sql.Date;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;

public class TodoDAO {
    private static final String DB_URL;
    private static final String DB_PATH;

    static {
        try {
            URL location = TodoDAO.class.getProtectionDomain().getCodeSource().getLocation();
            Path locationPath = Paths.get(location.toURI());
            Path dbFilePath = locationPath.resolve("todo.db");

            DB_PATH = dbFilePath.toAbsolutePath().toString();
            DB_URL = "jdbc:sqlite:" + DB_PATH;

        } catch(URISyntaxException e) {
            e.printStackTrace();
            throw new RuntimeException("データベースパスの取得に失敗しました", e);
        }
    }

    @FunctionalInterface
    private interface StatementFunction<T> {
        T apply(PreparedStatement pStmt) throws SQLException;
    }

    private <T> T execute(String sql, boolean returnGeneratedKeys, String errorMessage,
            StatementFunction<T> action) {
        try(Connection conn = DriverManager.getConnection(DB_URL)) {
            try(PreparedStatement pStmt = returnGeneratedKeys
                    ? conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)
                    : conn.prepareStatement(sql)) {
                return action.apply(pStmt);
            }
        } catch(SQLException e) {
            e.printStackTrace();
            throw new RuntimeException(errorMessage, e);
        }
    }

    private <T> T execute(String sql, String errorMessage, StatementFunction<T> action) {
        return execute(sql, false, errorMessage, action);
    }

    private static void setExpirationDate(PreparedStatement pStmt, int index, LocalDate date)
            throws SQLException {
        if(date != null) {
            pStmt.setDate(index, Date.valueOf(date));
        } else {
            pStmt.setNull(index, Types.DATE);
        }
    }

    public void initializeDB() {
        new File(DB_PATH).getParentFile().mkdirs();
        String sql = "create table if not exists todo_items ("
            + "id integer primary key autoincrement, "
            + "task text not null, "
            + "expiration_date date, "
            + "is_completed boolean not null default false);";

        execute(sql, "DBの初期化に失敗しました", pStmt -> pStmt.executeUpdate());
    }

    public List<TodoItemModel> findAll() {
        return execute("select * from todo_items", "DBの読み込みに失敗しました", pStmt -> {
            List<TodoItemModel> todoList = new ArrayList<>();
            ResultSet rs = pStmt.executeQuery();

            while(rs.next()) {
                TodoItemModel item = new TodoItemModel();
                item.setId(rs.getInt("id"));
                item.setTask(rs.getString("task"));
                item.setExpirationDate(rs.getObject("expiration_date", LocalDate.class));
                item.setCompleted(rs.getBoolean("is_completed"));
                todoList.add(item);
            }
            return todoList;
        });
    }

    public TodoItemModel create(TodoItemModel item) {
        String sql = "insert into todo_items (task, expiration_date) values ("
            + "?, "
            + "?);";

        return execute(sql, true, "DBへの登録に失敗しました", pStmt -> {
            pStmt.setString(1, item.getTask());
            setExpirationDate(pStmt, 2, item.getExpirationDate());

            if(pStmt.executeUpdate() == 1) {
                ResultSet rs = pStmt.getGeneratedKeys();
                if(rs.next()) {
                    item.setId(rs.getInt(1));
                }
                return item;
            } else {
                return null;
            }
        });
    }

    public TodoItemModel update(TodoItemModel item) {
        String sql = "update todo_items set task = ?, "
            + "expiration_date = ?, "
            + "is_completed = ? "
            + "where id = ?;";

        return execute(sql, "DBへの更新に失敗しました", pStmt -> {
            pStmt.setString(1, item.getTask());
            setExpirationDate(pStmt, 2, item.getExpirationDate());
            pStmt.setBoolean(3, item.isCompleted());
            pStmt.setInt(4, item.getId());

            return pStmt.executeUpdate() == 1 ? item : null;
        });
    }

    public boolean delete(TodoItemModel item) {
        return execute("delete from todo_items where id = ?;", "DBへの削除に失敗しました", pStmt -> {
            pStmt.setInt(1, item.getId());

            return pStmt.executeUpdate() == 1;
        });
    }
}
