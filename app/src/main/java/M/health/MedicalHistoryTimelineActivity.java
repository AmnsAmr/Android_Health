package M.health;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import java.util.ArrayList;
import java.util.List;

public class MedicalHistoryTimelineActivity extends AppCompatActivity {
    private DatabaseHelper dbHelper;
    private AuthManager authManager;
    private LinearLayout timelineContainer;
    private Spinner spinnerFilter;
    private int patientId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_medical_history_timeline);

        dbHelper = new DatabaseHelper(this);
        authManager = AuthManager.getInstance(this);
        
        if (!authManager.isLoggedIn()) {
            finish();
            return;
        }

        patientId = authManager.getCurrentUser().id;
        
        View headerView = findViewById(R.id.headerLayout);
        UIHelper.setupHeaderWithSignOut(this, headerView, "Historique Médical", authManager);
        
        timelineContainer = findViewById(R.id.timelineContainer);
        spinnerFilter = findViewById(R.id.spinnerFilter);
        
        setupFilter();
        loadMedicalHistory("all");
        
        spinnerFilter.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                String[] filters = {"all", "appointments", "prescriptions", "test_results", "medical_records"};
                loadMedicalHistory(filters[position]);
            }
            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });
    }

    private void setupFilter() {
        String[] filterOptions = {"Tout", "Rendez-vous", "Prescriptions", "Résultats", "Dossiers"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, 
            android.R.layout.simple_spinner_item, filterOptions);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerFilter.setAdapter(adapter);
    }

    private void loadMedicalHistory(String filter) {
        timelineContainer.removeAllViews();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        
        List<TimelineItem> items = new ArrayList<>();
        
        if (filter.equals("all") || filter.equals("appointments")) {
            loadAppointments(db, items);
        }
        if (filter.equals("all") || filter.equals("prescriptions")) {
            loadPrescriptions(db, items);
        }
        if (filter.equals("all") || filter.equals("test_results")) {
            loadTestResults(db, items);
        }
        if (filter.equals("all") || filter.equals("medical_records")) {
            loadMedicalRecords(db, items);
        }
        
        // Sort by date descending
        items.sort((a, b) -> b.date.compareTo(a.date));
        
        for (TimelineItem item : items) {
            addTimelineItem(item);
        }
    }

    private void loadAppointments(SQLiteDatabase db, List<TimelineItem> items) {
        Cursor cursor = db.rawQuery(
            "SELECT a.appointment_datetime, u.full_name, a.notes, a.status " +
            "FROM appointments a " +
            "JOIN users u ON a.doctor_id = u.id " +
            "WHERE a.patient_id = ? ORDER BY a.appointment_datetime DESC", 
            new String[]{String.valueOf(patientId)});

        while (cursor.moveToNext()) {
            items.add(new TimelineItem(
                cursor.getString(0),
                "Rendez-vous",
                "Dr. " + cursor.getString(1),
                cursor.getString(2) != null ? cursor.getString(2) : "Statut: " + cursor.getString(3),
                "#2196F3"
            ));
        }
        cursor.close();
    }

    private void loadPrescriptions(SQLiteDatabase db, List<TimelineItem> items) {
        Cursor cursor = db.rawQuery(
            "SELECT p.created_at, u.full_name, p.medication, p.dosage " +
            "FROM prescriptions p " +
            "JOIN users u ON p.doctor_id = u.id " +
            "WHERE p.patient_id = ? ORDER BY p.created_at DESC", 
            new String[]{String.valueOf(patientId)});

        while (cursor.moveToNext()) {
            items.add(new TimelineItem(
                cursor.getString(0),
                "Prescription",
                "Dr. " + cursor.getString(1),
                cursor.getString(2) + " - " + cursor.getString(3),
                "#4CAF50"
            ));
        }
        cursor.close();
    }

    private void loadTestResults(SQLiteDatabase db, List<TimelineItem> items) {
        Cursor cursor = db.rawQuery(
            "SELECT t.test_date, u.full_name, t.test_name, t.result " +
            "FROM test_results t " +
            "JOIN users u ON t.doctor_id = u.id " +
            "WHERE t.patient_id = ? ORDER BY t.test_date DESC", 
            new String[]{String.valueOf(patientId)});

        while (cursor.moveToNext()) {
            items.add(new TimelineItem(
                cursor.getString(0),
                "Résultat",
                "Dr. " + cursor.getString(1),
                cursor.getString(2) + ": " + cursor.getString(3),
                "#FF9800"
            ));
        }
        cursor.close();
    }

    private void loadMedicalRecords(SQLiteDatabase db, List<TimelineItem> items) {
        Cursor cursor = db.rawQuery(
            "SELECT m.created_at, u.full_name, m.diagnosis, m.treatment " +
            "FROM medical_records m " +
            "JOIN users u ON m.doctor_id = u.id " +
            "WHERE m.patient_id = ? ORDER BY m.created_at DESC", 
            new String[]{String.valueOf(patientId)});

        while (cursor.moveToNext()) {
            items.add(new TimelineItem(
                cursor.getString(0),
                "Dossier",
                "Dr. " + cursor.getString(1),
                "Diagnostic: " + cursor.getString(2) + "\nTraitement: " + cursor.getString(3),
                "#9C27B0"
            ));
        }
        cursor.close();
    }

    private void addTimelineItem(TimelineItem item) {
        View itemView = getLayoutInflater().inflate(R.layout.item_timeline, null);
        
        TextView tvDate = itemView.findViewById(R.id.tvDate);
        TextView tvType = itemView.findViewById(R.id.tvType);
        TextView tvDoctor = itemView.findViewById(R.id.tvDoctor);
        TextView tvDetails = itemView.findViewById(R.id.tvDetails);
        View colorIndicator = itemView.findViewById(R.id.colorIndicator);
        
        tvDate.setText(item.date);
        tvType.setText(item.type);
        tvDoctor.setText(item.doctor);
        tvDetails.setText(item.details);
        colorIndicator.setBackgroundColor(android.graphics.Color.parseColor(item.color));
        
        timelineContainer.addView(itemView);
    }

    private static class TimelineItem {
        String date, type, doctor, details, color;
        
        TimelineItem(String date, String type, String doctor, String details, String color) {
            this.date = date;
            this.type = type;
            this.doctor = doctor;
            this.details = details;
            this.color = color;
        }
    }
}