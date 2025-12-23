package M.health;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class AdminDashboardActivity extends AppCompatActivity {
    private DatabaseHelper dbHelper;
    private TextView statsText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_dashboard);

        dbHelper = new DatabaseHelper(this);
        statsText = findViewById(R.id.statsText);

        LinearLayout manageUsersBtn = findViewById(R.id.manageUsersBtn);
        LinearLayout managePatientsBtn = findViewById(R.id.managePatientsBtn);

        manageUsersBtn.setOnClickListener(v -> 
            startActivity(new Intent(this, ManageUsersActivity.class)));
        
        managePatientsBtn.setOnClickListener(v -> 
            startActivity(new Intent(this, ManagePatientsActivity.class)));

        LinearLayout viewTablesBtn = findViewById(R.id.viewTablesBtn);
        viewTablesBtn.setOnClickListener(v -> 
            startActivity(new Intent(this, ViewTablesActivity.class)));

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