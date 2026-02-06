package M.health;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class SecretaryDashboardActivity extends AppCompatActivity {

    private DatabaseHelper dbHelper;
    private AuthManager authManager;

    private TextView tvSecretaryName;
    private TextView tvCurrentDate;
    private TextView tvTodayAppointmentsCount;
    private TextView tvUrgentRequestsCount;
    private RecyclerView rvTodayAppointments;

    private CardView cardManageAppointments;
    private CardView cardViewPatients;
    private CardView cardDoctorSchedules;
    private CardView cardUrgentRequests;

    private AppointmentAdapter appointmentAdapter;
    private List<Appointment> todayAppointments;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        authManager = AuthManager.getInstance(this);
        dbHelper = new DatabaseHelper(this);

        // Verify secretary role and permissions
        if (!validateAccess()) {
            redirectToLogin();
            return;
        }

        setContentView(R.layout.activity_secretary_dashboard);

        initializeViews();
        loadSecretaryInfo();
        loadDashboardStats();
        loadTodayAppointments();
        setupClickListeners();
    }

    /**
     * Validate that current user is a secretary with proper permissions
     */
    private boolean validateAccess() {
        if (!authManager.isLoggedIn()) {
            Toast.makeText(this, "Session expirée", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (!authManager.hasRole("secretary")) {
            Toast.makeText(this, "Accès refusé - Rôle incorrect", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (!authManager.hasPermission("secretary_view_patient_list")) {
            Toast.makeText(this, "Accès refusé - Permissions insuffisantes", Toast.LENGTH_SHORT).show();
            return false;
        }

        return true;
    }

    private void initializeViews() {
        tvSecretaryName = findViewById(R.id.tvSecretaryName);
        tvCurrentDate = findViewById(R.id.tvCurrentDate);
        tvTodayAppointmentsCount = findViewById(R.id.tvTodayAppointmentsCount);
        tvUrgentRequestsCount = findViewById(R.id.tvUrgentRequestsCount);
        rvTodayAppointments = findViewById(R.id.rvTodayAppointments);

        cardManageAppointments = findViewById(R.id.cardManageAppointments);
        cardViewPatients = findViewById(R.id.cardViewPatients);
        cardDoctorSchedules = findViewById(R.id.cardDoctorSchedules);
        cardUrgentRequests = findViewById(R.id.cardUrgentRequests);

        // Setup header
        ((TextView) findViewById(R.id.tvUserName)).setText(authManager.getCurrentUser().fullName);
        ((TextView) findViewById(R.id.tvUserRole)).setText("Secrétaire Médicale");
        findViewById(R.id.tvUserInfo).setVisibility(View.GONE);
        findViewById(R.id.btnSettings).setOnClickListener(v -> startActivity(new Intent(this, SettingsActivity.class)));
        findViewById(R.id.btnSignOut).setOnClickListener(v -> logout());

        // Setup RecyclerView
        rvTodayAppointments.setLayoutManager(new LinearLayoutManager(this));
        todayAppointments = new ArrayList<>();
        appointmentAdapter = new AppointmentAdapter(todayAppointments, this::onAppointmentClick);
        rvTodayAppointments.setAdapter(appointmentAdapter);

        // Display current date
        SimpleDateFormat dateFormat = new SimpleDateFormat("EEEE dd MMMM yyyy", Locale.FRENCH);
        tvCurrentDate.setText(dateFormat.format(new Date()));
    }

    private void loadSecretaryInfo() {
        AuthManager.User currentUser = authManager.getCurrentUser();
        if (currentUser != null) {
            tvSecretaryName.setText(currentUser.fullName);
        }
    }

    private void loadDashboardStats() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        // Count today's appointments
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        String today = dateFormat.format(new Date());

        Cursor appointmentsCursor = db.rawQuery(
                "SELECT COUNT(*) FROM appointments WHERE DATE(appointment_datetime) = ? AND status = 'scheduled'",
                new String[]{today}
        );

        if (appointmentsCursor.moveToFirst()) {
            int count = appointmentsCursor.getInt(0);
            tvTodayAppointmentsCount.setText(String.valueOf(count));
        }
        appointmentsCursor.close();

        // Count urgent messages (messages marked as urgent)
        int secretaryUserId = authManager.getUserId();
        Cursor urgentCursor = db.rawQuery(
                "SELECT COUNT(*) FROM messages WHERE receiver_id = ? AND is_urgent = 1",
                new String[]{String.valueOf(secretaryUserId)}
        );

        if (urgentCursor.moveToFirst()) {
            int count = urgentCursor.getInt(0);
            tvUrgentRequestsCount.setText(String.valueOf(count));
        }
        urgentCursor.close();
    }

    private void loadTodayAppointments() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        todayAppointments.clear();

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        String today = dateFormat.format(new Date());

        String query = "SELECT a.id, a.appointment_datetime, a.status, " +
                "p.full_name as patient_name, d.full_name as doctor_name, " +
                "doc.specialization " +
                "FROM appointments a " +
                "JOIN users p ON a.patient_id = p.id " +
                "JOIN users d ON a.doctor_id = d.id " +
                "LEFT JOIN doctors doc ON d.id = doc.user_id " +
                "WHERE DATE(a.appointment_datetime) = ? " +
                "AND a.status = 'scheduled' " +
                "ORDER BY a.appointment_datetime ASC";

        Cursor cursor = db.rawQuery(query, new String[]{today});

        while (cursor.moveToNext()) {
            Appointment appointment = new Appointment();
            appointment.setId(cursor.getInt(0));
            appointment.setDateTime(cursor.getString(1));
            appointment.setStatus(cursor.getString(2));
            appointment.setPatientName(cursor.getString(3));
            appointment.setDoctorName(cursor.getString(4));
            appointment.setSpecialization(cursor.getString(5));

            todayAppointments.add(appointment);
        }
        cursor.close();

        appointmentAdapter.notifyDataSetChanged();
    }

    private void setupClickListeners() {
        cardManageAppointments.setOnClickListener(v -> {
            if (authManager.hasPermission("secretary_manage_appointments")) {
                Intent intent = new Intent(this, ManageAppointmentsActivity.class);
                startActivity(intent);
            } else {
                Toast.makeText(this, "Permission manquante", Toast.LENGTH_SHORT).show();
            }
        });

        cardViewPatients.setOnClickListener(v -> {
            if (authManager.hasPermission("secretary_view_patient_list")) {
                Intent intent = new Intent(this, SecretaryPatientManagementActivity.class);
                startActivity(intent);
            } else {
                Toast.makeText(this, "Permission manquante", Toast.LENGTH_SHORT).show();
            }
        });

        cardDoctorSchedules.setOnClickListener(v -> {
            Intent intent = new Intent(this, DoctorSchedulesActivity.class);
            startActivity(intent);
        });

        cardUrgentRequests.setOnClickListener(v -> {
            Intent intent = new Intent(this, UrgentRequestsActivity.class);
            startActivity(intent);
        });
    }

    private void onAppointmentClick(Appointment appointment) {
        // Open appointment details for modification
        Intent intent = new Intent(this, EditAppointmentActivity.class);
        intent.putExtra("appointment_id", appointment.getId());
        startActivity(intent);
    }

    private void logout() {
        authManager.logout();
        redirectToLogin();
    }

    private void redirectToLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Revalidate access on resume
        if (!validateAccess()) {
            redirectToLogin();
            return;
        }

        // Refresh data when returning to dashboard
        loadDashboardStats();
        loadTodayAppointments();
    }

    // Inner Appointment Model Class
    public static class Appointment {
        private int id;
        private String dateTime;
        private String status;
        private String patientName;
        private String doctorName;
        private String specialization;

        public int getId() { return id; }
        public void setId(int id) { this.id = id; }
        public String getDateTime() { return dateTime; }
        public void setDateTime(String dateTime) { this.dateTime = dateTime; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public String getPatientName() { return patientName; }
        public void setPatientName(String patientName) { this.patientName = patientName; }
        public String getDoctorName() { return doctorName; }
        public void setDoctorName(String doctorName) { this.doctorName = doctorName; }
        public String getSpecialization() { return specialization; }
        public void setSpecialization(String specialization) { this.specialization = specialization; }
    }
}