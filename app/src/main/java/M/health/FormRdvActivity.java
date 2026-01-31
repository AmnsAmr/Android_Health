package M.health;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import java.util.ArrayList;
import java.util.List;

public class FormRdvActivity extends AppCompatActivity {
    private DatabaseHelper dbHelper;
    private AuthManager authManager;
    private EditText etDate, etHeure, etNotes;
    private Spinner spinnerPatient, spinnerMedecin;
    private Button btnEnregistrer, btnRetour;
    private List<Integer> patientIds, doctorIds;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_form_rdv);

        dbHelper = new DatabaseHelper(this);
        authManager = AuthManager.getInstance(this);
        
        if (!authManager.hasPermission("secretary_manage_appointments")) {
            Toast.makeText(this, "Accès refusé", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initViews();
        loadPatients();
        loadDoctors();
        setupListeners();
    }

    private void initViews() {
        etDate = findViewById(R.id.et_date_rdv);
        etHeure = findViewById(R.id.et_heure_rdv);
        etNotes = findViewById(R.id.et_notes_rdv);
        spinnerPatient = findViewById(R.id.spinner_patient);
        spinnerMedecin = findViewById(R.id.spinner_medecin);
        btnEnregistrer = findViewById(R.id.btn_enregistrer_rdv);
        btnRetour = findViewById(R.id.btn_retour_menu);
    }

    private void loadPatients() {
        patientIds = new ArrayList<>();
        List<String> patientNames = new ArrayList<>();
        patientNames.add("Choisir un patient...");
        patientIds.add(-1);

        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery(
            "SELECT u.id, u.full_name FROM users u WHERE u.role = 'patient' AND u.is_active = 1", null);

        while (cursor.moveToNext()) {
            patientIds.add(cursor.getInt(0));
            patientNames.add(cursor.getString(1));
        }
        cursor.close();

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, patientNames);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerPatient.setAdapter(adapter);
    }

    private void loadDoctors() {
        doctorIds = new ArrayList<>();
        List<String> doctorNames = new ArrayList<>();
        doctorNames.add("Choisir un médecin...");
        doctorIds.add(-1);

        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery(
            "SELECT u.id, u.full_name, COALESCE(d.specialization, 'Généraliste') as specialization " +
            "FROM users u LEFT JOIN doctors d ON u.id = d.user_id " +
            "WHERE u.role = 'doctor' AND u.is_active = 1", null);

        while (cursor.moveToNext()) {
            doctorIds.add(cursor.getInt(0));
            doctorNames.add("Dr. " + cursor.getString(1) + " (" + cursor.getString(2) + ")");
        }
        cursor.close();

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, doctorNames);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerMedecin.setAdapter(adapter);
    }

    private void setupListeners() {
        btnEnregistrer.setOnClickListener(v -> enregistrerRendezVous());
        btnRetour.setOnClickListener(v -> finish());
    }

    private void enregistrerRendezVous() {
        String date = etDate.getText().toString().trim();
        String heure = etHeure.getText().toString().trim();
        String notes = etNotes.getText().toString().trim();
        
        if (date.isEmpty() || heure.isEmpty() || 
            spinnerPatient.getSelectedItemPosition() == 0 || 
            spinnerMedecin.getSelectedItemPosition() == 0) {
            Toast.makeText(this, "Veuillez remplir tous les champs obligatoires", Toast.LENGTH_SHORT).show();
            return;
        }

        int patientId = patientIds.get(spinnerPatient.getSelectedItemPosition());
        int doctorId = doctorIds.get(spinnerMedecin.getSelectedItemPosition());
        String datetime = date + " " + heure;

        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("patient_id", patientId);
        values.put("doctor_id", doctorId);
        values.put("appointment_datetime", datetime);
        values.put("status", "scheduled");
        values.put("notes", notes);
        values.put("created_by", "secretary");

        long result = db.insert("appointments", null, values);
        
        if (result != -1) {
            Toast.makeText(this, "Rendez-vous créé avec succès", Toast.LENGTH_SHORT).show();
            finish();
        } else {
            Toast.makeText(this, "Erreur lors de la création", Toast.LENGTH_SHORT).show();
        }
    }
}