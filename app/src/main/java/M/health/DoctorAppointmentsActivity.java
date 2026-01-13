package M.health;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import java.util.ArrayList;
import java.util.List;

public class DoctorAppointmentsActivity extends AppCompatActivity {
    private DatabaseHelper dbHelper;
    private ListView appointmentsListView;
    private int doctorId;
    private List<Appointment> appointments;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_doctor_appointments);

        dbHelper = new DatabaseHelper(this);
        doctorId = getIntent().getIntExtra("doctor_id", -1);
        appointmentsListView = findViewById(R.id.appointmentsListView);
        appointments = new ArrayList<>();

        appointmentsListView.setOnItemClickListener((parent, view, position, id) -> 
            showAppointmentOptions(appointments.get(position)));

        loadAppointments();
    }

    private void loadAppointments() {
        appointments.clear();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        
        Cursor cursor = db.rawQuery(
            "SELECT a.id, u.full_name, a.appointment_datetime, a.status, a.notes " +
            "FROM appointments a " +
            "JOIN users u ON a.patient_id = u.id " +
            "WHERE a.doctor_id = ? ORDER BY a.appointment_datetime DESC", 
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
        String[] options = {"Voir détails", "Marquer comme terminé", "Annuler"};
        
        new AlertDialog.Builder(this)
            .setTitle(appointment.patientName)
            .setItems(options, (dialog, which) -> {
                switch (which) {
                    case 0:
                        showAppointmentDetails(appointment);
                        break;
                    case 1:
                        updateAppointmentStatus(appointment.id, "completed");
                        break;
                    case 2:
                        updateAppointmentStatus(appointment.id, "cancelled");
                        break;
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
            loadAppointments();
        }
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