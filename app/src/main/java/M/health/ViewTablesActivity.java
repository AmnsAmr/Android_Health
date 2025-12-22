package M.health;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class ViewTablesActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_tables);

        TextView tablesText = findViewById(R.id.tablesText);
        DatabaseHelper dbHelper = new DatabaseHelper(this);
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        StringBuilder result = new StringBuilder();
        
        // Show all tables
        Cursor cursor = db.rawQuery("SELECT name FROM sqlite_master WHERE type='table'", null);
        result.append("TABLES:\n");
        while (cursor.moveToNext()) {
            result.append("- ").append(cursor.getString(0)).append("\n");
        }
        cursor.close();

        // Show users table content
        cursor = db.rawQuery("SELECT * FROM users", null);
        result.append("\nUSERS TABLE:\n");
        while (cursor.moveToNext()) {
            result.append("ID: ").append(cursor.getInt(0))
                  .append(", Name: ").append(cursor.getString(1))
                  .append(", Email: ").append(cursor.getString(2))
                  .append(", Role: ").append(cursor.getString(4)).append("\n");
        }
        cursor.close();

        tablesText.setText(result.toString());
    }
}