package M.health;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class PatientAdminDetailsActivity extends AppCompatActivity {

    private DatabaseHelper dbHelper;
    private int patientId;

    private TextView tvPatientName, tvPatientEmail, tvPatientPhone, tvPatientDob, tvPatientGender, tvPatientEmergencyContact;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_patient_admin_details);

        dbHelper = new DatabaseHelper(this);
        patientId = getIntent().getIntExtra("patient_id", -1);

        if (patientId == -1) {
            // Handle error: patient ID not found
            finish();
            return;
        }

        initializeViews();
        loadPatientDetails();
    }

    private void initializeViews() {
        tvPatientName = findViewById(R.id.tvPatientName);
        tvPatientEmail = findViewById(R.id.tvPatientEmail);
        tvPatientPhone = findViewById(R.id.tvPatientPhone);
        tvPatientDob = findViewById(R.id.tvPatientDob);
        tvPatientGender = findViewById(R.id.tvPatientGender);
        tvPatientEmergencyContact = findViewById(R.id.tvPatientEmergencyContact);
    }

    private void loadPatientDetails() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        String query = "SELECT u.full_name, u.email, u.phone, p.date_of_birth, p.gender, p.emergency_contact " +
                "FROM users u LEFT JOIN patients p ON u.id = p.user_id " +
                "WHERE u.id = ?";

        Cursor cursor = db.rawQuery(query, new String[]{String.valueOf(patientId)});

        if (cursor.moveToFirst()) {
            tvPatientName.setText(cursor.getString(0));
            tvPatientEmail.setText(cursor.getString(1));
            tvPatientPhone.setText(cursor.getString(2));
            tvPatientDob.setText("DOB: " + cursor.getString(3));
            tvPatientGender.setText("Gender: " + cursor.getString(4));
            tvPatientEmergencyContact.setText("Emergency Contact: " + cursor.getString(5));
        }

        cursor.close();
    }
}
