package M.health;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import java.util.ArrayList;
import java.util.List;

public class UrgentRequestsActivity extends AppCompatActivity {
    private DatabaseHelper dbHelper;
    private AuthManager authManager;
    private ListView lvUrgentMessages;
    private List<UrgentMessage> messages;
    private UrgentMessageAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_urgent_requests);

        dbHelper = new DatabaseHelper(this);
        authManager = AuthManager.getInstance(this);

        lvUrgentMessages = findViewById(R.id.lvUrgentMessages);
        Button btnSendUrgent = findViewById(R.id.btnSendUrgentMessage);

        messages = new ArrayList<>();
        adapter = new UrgentMessageAdapter();
        lvUrgentMessages.setAdapter(adapter);

        loadUrgentMessages();

        btnSendUrgent.setOnClickListener(v -> showSendUrgentMessageDialog());
    }

    private void loadUrgentMessages() {
        messages.clear();
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        String query = "SELECT m.id, m.message, m.sent_at, " +
                "sender.full_name as sender_name, receiver.full_name as receiver_name " +
                "FROM messages m " +
                "JOIN users sender ON m.sender_id = sender.id " +
                "JOIN users receiver ON m.receiver_id = receiver.id " +
                "WHERE m.is_urgent = 1 " +
                "ORDER BY m.sent_at DESC";

        Cursor cursor = db.rawQuery(query, null);

        while (cursor.moveToNext()) {
            messages.add(new UrgentMessage(
                cursor.getInt(0),
                cursor.getString(1),
                cursor.getString(2),
                cursor.getString(3),
                cursor.getString(4)
            ));
        }
        cursor.close();
        adapter.notifyDataSetChanged();
    }

    private void showSendUrgentMessageDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_send_urgent_message, null);

        Spinner spinnerDoctor = dialogView.findViewById(R.id.spinnerUrgentDoctor);
        EditText etMessage = dialogView.findViewById(R.id.etUrgentMessage);

        loadDoctors(spinnerDoctor);

        new AlertDialog.Builder(this)
            .setTitle("Envoyer Message Urgent")
            .setView(dialogView)
            .setPositiveButton("Envoyer", (dialog, which) -> {
                if (spinnerDoctor.getSelectedItem() != null) {
                    int doctorId = ((DoctorItem) spinnerDoctor.getSelectedItem()).id;
                    String message = etMessage.getText().toString().trim();
                    if (!message.isEmpty()) {
                        sendUrgentMessage(doctorId, message);
                    } else {
                        Toast.makeText(this, "Message vide", Toast.LENGTH_SHORT).show();
                    }
                }
            })
            .setNegativeButton("Annuler", null)
            .show();
    }

    private void loadDoctors(Spinner spinner) {
        List<DoctorItem> doctors = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        
        Cursor cursor = db.rawQuery(
            "SELECT u.id, u.full_name, d.specialization " +
            "FROM users u JOIN doctors d ON u.id = d.user_id " +
            "WHERE u.role = 'doctor' AND u.is_active = 1", null);

        while (cursor.moveToNext()) {
            doctors.add(new DoctorItem(cursor.getInt(0), cursor.getString(1), cursor.getString(2)));
        }
        cursor.close();

        ArrayAdapter<DoctorItem> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, doctors);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
    }

    private void sendUrgentMessage(int doctorId, String message) {
        int secretaryId = authManager.getUserId();
        Log.d("UrgentRequests", "Sending urgent message from secretary " + secretaryId + " to doctor " + doctorId);
        
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        
        ContentValues values = new ContentValues();
        values.put("sender_id", secretaryId);
        values.put("receiver_id", doctorId);
        values.put("message", message);
        values.put("is_urgent", 1);

        long result = db.insert("messages", null, values);
        
        Log.d("UrgentRequests", "Insert result: " + result);
        
        if (result != -1) {
            Toast.makeText(this, "Message urgent envoyé", Toast.LENGTH_SHORT).show();
            loadUrgentMessages();
        } else {
            Toast.makeText(this, "Erreur d'envoi", Toast.LENGTH_SHORT).show();
        }
    }

    private class UrgentMessageAdapter extends BaseAdapter {
        @Override
        public int getCount() { return messages.size(); }

        @Override
        public Object getItem(int position) { return messages.get(position); }

        @Override
        public long getItemId(int position) { return messages.get(position).id; }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(UrgentRequestsActivity.this)
                        .inflate(R.layout.item_urgent_message, parent, false);
            }

            UrgentMessage msg = messages.get(position);

            TextView tvSender = convertView.findViewById(R.id.tvUrgentSender);
            TextView tvReceiver = convertView.findViewById(R.id.tvUrgentReceiver);
            TextView tvMessage = convertView.findViewById(R.id.tvUrgentMessageText);
            TextView tvTime = convertView.findViewById(R.id.tvUrgentTime);

            tvSender.setText("De: " + msg.senderName);
            tvReceiver.setText("À: " + msg.receiverName);
            tvMessage.setText(msg.message);
            tvTime.setText(msg.sentAt);

            return convertView;
        }
    }

    private static class UrgentMessage {
        int id;
        String message, sentAt, senderName, receiverName;

        UrgentMessage(int id, String message, String sentAt, String senderName, String receiverName) {
            this.id = id;
            this.message = message;
            this.sentAt = sentAt;
            this.senderName = senderName;
            this.receiverName = receiverName;
        }
    }

    private static class DoctorItem {
        int id;
        String name, specialization;

        DoctorItem(int id, String name, String specialization) {
            this.id = id;
            this.name = name;
            this.specialization = specialization;
        }

        @Override
        public String toString() { return name + " - " + specialization; }
    }
}
