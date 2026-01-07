package M.health;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import java.util.ArrayList;
import java.util.List;

public class AuthManager {
    private static AuthManager instance;
    private DatabaseHelper dbHelper;
    private User currentUser;
    private List<String> userPermissions;

    private AuthManager(Context context) {
        dbHelper = new DatabaseHelper(context);
        userPermissions = new ArrayList<>();
    }

    public static synchronized AuthManager getInstance(Context context) {
        if (instance == null) {
            instance = new AuthManager(context.getApplicationContext());
        }
        return instance;
    }

    public User login(String email, String password) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery(
            "SELECT u.id, u.full_name, u.email, u.role, u.role_id, u.is_active, r.name as role_name " +
            "FROM users u LEFT JOIN roles r ON u.role_id = r.id " +
            "WHERE u.email = ? AND u.password_hash = ? AND u.is_active = 1", 
            new String[]{email, password});

        if (cursor.moveToFirst()) {
            currentUser = new User(
                cursor.getInt(0),
                cursor.getString(1),
                cursor.getString(2),
                cursor.getString(3),
                cursor.getInt(4),
                cursor.getInt(5) == 1
            );
            cursor.close();
            
            updateLastLogin(currentUser.id);
            loadUserPermissions();
            return currentUser;
        }
        cursor.close();
        return null;
    }

    private void updateLastLogin(int userId) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("last_login", System.currentTimeMillis());
        db.update("users", values, "id = ?", new String[]{String.valueOf(userId)});
    }

    private void loadUserPermissions() {
        if (currentUser == null) return;
        
        userPermissions.clear();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery(
            "SELECT permission_key FROM permissions WHERE role = ? AND role_required = 1", 
            new String[]{currentUser.role});
        
        while (cursor.moveToNext()) {
            userPermissions.add(cursor.getString(0));
        }
        cursor.close();
    }

    public boolean hasPermission(String permissionKey) {
        return currentUser != null && currentUser.isActive && userPermissions.contains(permissionKey);
    }

    public boolean isLoggedIn() {
        return currentUser != null && currentUser.isActive;
    }

    public User getCurrentUser() {
        return currentUser;
    }

    public void logout() {
        currentUser = null;
        userPermissions.clear();
    }

    public boolean validateSession() {
        if (currentUser == null) return false;
        
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery(
            "SELECT is_active FROM users WHERE id = ?", 
            new String[]{String.valueOf(currentUser.id)});
        
        if (cursor.moveToFirst()) {
            boolean isActive = cursor.getInt(0) == 1;
            cursor.close();
            if (!isActive) {
                logout();
                return false;
            }
            return true;
        }
        cursor.close();
        logout();
        return false;
    }

    public void refreshPermissions() {
        if (currentUser != null) {
            loadUserPermissions();
        }
    }

    public static class User {
        public final int id;
        public final String fullName;
        public final String email;
        public final String role;
        public final int roleId;
        public final boolean isActive;

        public User(int id, String fullName, String email, String role, int roleId, boolean isActive) {
            this.id = id;
            this.fullName = fullName;
            this.email = email;
            this.role = role;
            this.roleId = roleId;
            this.isActive = isActive;
        }
    }
}