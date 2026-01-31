package M.health;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.util.Log;
import android.widget.ScrollView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class DatabaseDiagnosticActivity extends AppCompatActivity {
    private static final String TAG = "DBDiagnostic";
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        ScrollView scrollView = new ScrollView(this);
        TextView tv = new TextView(this);
        tv.setPadding(20, 20, 20, 20);
        tv.setTextSize(12);
        scrollView.addView(tv);
        setContentView(scrollView);
        
        DatabaseHelper dbHelper = new DatabaseHelper(this);
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        AuthManager authManager = AuthManager.getInstance(this);
        
        StringBuilder result = new StringBuilder();
        
        result.append("DATABASE DIAGNOSTIC\n");
        result.append("==================\n\n");
        
        // Database path
        result.append("Database Path: ").append(db.getPath()).append("\n\n");
        
        // Current user
        if (authManager.isLoggedIn()) {
            result.append("CURRENT USER:\n");
            result.append("ID: ").append(authManager.getUserId()).append("\n");
            result.append("Name: ").append(authManager.getCurrentUser().fullName).append("\n");
            result.append("Role: ").append(authManager.getCurrentUser().role).append("\n\n");
        } else {
            result.append("NO USER LOGGED IN\n\n");
        }
        
        // All users
        result.append("ALL USERS:\n");
        result.append("---------\n");
        Cursor userCursor = db.rawQuery("SELECT id, full_name, email, role, is_active FROM users ORDER BY id", null);
        while (userCursor.moveToNext()) {
            result.append(String.format("ID:%d | %s | %s | %s | Active:%d\n",
                userCursor.getInt(0),
                userCursor.getString(1),
                userCursor.getString(2),
                userCursor.getString(3),
                userCursor.getInt(4)));
        }
        userCursor.close();
        result.append("\n");
        
        // All messages
        result.append("ALL MESSAGES:\n");
        result.append("------------\n");
        Cursor msgCursor = db.rawQuery(
            "SELECT m.id, m.sender_id, s.full_name, m.receiver_id, r.full_name, " +
            "m.message, m.is_urgent, m.sent_at " +
            "FROM messages m " +
            "LEFT JOIN users s ON m.sender_id = s.id " +
            "LEFT JOIN users r ON m.receiver_id = r.id " +
            "ORDER BY m.id", null);
        
        int msgCount = msgCursor.getCount();
        result.append("Total: ").append(msgCount).append("\n\n");
        
        while (msgCursor.moveToNext()) {
            result.append(String.format("Msg ID:%d\n", msgCursor.getInt(0)));
            result.append(String.format("  From: %d (%s)\n", msgCursor.getInt(1), msgCursor.getString(2)));
            result.append(String.format("  To: %d (%s)\n", msgCursor.getInt(3), msgCursor.getString(4)));
            result.append(String.format("  Text: %s\n", msgCursor.getString(5)));
            result.append(String.format("  Urgent: %d\n", msgCursor.getInt(6)));
            result.append(String.format("  Time: %s\n\n", msgCursor.getString(7)));
        }
        msgCursor.close();
        
        if (msgCount == 0) {
            result.append("NO MESSAGES FOUND IN DATABASE!\n");
            result.append("This means messages are not being saved or you're looking at the wrong database.\n\n");
        }
        
        // Test query for doctor messages
        if (authManager.isLoggedIn() && authManager.getCurrentUser().role.equals("doctor")) {
            int doctorId = authManager.getUserId();
            result.append("DOCTOR MESSAGE QUERY TEST:\n");
            result.append("-------------------------\n");
            
            Cursor testCursor = db.rawQuery(
                "SELECT COUNT(*) FROM messages WHERE sender_id = ? OR receiver_id = ?",
                new String[]{String.valueOf(doctorId), String.valueOf(doctorId)});
            if (testCursor.moveToFirst()) {
                result.append("Messages for doctor ID ").append(doctorId).append(": ")
                    .append(testCursor.getInt(0)).append("\n\n");
            }
            testCursor.close();
        }
        
        Log.d(TAG, result.toString());
        tv.setText(result.toString());
    }
}
