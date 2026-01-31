package M.health;

import android.app.AlertDialog;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import java.util.ArrayList;
import java.util.List;

public class RdvListActivity extends AppCompatActivity {
    private DatabaseHelper dbHelper;
    private AuthManager authManager;
    private ListView lvRendezVous;
    private List<Appointment> appointments;
    private Button btnRetour;
    private RdvAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rdv_list);

        dbHelper = new DatabaseHelper(this);
        authManager = AuthManager.getInstance(this);
        
        if (!authManager.hasPermission("secretary_manage_appointments")) {
            Toast.makeText(this, "Accès refusé", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        lvRendezVous = findViewById(R.id.lv_rendezvous);
        btnRetour = findViewById(R.id.btn_retour_planning);
        
        appointments = new ArrayList<>();
        adapter = new RdvAdapter();
        lvRendezVous.setAdapter(adapter);
        
        loadAppointments();
        
        btnRetour.setOnClickListener(v -> finish());
    }

    private void loadAppointments() {
        appointments.clear();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        
        Cursor cursor = db.rawQuery(
            "SELECT a.id, p.full_name as patient_name, d.full_name as doctor_name, " +
            "a.appointment_datetime, a.status, a.notes, COALESCE(doc.specialization, 'Généraliste') as specialization " +
            "FROM appointments a " +
            "JOIN users p ON a.patient_id = p.id " +
            "JOIN users d ON a.doctor_id = d.id " +
            "LEFT JOIN doctors doc ON a.doctor_id = doc.user_id " +
            "WHERE a.status != 'cancelled' " +
            "ORDER BY a.appointment_datetime ASC", null);

        while (cursor.moveToNext()) {
            appointments.add(new Appointment(
                cursor.getInt(0),
                cursor.getString(1),
                cursor.getString(2),
                cursor.getString(3),
                cursor.getString(4),
                cursor.getString(5),
                cursor.getString(6)
            ));
        }
        cursor.close();
        adapter.notifyDataSetChanged();
    }

    private class RdvAdapter extends BaseAdapter {
        @Override
        public int getCount() {
            return appointments.size();
        }

        @Override
        public Object getItem(int position) {
            return appointments.get(position);
        }

        @Override
        public long getItemId(int position) {
            return appointments.get(position).id;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(RdvListActivity.this)
                        .inflate(R.layout.activity_item_rdv, parent, false);
            }

            Appointment appointment = appointments.get(position);
            
            TextView tvNom = convertView.findViewById(R.id.tv_patient_nom);
            TextView tvDetails = convertView.findViewById(R.id.tv_rdv_details);
            Button btnConfirm = convertView.findViewById(R.id.btn_confirm);
            Button btnCancel = convertView.findViewById(R.id.btn_cancel);

            tvNom.setText(appointment.patientName);
            tvDetails.setText("Dr. " + appointment.doctorName + " (" + appointment.specialization + ")\n" +
                            appointment.datetime + "\n" + appointment.status.toUpperCase());

            btnConfirm.setOnClickListener(v -> confirmAppointment(appointment.id, position));
            btnCancel.setOnClickListener(v -> cancelAppointment(appointment.id, position));

            return convertView;
        }
    }

    private void confirmAppointment(int appointmentId, int position) {
        new AlertDialog.Builder(this)
            .setTitle("Confirmer le rendez-vous")
            .setMessage("Confirmer ce rendez-vous ?")
            .setPositiveButton("Confirmer", (dialog, which) -> {
                updateAppointmentStatus(appointmentId, "completed", position);
            })
            .setNegativeButton("Annuler", null)
            .show();
    }

    private void cancelAppointment(int appointmentId, int position) {
        new AlertDialog.Builder(this)
            .setTitle("Annuler le rendez-vous")
            .setMessage("Annuler ce rendez-vous ?")
            .setPositiveButton("Annuler RDV", (dialog, which) -> {
                updateAppointmentStatus(appointmentId, "cancelled", position);
            })
            .setNegativeButton("Retour", null)
            .show();
    }

    private void updateAppointmentStatus(int appointmentId, String status, int position) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("status", status);
        
        int result = db.update("appointments", values, "id = ?", 
            new String[]{String.valueOf(appointmentId)});
        
        if (result > 0) {
            String message = status.equals("cancelled") ? "Rendez-vous annulé" : "Rendez-vous confirmé";
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
            loadAppointments();
        } else {
            Toast.makeText(this, "Erreur lors de la mise à jour", Toast.LENGTH_SHORT).show();
        }
    }

    private static class Appointment {
        int id;
        String patientName, doctorName, datetime, status, notes, specialization;
        
        Appointment(int id, String patientName, String doctorName, String datetime, 
                   String status, String notes, String specialization) {
            this.id = id;
            this.patientName = patientName;
            this.doctorName = doctorName;
            this.datetime = datetime;
            this.status = status;
            this.notes = notes;
            this.specialization = specialization;
        }
    }
}