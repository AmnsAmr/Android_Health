package M.health;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class AdminDashboardActivity extends AppCompatActivity {
    private DatabaseHelper dbHelper;
    private AuthManager authManager;
    private TextView statsText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_dashboard);

        dbHelper = new DatabaseHelper(this);
        authManager = AuthManager.getInstance(this);
        statsText = findViewById(R.id.statsText);

        if (!authManager.isLoggedIn() || !authManager.validateSession()) {
            Toast.makeText(this, "Session expirée", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        if (!authManager.hasPermission("admin_manage_users")) {
            Toast.makeText(this, "Accès refusé", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Setup reusable user profile header
        View userProfileHeader = findViewById(R.id.userProfileHeader);
        UIHelper.setupUserProfileHeader(this, userProfileHeader, authManager);

        LinearLayout manageUsersBtn = findViewById(R.id.manageUsersBtn);
        LinearLayout managePatientsBtn = findViewById(R.id.managePatientsBtn);

        manageUsersBtn.setOnClickListener(v -> {
            if (authManager.hasPermission("admin_manage_users")) {
                startActivity(new Intent(this, ManageUsersActivity.class));
            } else {
                Toast.makeText(this, "Accès refusé", Toast.LENGTH_SHORT).show();
            }
        });
        
        managePatientsBtn.setOnClickListener(v -> {
            if (authManager.hasPermission("admin_manage_patients")) {
                startActivity(new Intent(this, ManagePatientsActivity.class));
            } else {
                Toast.makeText(this, "Accès refusé", Toast.LENGTH_SHORT).show();
            }
        });

        LinearLayout viewTablesBtn = findViewById(R.id.viewTablesBtn);
        viewTablesBtn.setOnClickListener(v -> {
            if (authManager.hasPermission("admin_view_all_data")) {
                startActivity(new Intent(this, ViewTablesActivity.class));
            } else {
                Toast.makeText(this, "Accès refusé", Toast.LENGTH_SHORT).show();
            }
        });

        loadStats();
    }

    private void loadStats() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        StringBuilder stats = new StringBuilder();

        // Count users by role
        Cursor cursor = db.rawQuery("SELECT role, COUNT(*) FROM users GROUP BY role", null);
        stats.append("Utilisateurs:\n");
        while (cursor.moveToNext()) {
            stats.append(cursor.getString(0)).append(": ").append(cursor.getInt(1)).append("\n");
        }
        cursor.close();

        // Count appointments
        cursor = db.rawQuery("SELECT COUNT(*) FROM appointments", null);
        if (cursor.moveToFirst()) {
            stats.append("\nRendez-vous: ").append(cursor.getInt(0));
        }
        cursor.close();

        statsText.setText(stats.toString());
    }
}