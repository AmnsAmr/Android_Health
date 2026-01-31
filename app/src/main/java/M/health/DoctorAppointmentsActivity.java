package M.health;

import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import java.util.ArrayList;
import java.util.List;

public class DoctorAppointmentsActivity extends AppCompatActivity {
    private DatabaseHelper dbHelper;
    private AuthManager authManager;
    private ListView appointmentsListView;
    private Spinner spinnerFilter;
    private int doctorId;
    private List<Appointment> appointments;
    private String currentFilter = "scheduled";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        dbHelper = new DatabaseHelper(this);
        authManager = AuthManager.getInstance(this);

        // Validate session and permissions
        if (!authManager.isLoggedIn() || !authManager.validateSession()) {
            redirectToLogin();
            return;
        }

        if (!authManager.hasPermission("doctor_manage_appointments")) {
            Toast.makeText(this, "Accès refusé", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        setContentView(R.layout.activity_doctor_appointments);

        doctorId = authManager.getUserId();
        appointmentsListView = findViewById(R.id.appointmentsListView);
        spinnerFilter = findViewById(R.id.spinnerFilter);
        appointments = new ArrayList<>();

        setupFilterSpinner();

        appointmentsListView.setOnItemClickListener((parent, view, position, id) ->
                showAppointmentOptions(appointments.get(position)));

        loadAppointments();
    }

    private void setupFilterSpinner() {
        String[] filters = {"À venir", "Tous", "Terminés", "Annulés"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, filters);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerFilter.setAdapter(adapter);

        spinnerFilter.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                switch (position) {
                    case 0: currentFilter = "scheduled"; break;
                    case 1: currentFilter = "all"; break;
                    case 2: currentFilter = "completed"; break;
                    case 3: currentFilter = "cancelled"; break;
                }
                loadAppointments();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void loadAppointments() {
        appointments.clear();
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        String whereClause = "a.doctor_id = ?";
        if (!currentFilter.equals("all")) {
            whereClause += " AND a.status = '" + currentFilter + "'";
        }

        String orderBy = currentFilter.equals("scheduled") ? "ASC" : "DESC";

        Cursor cursor = db.rawQuery(
                "SELECT a.id, u.full_name, a.appointment_datetime, a.status, a.notes " +
                        "FROM appointments a " +
                        "JOIN users u ON a.patient_id = u.id " +
                        "WHERE " + whereClause + " " +
                        "ORDER BY a.appointment_datetime " + orderBy,
                new String[]{String.valueOf(doctorId)});

        List<String> appointmentStrings = new ArrayList<>();
        while (cursor.moveToNext()) {
            Appointment appointment = new Appointment(
                    cursor.getInt(0),
                    cursor.getString(1),
                    cursor.getString(2),
                    cursor.getString(3),
                    cursor.getString(4)
            );
            appointments.add(appointment);
            appointmentStrings.add(appointment.patientName + " - " +
                    appointment.datetime + " (" + appointment.status + ")");
        }
        cursor.close();

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1, appointmentStrings);
        appointmentsListView.setAdapter(adapter);
    }

    private void showAppointmentOptions(Appointment appointment) {
        List<String> optionsList = new ArrayList<>();
        optionsList.add("Voir détails");
        
        if (appointment.status.equals("scheduled")) {
            optionsList.add("Marquer comme terminé");
            optionsList.add("Annuler");
        }

        String[] options = optionsList.toArray(new String[0]);

        new AlertDialog.Builder(this)
                .setTitle(appointment.patientName)
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        showAppointmentDetails(appointment);
                    } else if (appointment.status.equals("scheduled")) {
                        if (which == 1) {
                            completeAppointment(appointment.id);
                        } else if (which == 2) {
                            cancelAppointment(appointment.id);
                        }
                    }
                })
                .show();
    }

    private void showAppointmentDetails(Appointment appointment) {
        String details = "Patient: " + appointment.patientName + "\n" +
                "Date/Heure: " + appointment.datetime + "\n" +
                "Statut: " + appointment.status + "\n" +
                "Notes: " + (appointment.notes != null ? appointment.notes : "Aucune");

        new AlertDialog.Builder(this)
                .setTitle("Détails du Rendez-vous")
                .setMessage(details)
                .setPositiveButton("Fermer", null)
                .show();
    }

    private void updateAppointmentStatus(int appointmentId, String status) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("status", status);

        int result = db.update("appointments", values, "id = ?",
                new String[]{String.valueOf(appointmentId)});

        if (result > 0) {
            Toast.makeText(this, "Statut mis à jour", Toast.LENGTH_SHORT).show();
            loadAppointments();
        } else {
            Toast.makeText(this, "Erreur lors de la mise à jour", Toast.LENGTH_SHORT).show();
        }
    }

    private void completeAppointment(int appointmentId) {
        updateAppointmentStatus(appointmentId, "completed");
    }

    private void cancelAppointment(int appointmentId) {
        updateAppointmentStatus(appointmentId, "cancelled");
    }

    private void redirectToLogin() {
        Toast.makeText(this, "Session expirée", Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!authManager.isLoggedIn() || !authManager.validateSession()) {
            redirectToLogin();
            return;
        }
        loadAppointments();
    }

    private static class Appointment {
        int id;
        String patientName, datetime, status, notes;

        Appointment(int id, String patientName, String datetime, String status, String notes) {
            this.id = id;
            this.patientName = patientName;
            this.datetime = datetime;
            this.status = status;
            this.notes = notes;
        }
    }
}