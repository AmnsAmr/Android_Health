package M.health;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import java.util.ArrayList;
import java.util.List;

public class UrgentRequestsActivity extends AppCompatActivity {

    private DatabaseHelper dbHelper;
    private AuthManager authManager;
    private ListView lvUrgentMessages;
    private List<UrgentMessage> messageList;
    private ArrayAdapter<UrgentMessage> adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_urgent_requests);

        dbHelper = new DatabaseHelper(this);
        authManager = AuthManager.getInstance(this);

        if (!authManager.isLoggedIn()) {
            finish();
            return;
        }

        lvUrgentMessages = findViewById(R.id.lvUrgentMessages);
        messageList = new ArrayList<>();

        loadUrgentMessages();
    }

    private void loadUrgentMessages() {
        messageList.clear();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        int currentUserId = authManager.getUserId();

        // Query for urgent messages sent to the current user (secretary)
        Cursor cursor = db.rawQuery(
                "SELECT m.id, u.full_name, m.message, m.sent_at " +
                        "FROM messages m " +
                        "JOIN users u ON m.sender_id = u.id " +
                        "WHERE m.receiver_id = ? AND m.is_urgent = 1 " +
                        "ORDER BY m.sent_at DESC",
                new String[]{String.valueOf(currentUserId)});

        while (cursor.moveToNext()) {
            messageList.add(new UrgentMessage(
                    cursor.getInt(0),
                    cursor.getString(1),
                    cursor.getString(2),
                    cursor.getString(3)
            ));
        }
        cursor.close();

        adapter = new ArrayAdapter<UrgentMessage>(this, android.R.layout.simple_list_item_2, messageList) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View view = super.getView(position, convertView, parent);

                // Card styling
                view.setBackgroundColor(getResources().getColor(android.R.color.white));
                view.setPadding(30, 30, 30, 30);
                view.setElevation(2f);

                TextView text1 = view.findViewById(android.R.id.text1);
                TextView text2 = view.findViewById(android.R.id.text2);

                UrgentMessage msg = getItem(position);
                text1.setText(msg.senderName + " (" + msg.date + ")");
                text1.setTypeface(null, android.graphics.Typeface.BOLD);
                text1.setTextColor(getResources().getColor(R.color.black));
                text2.setText(msg.content);
                text2.setMaxLines(2);

                return view;
            }
        };

        lvUrgentMessages.setAdapter(adapter);
        lvUrgentMessages.setOnItemClickListener((parent, view, position, id) ->
                showMessageOptions(messageList.get(position)));
    }

    private void showMessageOptions(UrgentMessage msg) {
        new AlertDialog.Builder(this)
                .setTitle("Message de " + msg.senderName)
                .setMessage(msg.content)
                .setPositiveButton("Marquer comme traité", (dialog, which) -> markAsHandled(msg.id))
                .setNegativeButton("Fermer", null)
                .show();
    }

    private void markAsHandled(int messageId) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("is_urgent", 0); // Remove urgent flag

        int rows = db.update("messages", values, "id = ?", new String[]{String.valueOf(messageId)});
        if (rows > 0) {
            Toast.makeText(this, "Message traité", Toast.LENGTH_SHORT).show();
            loadUrgentMessages(); // Refresh list
        }
    }

    private static class UrgentMessage {
        int id;
        String senderName, content, date;

        UrgentMessage(int id, String senderName, String content, String date) {
            this.id = id;
            this.senderName = senderName;
            this.content = content;
            this.date = date;
        }
    }
}