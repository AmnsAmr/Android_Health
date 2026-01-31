package M.health;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class ConversationActivity extends AppCompatActivity {
    private DatabaseHelper dbHelper;
    private AuthManager authManager;
    private LinearLayout messagesContainer;
    private EditText etMessage;
    private int currentUserId;
    private int otherUserId;
    private String otherUserName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_conversation);

        dbHelper = new DatabaseHelper(this);
        authManager = AuthManager.getInstance(this);
        
        if (!authManager.isLoggedIn()) {
            finish();
            return;
        }

        currentUserId = authManager.getCurrentUser().id;
        otherUserId = getIntent().getIntExtra("other_user_id", -1);
        otherUserName = getIntent().getStringExtra("other_user_name");
        
        if (otherUserId == -1 || otherUserName == null) {
            finish();
            return;
        }

        View headerView = findViewById(R.id.headerLayout);
        UIHelper.setupHeaderWithSignOut(this, headerView, otherUserName, authManager);
        
        messagesContainer = findViewById(R.id.messagesContainer);
        etMessage = findViewById(R.id.etMessage);
        
        findViewById(R.id.btnSend).setOnClickListener(v -> sendMessage());
        
        loadMessages();
    }

    private void loadMessages() {
        messagesContainer.removeAllViews();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        
        Cursor cursor = db.rawQuery(
            "SELECT m.message, m.sent_at, u.full_name, m.sender_id " +
            "FROM messages m " +
            "JOIN users u ON m.sender_id = u.id " +
            "WHERE (m.sender_id = ? AND m.receiver_id = ?) OR (m.sender_id = ? AND m.receiver_id = ?) " +
            "ORDER BY m.sent_at ASC", 
            new String[]{String.valueOf(currentUserId), String.valueOf(otherUserId),
                        String.valueOf(otherUserId), String.valueOf(currentUserId)});

        while (cursor.moveToNext()) {
            addMessageBubble(
                cursor.getString(0),
                cursor.getString(1),
                cursor.getString(2),
                cursor.getInt(3) == currentUserId
            );
        }
        cursor.close();
    }

    private void addMessageBubble(String message, String timestamp, String senderName, boolean isFromMe) {
        View messageView = getLayoutInflater().inflate(R.layout.item_message_bubble, null);
        
        TextView tvMessage = messageView.findViewById(R.id.tvMessage);
        TextView tvTimestamp = messageView.findViewById(R.id.tvTimestamp);
        TextView tvSender = messageView.findViewById(R.id.tvSender);
        
        tvMessage.setText(message);
        tvTimestamp.setText(timestamp);
        tvSender.setText(isFromMe ? "Moi" : senderName);
        
        // Style differently for sent vs received messages
        if (isFromMe) {
            messageView.setBackgroundResource(R.color.primary_blue);
            tvMessage.setTextColor(getResources().getColor(android.R.color.white));
        } else {
            messageView.setBackgroundResource(android.R.color.white);
            tvMessage.setTextColor(getResources().getColor(android.R.color.black));
        }
        
        messagesContainer.addView(messageView);
    }

    private void sendMessage() {
        String messageText = etMessage.getText().toString().trim();
        if (messageText.isEmpty()) {
            Toast.makeText(this, "Veuillez saisir un message", Toast.LENGTH_SHORT).show();
            return;
        }

        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("sender_id", currentUserId);
        values.put("receiver_id", otherUserId);
        values.put("message", messageText);
        values.put("is_urgent", 0);
        
        long result = db.insert("messages", null, values);
        
        if (result != -1) {
            etMessage.setText("");
            loadMessages();
        } else {
            Toast.makeText(this, "Erreur lors de l'envoi", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadMessages();
    }
}