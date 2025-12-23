package M.health;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import java.util.ArrayList;
import java.util.List;

public class DoctorPatientsActivity extends AppCompatActivity {
    private DatabaseHelper dbHelper;
    private ListView patientsListView;
    private int doctorId;
    private List<Patient> patients;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_doctor_patients);

        dbHelper = new DatabaseHelper(this);
        doctorId = getIntent().getIntExtra("doctor_id", -1);
        patientsListView = findViewById(R.id.patientsListView);
        patients = new ArrayList<>();

        patientsListView.setOnItemClickListener((parent, view, position, id) -> 
            showPatientDetails(patients.get(position)));

        loadPatients();
    }

    private void loadPatients() {
        patients.clear();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        
        Cursor cursor = db.rawQuery(
            "SELECT DISTINCT u.id, u.full_name, p.date_of_birth, p.blood_type " +
            "FROM users u " +
            "JOIN patients p ON u.id = p.user_id " +
            "JOIN appointments a ON u.id = a.patient_id " +
            "WHERE a.doctor_id = ? AND u.role = 'patient'", 
            new String[]{String.valueOf(doctorId)});

        List<String> patientStrings = new ArrayList<>();
        while (cursor.moveToNext()) {
            Patient patient = new Patient(
                cursor.getInt(0), 
                cursor.getString(1),
                cursor.getString(2),
                cursor.getString(3)
            );
            patients.add(patient);
            patientStrings.add(patient.fullName + " - " + patient.bloodType);
        }
        cursor.close();

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, 
            android.R.layout.simple_list_item_1, patientStrings);
        patientsListView.setAdapter(adapter);
    }

    private void showPatientDetails(Patient patient) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        StringBuilder details = new StringBuilder();
        
        details.append("Patient: ").append(patient.fullName).append("\n");
        details.append("Date de naissance: ").append(patient.dateOfBirth).append("\n");
        details.append("Groupe sanguin: ").append(patient.bloodType).append("\n\n");

        // Medical records
        details.append("DOSSIERS MÉDICAUX:\n");
        Cursor cursor = db.rawQuery(
            "SELECT diagnosis, treatment, created_at FROM medical_records " +
            "WHERE patient_id = ? AND doctor_id = ? ORDER BY created_at DESC", 
            new String[]{String.valueOf(patient.id), String.valueOf(doctorId)});
        
        while (cursor.moveToNext()) {
            details.append("• ").append(cursor.getString(0)).append("\n");
            details.append("  Traitement: ").append(cursor.getString(1)).append("\n");
            details.append("  Date: ").append(cursor.getString(2)).append("\n\n");
        }
        cursor.close();

        // Test results
        details.append("RÉSULTATS DE TESTS:\n");
        cursor = db.rawQuery(
            "SELECT test_name, result, test_date FROM test_results " +
            "WHERE patient_id = ? AND doctor_id = ? ORDER BY test_date DESC", 
            new String[]{String.valueOf(patient.id), String.valueOf(doctorId)});
        
        while (cursor.moveToNext()) {
            details.append("• ").append(cursor.getString(0)).append(": ");
            details.append(cursor.getString(1)).append("\n");
            details.append("  Date: ").append(cursor.getString(2)).append("\n\n");
        }
        cursor.close();

        new AlertDialog.Builder(this)
            .setTitle("Dossier Patient")
            .setMessage(details.toString())
            .setPositiveButton("Fermer", null)
            .show();
    }

    private static class Patient {
        int id;
        String fullName, dateOfBirth, bloodType;

        Patient(int id, String fullName, String dateOfBirth, String bloodType) {
            this.id = id;
            this.fullName = fullName;
            this.dateOfBirth = dateOfBirth;
            this.bloodType = bloodType;
        }
    }
}