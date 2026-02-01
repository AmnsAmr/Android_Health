package M.health;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import java.util.*;

public class ManageAppointmentsActivity extends AppCompatActivity {
    private DatabaseHelper dbHelper;
    private AuthManager authManager;
    private ListView lvAppointments;
    private List<AppointmentItem> appointments;
    private AppointmentListAdapter adapter;
    private Spinner spinnerFilter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_appointments);

        dbHelper = new DatabaseHelper(this);
        authManager = AuthManager.getInstance(this);

        lvAppointments = findViewById(R.id.lvAppointments);
        spinnerFilter = findViewById(R.id.spinnerFilter);
        Button btnCreateAppointment = findViewById(R.id.btnCreateAppointment);

        appointments = new ArrayList<>();
        adapter = new AppointmentListAdapter();
        lvAppointments.setAdapter(adapter);

        setupFilterSpinner();
        loadAppointments("pending");

        btnCreateAppointment.setOnClickListener(v -> showCreateAppointmentDialog());
    }

    private void setupFilterSpinner() {
        String[] filters = {"En attente", "Tous", "Programmés", "Annulés", "Terminés"};
        ArrayAdapter<String> filterAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, filters);
        filterAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerFilter.setAdapter(filterAdapter);

        spinnerFilter.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String[] statuses = {"pending", "all", "scheduled", "cancelled", "completed"};
                loadAppointments(statuses[position]);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void loadAppointments(String status) {
        appointments.clear();
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        String query = "SELECT a.id, a.appointment_datetime, a.status, " +
                "p.full_name as patient_name, d.full_name as doctor_name, doc.specialization " +
                "FROM appointments a " +
                "JOIN users p ON a.patient_id = p.id " +
                "JOIN users d ON a.doctor_id = d.id " +
                "LEFT JOIN doctors doc ON d.id = doc.user_id ";

        if (!status.equals("all")) {
            query += "WHERE a.status = ? ";
        }
        query += "ORDER BY a.appointment_datetime DESC";

        Cursor cursor = status.equals("all") ? 
            db.rawQuery(query, null) : 
            db.rawQuery(query, new String[]{status});

        while (cursor.moveToNext()) {
            appointments.add(new AppointmentItem(
                cursor.getInt(0),
                cursor.getString(1),
                cursor.getString(2),
                cursor.getString(3),
                cursor.getString(4),
                cursor.getString(5)
            ));
        }
        cursor.close();
        adapter.notifyDataSetChanged();
    }

    private void showCreateAppointmentDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_create_appointment, null);
        
        Spinner spinnerPatient = dialogView.findViewById(R.id.spinnerPatient);
        Spinner spinnerDoctor = dialogView.findViewById(R.id.spinnerDoctor);
        EditText etDate = dialogView.findViewById(R.id.etAppointmentDate);
        EditText etTime = dialogView.findViewById(R.id.etAppointmentTime);
        EditText etNotes = dialogView.findViewById(R.id.etAppointmentNotes);

        loadPatients(spinnerPatient);
        loadDoctors(spinnerDoctor);

        etDate.setOnClickListener(v -> showDatePicker(etDate));
        etTime.setOnClickListener(v -> showTimePicker(etTime));

        new AlertDialog.Builder(this)
            .setTitle("Créer Rendez-vous")
            .setView(dialogView)
            .setPositiveButton("Créer", (dialog, which) -> {
                if (spinnerPatient.getSelectedItem() != null && spinnerDoctor.getSelectedItem() != null) {
                    int patientId = ((SpinnerItem) spinnerPatient.getSelectedItem()).id;
                    int doctorId = ((SpinnerItem) spinnerDoctor.getSelectedItem()).id;
                    String dateTime = etDate.getText().toString() + " " + etTime.getText().toString() + ":00";
                    String notes = etNotes.getText().toString();
                    createAppointment(patientId, doctorId, dateTime, notes);
                }
            })
            .setNegativeButton("Annuler", null)
            .show();
    }

    private void loadPatients(Spinner spinner) {
        List<SpinnerItem> patients = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT id, full_name FROM users WHERE role = 'patient' AND is_active = 1", null);
        
        while (cursor.moveToNext()) {
            patients.add(new SpinnerItem(cursor.getInt(0), cursor.getString(1)));
        }
        cursor.close();

        ArrayAdapter<SpinnerItem> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, patients);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
    }

    private void loadDoctors(Spinner spinner) {
        List<SpinnerItem> doctors = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT id, full_name FROM users WHERE role = 'doctor' AND is_active = 1", null);
        
        while (cursor.moveToNext()) {
            doctors.add(new SpinnerItem(cursor.getInt(0), cursor.getString(1)));
        }
        cursor.close();

        ArrayAdapter<SpinnerItem> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, doctors);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
    }

    private void showDatePicker(EditText editText) {
        Calendar cal = Calendar.getInstance();
        new DatePickerDialog(this, (view, year, month, day) -> {
            editText.setText(String.format(Locale.getDefault(), "%04d-%02d-%02d", year, month + 1, day));
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void showTimePicker(EditText editText) {
        Calendar cal = Calendar.getInstance();
        new TimePickerDialog(this, (view, hour, minute) -> {
            editText.setText(String.format(Locale.getDefault(), "%02d:%02d", hour, minute));
        }, cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), true).show();
    }

    private void createAppointment(int patientId, int doctorId, String dateTime, String notes) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("patient_id", patientId);
        values.put("doctor_id", doctorId);
        values.put("appointment_datetime", dateTime);
        values.put("status", "scheduled");
        values.put("notes", notes);
        values.put("created_by", "secretary");

        long result = db.insert("appointments", null, values);
        if (result != -1) {
            Toast.makeText(this, "Rendez-vous créé", Toast.LENGTH_SHORT).show();
            loadAppointments("pending");
        }
    }

    private void showAppointmentOptions(AppointmentItem appointment) {
        List<String> optionsList = new ArrayList<>();
        optionsList.add("Modifier");
        
        if (appointment.status.equals("pending")) {
            optionsList.add("Confirmer");
            optionsList.add("Rejeter");
        } else if (appointment.status.equals("scheduled")) {
            optionsList.add("Annuler");
        }
        optionsList.add("Supprimer");

        String[] options = optionsList.toArray(new String[0]);
        
        new AlertDialog.Builder(this)
            .setTitle("Options")
            .setItems(options, (dialog, which) -> {
                if (which == 0) {
                    showEditDialog(appointment);
                } else if (appointment.status.equals("pending")) {
                    if (which == 1) updateAppointmentStatus(appointment.id, "scheduled");
                    else if (which == 2) updateAppointmentStatus(appointment.id, "cancelled");
                    else if (which == 3) deleteAppointment(appointment.id);
                } else if (appointment.status.equals("scheduled")) {
                    if (which == 1) updateAppointmentStatus(appointment.id, "cancelled");
                    else if (which == 2) deleteAppointment(appointment.id);
                } else {
                    if (which == 1) deleteAppointment(appointment.id);
                }
            })
            .show();
    }

    private void showEditDialog(AppointmentItem appointment) {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_create_appointment, null);
        
        EditText etDate = dialogView.findViewById(R.id.etAppointmentDate);
        EditText etTime = dialogView.findViewById(R.id.etAppointmentTime);
        EditText etNotes = dialogView.findViewById(R.id.etAppointmentNotes);

        String[] parts = appointment.dateTime.split(" ");
        if (parts.length >= 2) {
            etDate.setText(parts[0]);
            etTime.setText(parts[1].substring(0, 5));
        }

        etDate.setOnClickListener(v -> showDatePicker(etDate));
        etTime.setOnClickListener(v -> showTimePicker(etTime));

        new AlertDialog.Builder(this)
            .setTitle("Modifier Rendez-vous")
            .setView(dialogView)
            .setPositiveButton("Modifier", (dialog, which) -> {
                String dateTime = etDate.getText().toString() + " " + etTime.getText().toString() + ":00";
                String notes = etNotes.getText().toString();
                updateAppointment(appointment.id, dateTime, notes);
            })
            .setNegativeButton("Annuler", null)
            .show();
    }

    private void updateAppointment(int id, String dateTime, String notes) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("appointment_datetime", dateTime);
        values.put("notes", notes);

        int result = db.update("appointments", values, "id = ?", new String[]{String.valueOf(id)});
        if (result > 0) {
            Toast.makeText(this, "Rendez-vous modifié", Toast.LENGTH_SHORT).show();
            loadAppointments("pending");
        }
    }

    private void updateAppointmentStatus(int id, String status) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("status", status);

        int result = db.update("appointments", values, "id = ?", new String[]{String.valueOf(id)});
        if (result > 0) {
            String message = status.equals("scheduled") ? "Rendez-vous confirmé" : "Statut mis à jour";
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
            loadAppointments("pending");
        }
    }

    private void deleteAppointment(int id) {
        new AlertDialog.Builder(this)
            .setTitle("Confirmer")
            .setMessage("Supprimer ce rendez-vous?")
            .setPositiveButton("Oui", (dialog, which) -> {
                SQLiteDatabase db = dbHelper.getWritableDatabase();
                db.delete("appointments", "id = ?", new String[]{String.valueOf(id)});
                Toast.makeText(this, "Rendez-vous supprimé", Toast.LENGTH_SHORT).show();
                loadAppointments("pending");
            })
            .setNegativeButton("Non", null)
            .show();
    }

    private class AppointmentListAdapter extends BaseAdapter {
        @Override
        public int getCount() { return appointments.size(); }

        @Override
        public Object getItem(int position) { return appointments.get(position); }

        @Override
        public long getItemId(int position) { return appointments.get(position).id; }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(ManageAppointmentsActivity.this)
                        .inflate(R.layout.item_appointment_manage, parent, false);
            }

            AppointmentItem item = appointments.get(position);
            
            TextView tvDateTime = convertView.findViewById(R.id.tvAppointmentDateTime);
            TextView tvPatient = convertView.findViewById(R.id.tvAppointmentPatient);
            TextView tvDoctor = convertView.findViewById(R.id.tvAppointmentDoctor);
            TextView tvStatus = convertView.findViewById(R.id.tvAppointmentStatus);

            tvDateTime.setText(item.dateTime);
            tvPatient.setText("Patient: " + item.patientName);
            tvDoctor.setText("Dr. " + item.doctorName + " - " + (item.specialization != null ? item.specialization : ""));
            tvStatus.setText(item.status);

            convertView.setOnClickListener(v -> showAppointmentOptions(item));

            return convertView;
        }
    }

    static class AppointmentItem {
        int id;
        String dateTime, status, patientName, doctorName, specialization;

        AppointmentItem(int id, String dateTime, String status, String patientName, String doctorName, String specialization) {
            this.id = id;
            this.dateTime = dateTime;
            this.status = status;
            this.patientName = patientName;
            this.doctorName = doctorName;
            this.specialization = specialization;
        }

        public int getId() { return id; }
        public String getDateTime() { return dateTime; }
        public String getStatus() { return status; }
        public String getPatientName() { return patientName; }
        public String getPatientPhone() { return null; }
        public String getDoctorName() { return doctorName; }
        public String getSpecialization() { return specialization; }
    }

    private static class SpinnerItem {
        int id;
        String name;

        SpinnerItem(int id, String name) {
            this.id = id;
            this.name = name;
        }

        @Override
        public String toString() { return name; }
    }
}
