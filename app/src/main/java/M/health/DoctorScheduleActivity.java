package M.health;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import java.util.ArrayList;
import java.util.List;

public class DoctorScheduleActivity extends AppCompatActivity {
    private DatabaseHelper dbHelper;
    private AuthManager authManager;
    private Spinner spinnerDoctors;
    private LinearLayout scheduleContainer;
    private List<Integer> doctorIds;
    private List<String> doctorNames;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_doctor_schedule);

        dbHelper = new DatabaseHelper(this);
        authManager = AuthManager.getInstance(this);
        
        if (!authManager.isLoggedIn()) {
            finish();
            return;
        }

        View headerView = findViewById(R.id.headerLayout);
        UIHelper.setupHeaderWithSignOut(this, headerView, "Planning Médecins", authManager);
        
        spinnerDoctors = findViewById(R.id.spinnerDoctors);
        scheduleContainer = findViewById(R.id.scheduleContainer);
        
        loadDoctors();
        
        spinnerDoctors.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                if (!doctorIds.isEmpty()) {
                    loadDoctorSchedule(doctorIds.get(position));
                }
            }
            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });
    }

    private void loadDoctors() {
        doctorIds = new ArrayList<>();
        doctorNames = new ArrayList<>();
        
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery(
            "SELECT u.id, u.full_name, d.specialization " +
            "FROM users u " +
            "JOIN doctors d ON u.id = d.user_id " +
            "WHERE u.role = 'doctor'", null);

        while (cursor.moveToNext()) {
            doctorIds.add(cursor.getInt(0));
            doctorNames.add("Dr. " + cursor.getString(1) + " - " + cursor.getString(2));
        }
        cursor.close();

        if (!doctorNames.isEmpty()) {
            ArrayAdapter<String> adapter = new ArrayAdapter<>(this, 
                android.R.layout.simple_spinner_item, doctorNames);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinnerDoctors.setAdapter(adapter);
            
            loadDoctorSchedule(doctorIds.get(0));
        }
    }

    private void loadDoctorSchedule(int doctorId) {
        scheduleContainer.removeAllViews();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        
        // Load appointments for next 7 days
        Cursor cursor = db.rawQuery(
            "SELECT a.appointment_datetime, u.full_name, a.status, a.notes " +
            "FROM appointments a " +
            "JOIN users u ON a.patient_id = u.id " +
            "WHERE a.doctor_id = ? AND date(a.appointment_datetime) >= date('now') " +
            "AND date(a.appointment_datetime) <= date('now', '+7 days') " +
            "ORDER BY a.appointment_datetime ASC", 
            new String[]{String.valueOf(doctorId)});

        String currentDate = "";
        LinearLayout dayContainer = null;
        
        while (cursor.moveToNext()) {
            String datetime = cursor.getString(0);
            String date = datetime.split(" ")[0];
            String time = datetime.split(" ")[1];
            String patientName = cursor.getString(1);
            String status = cursor.getString(2);
            String notes = cursor.getString(3);
            
            if (!date.equals(currentDate)) {
                currentDate = date;
                dayContainer = addDateHeader(date);
            }
            
            addAppointmentSlot(dayContainer, time, patientName, status, notes);
        }
        cursor.close();
        
        if (scheduleContainer.getChildCount() == 0) {
            addEmptyState();
        }
    }

    private LinearLayout addDateHeader(String date) {
        View headerView = getLayoutInflater().inflate(R.layout.item_schedule_date_header, null);
        TextView tvDate = headerView.findViewById(R.id.tvDate);
        tvDate.setText(date);
        scheduleContainer.addView(headerView);
        
        LinearLayout dayContainer = new LinearLayout(this);
        dayContainer.setOrientation(LinearLayout.VERTICAL);
        scheduleContainer.addView(dayContainer);
        
        return dayContainer;
    }

    private void addAppointmentSlot(LinearLayout dayContainer, String time, String patientName, 
                                  String status, String notes) {
        View slotView = getLayoutInflater().inflate(R.layout.item_appointment_slot, null);
        
        TextView tvTime = slotView.findViewById(R.id.tvTime);
        TextView tvPatient = slotView.findViewById(R.id.tvPatient);
        TextView tvStatus = slotView.findViewById(R.id.tvStatus);
        TextView tvNotes = slotView.findViewById(R.id.tvNotes);
        
        tvTime.setText(time);
        tvPatient.setText(patientName);
        tvStatus.setText(status.toUpperCase());
        tvNotes.setText(notes != null ? notes : "");
        
        // Color code by status
        int color = status.equals("scheduled") ? 0xFF4CAF50 : 
                   status.equals("cancelled") ? 0xFFE74C3C : 0xFFFF9800;
        tvStatus.setTextColor(color);
        
        dayContainer.addView(slotView);
    }

    private void addEmptyState() {
        TextView emptyView = new TextView(this);
        emptyView.setText("Aucun rendez-vous programmé pour les 7 prochains jours");
        emptyView.setTextSize(16);
        emptyView.setPadding(32, 64, 32, 64);
        emptyView.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        scheduleContainer.addView(emptyView);
    }
}