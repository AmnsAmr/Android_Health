package M.health;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class DoctorSchedulesActivity extends AppCompatActivity {

    private DatabaseHelper dbHelper;
    private AuthManager authManager;
    private ListView lvDoctors;
    private List<DoctorInfo> doctorList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_doctor_schedules);

        dbHelper = new DatabaseHelper(this);
        authManager = AuthManager.getInstance(this);

        if (!authManager.isLoggedIn()) {
            finish();
            return;
        }

        lvDoctors = findViewById(R.id.lvDoctors);
        doctorList = new ArrayList<>();

        loadDoctors();
    }

    private void loadDoctors() {
        doctorList.clear();
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        // Query to get doctors and their specialization
        Cursor cursor = db.rawQuery(
                "SELECT u.id, u.full_name, d.specialization " +
                        "FROM users u " +
                        "JOIN doctors d ON u.id = d.user_id " +
                        "WHERE u.role = 'doctor' AND u.is_active = 1",
                null);

        while (cursor.moveToNext()) {
            doctorList.add(new DoctorInfo(
                    cursor.getInt(0),
                    cursor.getString(1),
                    cursor.getString(2)
            ));
        }
        cursor.close();

        // Custom Adapter to match the Card design
        ArrayAdapter<DoctorInfo> adapter = new ArrayAdapter<DoctorInfo>(this, android.R.layout.simple_list_item_2, doctorList) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View view = super.getView(position, convertView, parent);

                // Styling the list item to look like a card
                view.setBackgroundResource(android.R.drawable.dialog_holo_light_frame);
                view.setPadding(30, 30, 30, 30);
                view.setBackgroundColor(getResources().getColor(android.R.color.white));
                view.setElevation(4f);

                TextView text1 = view.findViewById(android.R.id.text1);
                TextView text2 = view.findViewById(android.R.id.text2);

                DoctorInfo doc = getItem(position);
                text1.setText(doc.name);
                text1.setTypeface(null, android.graphics.Typeface.BOLD);
                text1.setTextColor(getResources().getColor(android.R.color.black));
                text2.setText(doc.specialization);

                return view;
            }
        };

        lvDoctors.setAdapter(adapter);
        lvDoctors.setOnItemClickListener((parent, view, position, id) ->
                showDoctorScheduleDialog(doctorList.get(position)));
    }

    private void showDoctorScheduleDialog(DoctorInfo doctor) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        String today = sdf.format(new Date());

        Cursor cursor = db.rawQuery(
                "SELECT u.full_name, a.appointment_datetime, a.status " +
                        "FROM appointments a " +
                        "JOIN users u ON a.patient_id = u.id " +
                        "WHERE a.doctor_id = ? AND DATE(a.appointment_datetime) = ? " +
                        "ORDER BY a.appointment_datetime ASC",
                new String[]{String.valueOf(doctor.id), today});

        StringBuilder schedule = new StringBuilder();
        if (cursor.getCount() == 0) {
            schedule.append("Aucun rendez-vous prévu aujourd'hui.");
        } else {
            while (cursor.moveToNext()) {
                String time = cursor.getString(1).substring(11, 16); // Extract HH:mm
                schedule.append(time).append(" - ").append(cursor.getString(0))
                        .append(" (").append(cursor.getString(2)).append(")\n\n");
            }
        }
        cursor.close();

        new AlertDialog.Builder(this)
                .setTitle("Planning: " + doctor.name)
                .setMessage(schedule.toString())
                .setPositiveButton("Fermer", null)
                .show();
    }

    private static class DoctorInfo {
        int id;
        String name;
        String specialization;

        DoctorInfo(int id, String name, String specialization) {
            this.id = id;
            this.name = name;
            this.specialization = specialization;
        }

        @Override
        public String toString() {
            return name + "\n" + specialization;
        }
    }
}