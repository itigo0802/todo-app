package jp.itigotti;

import java.sql.Connection;
import java.sql.Date;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class TodoDAO {
    private static final String DB_URL = "jdbc:sqlite:todo.db";

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
        }
    }

    public List<TodoItemModel> findAll() {
        List<TodoItemModel> todoList = new ArrayList<>();
        try(Connection conn = DriverManager.getConnection(DB_URL)) {
            String sql = "select * from todo_items";

            try(PreparedStatement pStmt = conn.prepareStatement(sql)) {
                ResultSet rs = pStmt.executeQuery();

                while(rs.next()) {
                    TodoItemModel item = new TodoItemModel();
                    item.setId(rs.getInt("id"));
                    item.setTask(rs.getString("task"));
                    item.setExpirationDate(rs.getObject("expiration_date", LocalDate.class));
                    item.setIsCompleted(rs.getBoolean("is_completed"));
                    todoList.add(item);
                }
            }
        } catch(SQLException e) {
            e.printStackTrace();
        }
        return todoList;
    }

    public boolean create(TodoItemModel item) {
        try(Connection conn =DriverManager.getConnection(DB_URL)) {
            String sql = "insert into todo_items (task, expiration_date) values ("
                + "?, "
                + "?);";
            
            try(PreparedStatement pStmt = conn.prepareStatement(sql)) {
                pStmt.setString(1, item.getTask());
                pStmt.setDate(2, Date.valueOf(item.getExpirationDate()));

                return pStmt.executeUpdate() == 1;
            }
        } catch(SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean update(TodoItemModel item) {
        try(Connection conn = DriverManager.getConnection(DB_URL)) {
            String sql = "update todo_items set task = ?, "
                + "expiration_date = ?, "
                + "is_completed = ? "
                + "where id = ?;";
            
            try(PreparedStatement pStmt = conn.prepareStatement(sql)) {
                pStmt.setString(1, item.getTask());
                pStmt.setDate(2, Date.valueOf(item.getExpirationDate()));
                pStmt.setBoolean(3, item.isCompleted());
                pStmt.setInt(4, item.getId());

                return pStmt.executeUpdate() == 1;
            }
        } catch(SQLException e) {
            e.printStackTrace();
            return false;
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
            return false;
        }
    }
}
