package M.health;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class PatientDashboardActivity extends AppCompatActivity {

    private DatabaseHelper dbHelper;
    private AuthManager authManager;

    // UI Components
    private TextView tvNextApptDoctor, tvNextApptSpecialty, tvNextApptDate;
    private LinearLayout cardNextAppointment;
    private LinearLayout cardRdv;
    private LinearLayout cardDossier;
    private LinearLayout cardMeds;
    private LinearLayout cardMessages;
    private Button btnEditRdv;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_patient_dashboard);

        dbHelper = new DatabaseHelper(this);
        authManager = AuthManager.getInstance(this);

        if (!authManager.isLoggedIn() || !authManager.validateSession()) {
            Toast.makeText(this, "Session expirée", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        AuthManager.User currentUser = authManager.getCurrentUser();
        if (!"patient".equals(currentUser.role)) {
            Toast.makeText(this, "Accès refusé", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Setup reusable user profile header
        View userProfileHeader = findViewById(R.id.userProfileHeader);
        UIHelper.setupUserProfileHeader(this, userProfileHeader, authManager);

        // Bind Views
        initializeViews();

        // Load Data
        loadNextAppointment(currentUser.id);

        // Setup Navigation
        setupNavigation();
    }

    private void initializeViews() {
        tvNextApptDoctor = findViewById(R.id.tvNextApptDoctor);
        tvNextApptSpecialty = findViewById(R.id.tvNextApptSpecialty);
        tvNextApptDate = findViewById(R.id.tvNextApptDate);
        cardNextAppointment = findViewById(R.id.cardRdvDoctor);
        btnEditRdv = findViewById(R.id.btnEditRdv);

        // Cartes de navigation
        cardRdv = findViewById(R.id.cardRdv);
        cardDossier = findViewById(R.id.cardDossier);
        cardMeds = findViewById(R.id.cardMeds);
        cardMessages = findViewById(R.id.cardMessages);
    }

    private void setupNavigation() {
        // Navigation vers Rendez-vous
        if (cardRdv != null) {
            cardRdv.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (authManager.hasPermission("patient_book_appointments")) {
                        Intent intent = new Intent(PatientDashboardActivity.this, book_appointment.class);
                        startActivity(intent);
                    } else {
                        Toast.makeText(PatientDashboardActivity.this, "Accès refusé", Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }

        // Navigation vers Dossier Médical
        if (cardDossier != null) {
            cardDossier.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (authManager.hasPermission("patient_view_own_records")) {
                        Intent intent = new Intent(PatientDashboardActivity.this, page_dossier_medical.class);
                        startActivity(intent);
                    } else {
                        Toast.makeText(PatientDashboardActivity.this, "Accès refusé", Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }

        // Navigation vers Médicaments
        if (cardMeds != null) {
            cardMeds.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (authManager.hasPermission("patient_view_own_records")) {
                        Intent intent = new Intent(PatientDashboardActivity.this, page_medicament.class);
                        startActivity(intent);
                    } else {
                        Toast.makeText(PatientDashboardActivity.this, "Accès refusé", Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }

        // Navigation vers Messages
        if (cardMessages != null) {
            cardMessages.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (authManager.hasPermission("patient_message_doctor")) {
                        Intent intent = new Intent(PatientDashboardActivity.this, page_message.class);
                        startActivity(intent);
                    } else {
                        Toast.makeText(PatientDashboardActivity.this, "Accès refusé", Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }

        // Bouton Modifier rendez-vous
        if (btnEditRdv != null) {
            btnEditRdv.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (authManager.hasPermission("patient_book_appointments")) {
                        Intent intent = new Intent(PatientDashboardActivity.this, PatientAppointmentsActivity.class);
                        startActivity(intent);
                    } else {
                        Toast.makeText(PatientDashboardActivity.this, "Accès refusé", Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }
    }



    @SuppressLint("SetTextI18n")
    private void loadNextAppointment(int patientId) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = null;

        try {
            String query = "SELECT u.full_name, d.specialization, a.appointment_datetime " +
                    "FROM appointments a " +
                    "JOIN users u ON a.doctor_id = u.id " +
                    "JOIN doctors d ON a.doctor_id = d.user_id " +
                    "WHERE a.patient_id = ? AND a.status = 'scheduled' " +
                    "ORDER BY a.appointment_datetime ASC LIMIT 1";

            cursor = db.rawQuery(query, new String[]{String.valueOf(patientId)});

            if (cursor.moveToFirst()) {
                String doctorName = cursor.getString(0);
                String specialization = cursor.getString(1);
                String date = cursor.getString(2);

                tvNextApptDoctor.setText("Dr. " + doctorName);
                tvNextApptSpecialty.setText(specialization);
                tvNextApptDate.setText(date);

                cardNextAppointment.setVisibility(View.VISIBLE);
            } else {
                tvNextApptDoctor.setText("Aucun rendez-vous");
                tvNextApptSpecialty.setText("");
                tvNextApptDate.setText("");
                if (btnEditRdv != null) {
                    btnEditRdv.setVisibility(View.GONE);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Erreur chargement RDV", Toast.LENGTH_SHORT).show();
        } finally {
            if (cursor != null) cursor.close();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (authManager.isLoggedIn() && authManager.validateSession()) {
            AuthManager.User currentUser = authManager.getCurrentUser();
            if (currentUser != null) {
                loadNextAppointment(currentUser.id);
            }
        } else {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        }
    }
}