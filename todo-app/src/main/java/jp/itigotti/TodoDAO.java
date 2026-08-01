package jp.itigotti;

import java.io.IOException;
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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class TodoDAO {
    private static final String DB_URL;
    private static final String DB_PATH;
    private final String dbUrl;

    public TodoDAO() {
        this.dbUrl = DB_URL;
    }

    public TodoDAO(String customDbUrl) {
        this.dbUrl = customDbUrl;
    }

    static {
        Path portablePath = Paths.get("todo.db");
        Path portableDir = portablePath.getParent();

        if(portableDir != null && Files.isWritable(portableDir)) {
            DB_PATH = portablePath.toAbsolutePath().toString();
        } else {
            String userHome = System.getProperty("user.home");
            if(userHome == null) {
                throw new RuntimeException("ユーザーホームディレクトリが取得できません");
            }

            Path homeDir = Paths.get(userHome, ".todo-app");
            try {
                Files.createDirectories(homeDir);
            } catch(IOException e) {
                throw new RuntimeException("データベースディレクトリの作成に失敗しました", e);
            }

            DB_PATH = homeDir.resolve("todo.db").toString();
        }
        DB_URL = "jdbc:sqlite:" + DB_PATH;
    }

    public void initializeDB() {
        try(Connection conn = DriverManager.getConnection(DB_URL)) {
            String sql = "create table if not exists todo_items ("
                + "id integer primary key autoincrement, "
                + "task text not null, "
                + "expiration_date date, "
                + "is_completed boolean not null default false);";

            try(PreparedStatement pStmt = conn.prepareStatement(sql)) {
                pStmt.executeUpdate();
            }
        } catch(SQLException e) {
            e.printStackTrace();
            throw new RuntimeException("DBの初期化に失敗しました", e);
        }
    }

    public List<TodoItemModel> findAll() {
        List<TodoItemModel> todoList = new ArrayList<>();
        try(Connection conn = DriverManager.getConnection(DB_URL)) {
            String sql = "select * from todo_items order by expiration_date asc, id asc";

            try(PreparedStatement pStmt = conn.prepareStatement(sql)) {
                ResultSet rs = pStmt.executeQuery();

                while(rs.next()) {
                    TodoItemModel item = new TodoItemModel();
                    item.setId(rs.getInt("id"));
                    item.setTask(rs.getString("task"));
                    item.setExpirationDate(rs.getObject("expiration_date", LocalDate.class));
                    item.setCompleted(rs.getBoolean("is_completed"));
                    todoList.add(item);
                }
            }
        } catch(SQLException e) {
            e.printStackTrace();
            throw new RuntimeException("DBの読み込みに失敗しました", e);
        }
        return todoList;
    }

    public TodoItemModel create(TodoItemModel item) {
        try(Connection conn = DriverManager.getConnection(DB_URL)) {
            String sql = "insert into todo_items (task, expiration_date, is_completed) values ("
                + "?,"
                + "?, "
                + "?);";
            
            try(PreparedStatement pStmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                pStmt.setString(1, item.getTask());

                if(item.getExpirationDate() != null) {
                    pStmt.setDate(2, Date.valueOf(item.getExpirationDate()));
                } else {
                    pStmt.setNull(2, Types.DATE);
                }

                pStmt.setBoolean(3, false);

                if(pStmt.executeUpdate() == 1) {
                    ResultSet rs = pStmt.getGeneratedKeys();
                    if(rs.next()) {
                        item.setId(rs.getInt(1));
                    }
                    return item;
                } else {
                    return null;
                }
            }
        } catch(SQLException e) {
            e.printStackTrace();
            throw new RuntimeException("DBへの登録に失敗しました", e);
        }
    }

    public TodoItemModel update(TodoItemModel item) {
        try(Connection conn = DriverManager.getConnection(DB_URL)) {
            String sql = "update todo_items set task = ?, "
                + "expiration_date = ?, "
                + "is_completed = ? "
                + "where id = ?;";
            
            try(PreparedStatement pStmt = conn.prepareStatement(sql)) {
                pStmt.setString(1, item.getTask());
                if(item.getExpirationDate() != null) {
                    pStmt.setDate(2, Date.valueOf(item.getExpirationDate()));
                } else {
                    pStmt.setNull(2, Types.DATE);
                }
                pStmt.setBoolean(3, item.isCompleted());
                pStmt.setInt(4, item.getId());

                if(pStmt.executeUpdate() == 1) {
                    return item;
                } else {
                    return null;
                }
            }
        } catch(SQLException e) {
            e.printStackTrace();
            throw new RuntimeException("DBへの更新に失敗しました", e);
        }
    }

    public boolean delete(TodoItemModel item) {
        try(Connection conn = DriverManager.getConnection(DB_URL)) {
            String sql = "delete from todo_items where id = ?;";

            try(PreparedStatement pStmt = conn.prepareStatement(sql)) {
                pStmt.setInt(1, item.getId());

                return pStmt.executeUpdate() == 1;
            }
        } catch(SQLException e) {
            e.printStackTrace();
            throw new RuntimeException("DBへの削除に失敗しました", e);
        }
    }
}
