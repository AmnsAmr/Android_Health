package M.health;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import java.text.SimpleDateFormat;
import java.util.*;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import java.text.SimpleDateFormat;
import java.util.*;

public class DoctorSchedulesActivity extends AppCompatActivity {
    private DatabaseHelper dbHelper;
    private ListView lvDoctors;
    private List<DoctorSchedule> doctors;
    private DoctorScheduleAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_doctor_schedules);

        dbHelper = new DatabaseHelper(this);
        lvDoctors = findViewById(R.id.lvDoctorSchedules);

        doctors = new ArrayList<>();
        adapter = new DoctorScheduleAdapter();
        lvDoctors.setAdapter(adapter);

        loadDoctorSchedules();
    }

    private void loadDoctorSchedules() {
        doctors.clear();
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        String today = dateFormat.format(new Date());

        String query = "SELECT u.id, u.full_name, d.specialization, " +
                "(SELECT COUNT(*) FROM appointments WHERE doctor_id = u.id AND DATE(appointment_datetime) = ? AND status = 'scheduled') as today_count, " +
                "(SELECT COUNT(*) FROM appointments WHERE doctor_id = u.id AND DATE(appointment_datetime) > ? AND status = 'scheduled') as upcoming_count " +
                "FROM users u " +
                "JOIN doctors d ON u.id = d.user_id " +
                "WHERE u.role = 'doctor' AND u.is_active = 1 " +
                "ORDER BY u.full_name ASC";

        Cursor cursor = db.rawQuery(query, new String[]{today, today});

        while (cursor.moveToNext()) {
            doctors.add(new DoctorSchedule(
                cursor.getInt(0),
                cursor.getString(1),
                cursor.getString(2),
                cursor.getInt(3),
                cursor.getInt(4)
            ));
        }
        cursor.close();
        adapter.notifyDataSetChanged();
    }

    private void showDoctorAppointments(int doctorId, String doctorName) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        
        String query = "SELECT a.appointment_datetime, p.full_name, a.status " +
                "FROM appointments a " +
                "JOIN users p ON a.patient_id = p.id " +
                "WHERE a.doctor_id = ? AND DATE(a.appointment_datetime) >= DATE('now') " +
                "ORDER BY a.appointment_datetime ASC";

        Cursor cursor = db.rawQuery(query, new String[]{String.valueOf(doctorId)});

        StringBuilder schedule = new StringBuilder();
        while (cursor.moveToNext()) {
            schedule.append(cursor.getString(0))
                    .append(" - ")
                    .append(cursor.getString(1))
                    .append(" (")
                    .append(cursor.getString(2))
                    .append(")\n");
        }
        cursor.close();

        if (schedule.length() == 0) {
            schedule.append("Aucun rendez-vous programmé");
        }

        new android.app.AlertDialog.Builder(this)
            .setTitle("Horaires - Dr. " + doctorName)
            .setMessage(schedule.toString())
            .setPositiveButton("Fermer", null)
            .show();
    }

    private class DoctorScheduleAdapter extends BaseAdapter {
        @Override
        public int getCount() { return doctors.size(); }

        @Override
        public Object getItem(int position) { return doctors.get(position); }

        @Override
        public long getItemId(int position) { return doctors.get(position).id; }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(DoctorSchedulesActivity.this)
                        .inflate(R.layout.item_doctor_schedule, parent, false);
            }

            DoctorSchedule doctor = doctors.get(position);

            TextView tvName = convertView.findViewById(R.id.tvDoctorScheduleName);
            TextView tvSpecialization = convertView.findViewById(R.id.tvDoctorScheduleSpecialization);
            TextView tvToday = convertView.findViewById(R.id.tvDoctorScheduleToday);
            TextView tvUpcoming = convertView.findViewById(R.id.tvDoctorScheduleUpcoming);
            Button btnViewSchedule = convertView.findViewById(R.id.btnViewDoctorSchedule);

            tvName.setText("Dr. " + doctor.name);
            tvSpecialization.setText(doctor.specialization);
            tvToday.setText("Aujourd'hui: " + doctor.todayCount);
            tvUpcoming.setText("À venir: " + doctor.upcomingCount);

            btnViewSchedule.setOnClickListener(v -> showDoctorAppointments(doctor.id, doctor.name));

            return convertView;
        }
    }

    private static class DoctorSchedule {
        int id, todayCount, upcomingCount;
        String name, specialization;

        DoctorSchedule(int id, String name, String specialization, int todayCount, int upcomingCount) {
            this.id = id;
            this.name = name;
            this.specialization = specialization;
            this.todayCount = todayCount;
            this.upcomingCount = upcomingCount;
        }
    }
}
