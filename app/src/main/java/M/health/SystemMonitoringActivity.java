package M.health;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class SystemMonitoringActivity extends AppCompatActivity {
    private DatabaseHelper dbHelper;
    private AuthManager authManager;
    
    private TextView tvTotalUsers, tvActiveUsers, tvTotalAppointments, tvTodayAppointments;
    private TextView tvTotalMessages, tvUrgentMessages, tvPendingRefills;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_system_monitoring);

        dbHelper = new DatabaseHelper(this);
        authManager = AuthManager.getInstance(this);
        
        if (!authManager.isLoggedIn() || !authManager.hasPermission("admin_view_all_data")) {
            finish();
            return;
        }

        View headerView = findViewById(R.id.headerLayout);
        UIHelper.setupHeaderWithSignOut(this, headerView, "Monitoring Système", authManager);
        
        initializeViews();
        loadSystemMetrics();
    }

    private void initializeViews() {
        tvTotalUsers = findViewById(R.id.tvTotalUsers);
        tvActiveUsers = findViewById(R.id.tvActiveUsers);
        tvTotalAppointments = findViewById(R.id.tvTotalAppointments);
        tvTodayAppointments = findViewById(R.id.tvTodayAppointments);
        tvTotalMessages = findViewById(R.id.tvTotalMessages);
        tvUrgentMessages = findViewById(R.id.tvUrgentMessages);
        tvPendingRefills = findViewById(R.id.tvPendingRefills);
    }

    private void loadSystemMetrics() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        
        // User metrics
        Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM users", null);
        if (cursor.moveToFirst()) {
            tvTotalUsers.setText(String.valueOf(cursor.getInt(0)));
        }
        cursor.close();
        
        cursor = db.rawQuery("SELECT COUNT(*) FROM users WHERE is_active = 1", null);
        if (cursor.moveToFirst()) {
            tvActiveUsers.setText(String.valueOf(cursor.getInt(0)));
        }
        cursor.close();
        
        // Appointment metrics
        cursor = db.rawQuery("SELECT COUNT(*) FROM appointments", null);
        if (cursor.moveToFirst()) {
            tvTotalAppointments.setText(String.valueOf(cursor.getInt(0)));
        }
        cursor.close();
        
        cursor = db.rawQuery("SELECT COUNT(*) FROM appointments WHERE date(appointment_datetime) = date('now')", null);
        if (cursor.moveToFirst()) {
            tvTodayAppointments.setText(String.valueOf(cursor.getInt(0)));
        }
        cursor.close();
        
        // Message metrics
        cursor = db.rawQuery("SELECT COUNT(*) FROM messages", null);
        if (cursor.moveToFirst()) {
            tvTotalMessages.setText(String.valueOf(cursor.getInt(0)));
        }
        cursor.close();
        
        cursor = db.rawQuery("SELECT COUNT(*) FROM messages WHERE is_urgent = 1", null);
        if (cursor.moveToFirst()) {
            tvUrgentMessages.setText(String.valueOf(cursor.getInt(0)));
        }
        cursor.close();
        
        // Refill requests
        cursor = db.rawQuery("SELECT COUNT(*) FROM prescription_refill_requests WHERE status = 'pending'", null);
        if (cursor.moveToFirst()) {
            tvPendingRefills.setText(String.valueOf(cursor.getInt(0)));
        }
        cursor.close();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadSystemMetrics();
    }
}