package M.health;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class DebugMessagesActivity extends AppCompatActivity {
    private static final String TAG = "DebugMessages";
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        TextView tv = new TextView(this);
        tv.setPadding(20, 20, 20, 20);
        setContentView(tv);
        
        DatabaseHelper dbHelper = new DatabaseHelper(this);
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        
        StringBuilder result = new StringBuilder();
        
        // Check messages table
        result.append("=== MESSAGES TABLE ===\n\n");
        Cursor msgCursor = db.rawQuery("SELECT * FROM messages", null);
        result.append("Total messages: ").append(msgCursor.getCount()).append("\n\n");
        
        while (msgCursor.moveToNext()) {
            result.append("ID: ").append(msgCursor.getInt(0)).append("\n");
            result.append("Sender ID: ").append(msgCursor.getInt(1)).append("\n");
            result.append("Receiver ID: ").append(msgCursor.getInt(2)).append("\n");
            result.append("Message: ").append(msgCursor.getString(3)).append("\n");
            result.append("Is Urgent: ").append(msgCursor.getInt(4)).append("\n");
            result.append("Sent At: ").append(msgCursor.getString(5)).append("\n\n");
        }
        msgCursor.close();
        
        // Check users table
        result.append("\n=== USERS TABLE ===\n\n");
        Cursor userCursor = db.rawQuery("SELECT id, full_name, role FROM users", null);
        result.append("Total users: ").append(userCursor.getCount()).append("\n\n");
        
        while (userCursor.moveToNext()) {
            result.append("ID: ").append(userCursor.getInt(0)).append("\n");
            result.append("Name: ").append(userCursor.getString(1)).append("\n");
            result.append("Role: ").append(userCursor.getString(2)).append("\n\n");
        }
        userCursor.close();
        
        // Check current doctor
        AuthManager authManager = AuthManager.getInstance(this);
        if (authManager.isLoggedIn()) {
            result.append("\n=== CURRENT USER ===\n\n");
            result.append("ID: ").append(authManager.getUserId()).append("\n");
            result.append("Name: ").append(authManager.getCurrentUser().fullName).append("\n");
            result.append("Role: ").append(authManager.getCurrentUser().role).append("\n");
        }
        
        Log.d(TAG, result.toString());
        tv.setText(result.toString());
    }
}
