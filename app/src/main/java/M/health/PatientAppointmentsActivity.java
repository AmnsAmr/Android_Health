package M.health;

import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class PatientAppointmentsActivity extends AppCompatActivity {
    private DatabaseHelper dbHelper;
    private AuthManager authManager;
    private LinearLayout appointmentsContainer;
    private int patientId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_patient_appointments);

        dbHelper = new DatabaseHelper(this);
        authManager = AuthManager.getInstance(this);
        
        if (!authManager.isLoggedIn()) {
            finish();
            return;
        }

        patientId = authManager.getCurrentUser().id;
        
        View userProfileHeader = findViewById(R.id.userProfileHeader);
        UIHelper.setupUserProfileHeader(this, userProfileHeader, authManager);
        
        appointmentsContainer = findViewById(R.id.appointmentsContainer);
        
        findViewById(R.id.btnNewAppointment).setOnClickListener(v -> {
            Intent intent = new Intent(this, book_appointment.class);
            startActivity(intent);
        });
        
        loadAppointments();
    }

    private void loadAppointments() {
        appointmentsContainer.removeAllViews();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        
        Cursor cursor = db.rawQuery(
            "SELECT a.id, u.full_name, d.specialization, a.appointment_datetime, a.status, a.notes " +
            "FROM appointments a " +
            "JOIN users u ON a.doctor_id = u.id " +
            "JOIN doctors d ON a.doctor_id = d.user_id " +
            "WHERE a.patient_id = ? AND a.status != 'cancelled' " +
            "ORDER BY a.appointment_datetime ASC", 
            new String[]{String.valueOf(patientId)});

        while (cursor.moveToNext()) {
            addAppointmentCard(
                cursor.getInt(0),
                cursor.getString(1),
                cursor.getString(2),
                cursor.getString(3),
                cursor.getString(4),
                cursor.getString(5)
            );
        }
        cursor.close();
    }

    private void addAppointmentCard(int appointmentId, String doctorName, String specialty, 
                                  String datetime, String status, String notes) {
        View cardView = getLayoutInflater().inflate(R.layout.item_appointment_card, null);
        
        TextView tvDoctor = cardView.findViewById(R.id.tvDoctor);
        TextView tvSpecialty = cardView.findViewById(R.id.tvSpecialty);
        TextView tvDateTime = cardView.findViewById(R.id.tvDateTime);
        TextView tvStatus = cardView.findViewById(R.id.tvStatus);
        TextView tvNotes = cardView.findViewById(R.id.tvNotes);
        
        tvDoctor.setText("Dr. " + doctorName);
        tvSpecialty.setText(specialty);
        tvDateTime.setText(datetime);
        tvStatus.setText(status.toUpperCase());
        tvNotes.setText(notes != null ? notes : "");

        cardView.findViewById(R.id.btnCancel).setOnClickListener(v -> 
            showCancelDialog(appointmentId, doctorName));
        
        appointmentsContainer.addView(cardView);
    }

    private void showCancelDialog(int appointmentId, String doctorName) {
        new AlertDialog.Builder(this)
            .setTitle("Annuler le rendez-vous")
            .setMessage("Voulez-vous annuler votre rendez-vous avec Dr. " + doctorName + " ?")
            .setPositiveButton("Annuler RDV", (dialog, which) -> cancelAppointment(appointmentId))
            .setNegativeButton("Retour", null)
            .show();
    }

    private void cancelAppointment(int appointmentId) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("status", "cancelled");
        
        int result = db.update("appointments", values, "id = ?", 
            new String[]{String.valueOf(appointmentId)});
        
        if (result > 0) {
            Toast.makeText(this, "Rendez-vous annulé", Toast.LENGTH_SHORT).show();
            loadAppointments();
        } else {
            Toast.makeText(this, "Erreur lors de l'annulation", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadAppointments();
    }
}