package M.health;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import java.util.ArrayList;
import java.util.List;

public class AuthManager {
    private static AuthManager instance;
    private DatabaseHelper dbHelper;
    private SharedPreferences prefs;
    private User currentUser;
    private List<String> userPermissions;

    private static final String PREFS_NAME = "HealthAppPrefs";
    private static final String KEY_USER_ID = "user_id";
    private static final String KEY_USER_ROLE = "user_role";
    private static final String KEY_USER_NAME = "user_name";
    private static final String KEY_USER_EMAIL = "user_email";
    private static final String KEY_IS_LOGGED_IN = "is_logged_in";

    private AuthManager(Context context) {
        dbHelper = new DatabaseHelper(context);
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        userPermissions = new ArrayList<>();
        restoreSession();
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
            saveSession();
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

    /**
     * Save user session to SharedPreferences for persistence across app restarts
     */
    private void saveSession() {
        if (currentUser == null) return;

        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt(KEY_USER_ID, currentUser.id);
        editor.putString(KEY_USER_ROLE, currentUser.role);
        editor.putString(KEY_USER_NAME, currentUser.fullName);
        editor.putString(KEY_USER_EMAIL, currentUser.email);
        editor.putBoolean(KEY_IS_LOGGED_IN, true);
        editor.apply();
    }

    /**
     * Restore user session from SharedPreferences
     */
    private void restoreSession() {
        if (!prefs.getBoolean(KEY_IS_LOGGED_IN, false)) {
            return;
        }

        int userId = prefs.getInt(KEY_USER_ID, -1);
        if (userId == -1) return;

        // Validate user still exists and is active
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT u.id, u.full_name, u.email, u.role, u.role_id, u.is_active " +
                        "FROM users u WHERE u.id = ? AND u.is_active = 1",
                new String[]{String.valueOf(userId)}
        );

        if (cursor.moveToFirst()) {
            currentUser = new User(
                    cursor.getInt(0),
                    cursor.getString(1),
                    cursor.getString(2),
                    cursor.getString(3),
                    cursor.getInt(4),
                    cursor.getInt(5) == 1
            );
            loadUserPermissions();
        } else {
            clearSession();
        }
        cursor.close();
    }

    /**
     * Clear session from memory and SharedPreferences
     */
    private void clearSession() {
        SharedPreferences.Editor editor = prefs.edit();
        editor.clear();
        editor.apply();

        currentUser = null;
        userPermissions.clear();
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
        clearSession();
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

    /**
     * Get user ID from current session (backward compatibility with SharedPreferences approach)
     */
    public int getUserId() {
        if (currentUser == null) {
            restoreSession(); // TRY TO RECOVER SESSION IF NULL
        }
        return currentUser != null ? currentUser.id : -1;
    }

    /**
     * Check if current user has a specific role
     */
    public boolean hasRole(String role) {
        return currentUser != null && currentUser.role.equalsIgnoreCase(role);
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