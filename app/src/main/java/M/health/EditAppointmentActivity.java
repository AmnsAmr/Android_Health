package M.health;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import java.util.Calendar;
import java.util.Locale;

public class EditAppointmentActivity extends AppCompatActivity {
    private DatabaseHelper dbHelper;
    private int appointmentId;
    private EditText etDate, etTime, etNotes;
    private TextView tvPatient, tvDoctor;
    private Spinner spinnerStatus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_appointment);

        dbHelper = new DatabaseHelper(this);
        appointmentId = getIntent().getIntExtra("appointment_id", -1);

        tvPatient = findViewById(R.id.tvEditAppointmentPatient);
        tvDoctor = findViewById(R.id.tvEditAppointmentDoctor);
        etDate = findViewById(R.id.etEditAppointmentDate);
        etTime = findViewById(R.id.etEditAppointmentTime);
        etNotes = findViewById(R.id.etEditAppointmentNotes);
        spinnerStatus = findViewById(R.id.spinnerEditAppointmentStatus);
        Button btnSave = findViewById(R.id.btnSaveAppointment);

        setupStatusSpinner();
        loadAppointmentData();

        etDate.setOnClickListener(v -> showDatePicker());
        etTime.setOnClickListener(v -> showTimePicker());
        btnSave.setOnClickListener(v -> saveAppointment());
    }

    private void setupStatusSpinner() {
        String[] statuses = {"scheduled", "cancelled", "completed"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, statuses);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerStatus.setAdapter(adapter);
    }

    private void loadAppointmentData() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery(
            "SELECT a.appointment_datetime, a.status, a.notes, " +
            "p.full_name as patient_name, d.full_name as doctor_name " +
            "FROM appointments a " +
            "JOIN users p ON a.patient_id = p.id " +
            "JOIN users d ON a.doctor_id = d.id " +
            "WHERE a.id = ?", new String[]{String.valueOf(appointmentId)});

        if (cursor.moveToFirst()) {
            String dateTime = cursor.getString(0);
            String[] parts = dateTime.split(" ");
            if (parts.length >= 2) {
                etDate.setText(parts[0]);
                etTime.setText(parts[1].substring(0, 5));
            }
            
            tvPatient.setText("Patient: " + cursor.getString(3));
            tvDoctor.setText("Médecin: Dr. " + cursor.getString(4));
            etNotes.setText(cursor.getString(2));
            
            String status = cursor.getString(1);
            for (int i = 0; i < spinnerStatus.getCount(); i++) {
                if (spinnerStatus.getItemAtPosition(i).toString().equals(status)) {
                    spinnerStatus.setSelection(i);
                    break;
                }
            }
        }
        cursor.close();
    }

    private void showDatePicker() {
        Calendar cal = Calendar.getInstance();
        new DatePickerDialog(this, (view, year, month, day) -> {
            etDate.setText(String.format(Locale.getDefault(), "%04d-%02d-%02d", year, month + 1, day));
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void showTimePicker() {
        Calendar cal = Calendar.getInstance();
        new TimePickerDialog(this, (view, hour, minute) -> {
            etTime.setText(String.format(Locale.getDefault(), "%02d:%02d", hour, minute));
        }, cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), true).show();
    }

    private void saveAppointment() {
        String dateTime = etDate.getText().toString() + " " + etTime.getText().toString() + ":00";
        String status = spinnerStatus.getSelectedItem().toString();
        String notes = etNotes.getText().toString();

        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("appointment_datetime", dateTime);
        values.put("status", status);
        values.put("notes", notes);

        int result = db.update("appointments", values, "id = ?", new String[]{String.valueOf(appointmentId)});
        
        if (result > 0) {
            Toast.makeText(this, "Rendez-vous mis à jour", Toast.LENGTH_SHORT).show();
            finish();
        } else {
            Toast.makeText(this, "Erreur de mise à jour", Toast.LENGTH_SHORT).show();
        }
    }
}
