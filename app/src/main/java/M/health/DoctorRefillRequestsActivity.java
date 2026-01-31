package M.health;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class DoctorRefillRequestsActivity extends AppCompatActivity {
    private DatabaseHelper dbHelper;
    private AuthManager authManager;
    private LinearLayout requestsContainer;
    private int doctorId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_doctor_refill_requests);

        dbHelper = new DatabaseHelper(this);
        authManager = AuthManager.getInstance(this);
        
        if (!authManager.isLoggedIn()) {
            finish();
            return;
        }

        doctorId = authManager.getCurrentUser().id;
        
        View headerView = findViewById(R.id.headerLayout);
        UIHelper.setupHeaderWithSignOut(this, headerView, "Demandes Renouvellement", authManager);
        
        requestsContainer = findViewById(R.id.requestsContainer);
        loadRefillRequests();
    }

    private void loadRefillRequests() {
        requestsContainer.removeAllViews();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        
        Cursor cursor = db.rawQuery(
            "SELECT rr.id, p.medication, p.dosage, u.full_name, rr.requested_at, rr.status " +
            "FROM prescription_refill_requests rr " +
            "JOIN prescriptions p ON rr.prescription_id = p.id " +
            "JOIN users u ON p.patient_id = u.id " +
            "WHERE p.doctor_id = ? AND rr.status = 'pending' " +
            "ORDER BY rr.requested_at DESC", 
            new String[]{String.valueOf(doctorId)});

        while (cursor.moveToNext()) {
            addRefillRequestCard(
                cursor.getInt(0),
                cursor.getString(1),
                cursor.getString(2),
                cursor.getString(3),
                cursor.getString(4)
            );
        }
        cursor.close();
    }

    private void addRefillRequestCard(int requestId, String medication, String dosage, 
                                    String patientName, String requestDate) {
        View cardView = getLayoutInflater().inflate(R.layout.item_refill_request_card, null);
        
        TextView tvMedication = cardView.findViewById(R.id.tvMedication);
        TextView tvDosage = cardView.findViewById(R.id.tvDosage);
        TextView tvPatient = cardView.findViewById(R.id.tvPatient);
        TextView tvDate = cardView.findViewById(R.id.tvDate);
        
        tvMedication.setText(medication);
        tvDosage.setText(dosage);
        tvPatient.setText("Patient: " + patientName);
        tvDate.setText("Demandé le: " + requestDate);

        cardView.findViewById(R.id.btnApprove).setOnClickListener(v -> 
            updateRequestStatus(requestId, "approved", "Demande approuvée"));
        
        cardView.findViewById(R.id.btnReject).setOnClickListener(v -> 
            updateRequestStatus(requestId, "rejected", "Demande rejetée"));
        
        requestsContainer.addView(cardView);
    }

    private void updateRequestStatus(int requestId, String status, String message) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("status", status);
        
        int result = db.update("prescription_refill_requests", values, "id = ?", 
            new String[]{String.valueOf(requestId)});
        
        if (result > 0) {
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
            loadRefillRequests();
        } else {
            Toast.makeText(this, "Erreur lors de la mise à jour", Toast.LENGTH_SHORT).show();
        }
    }
}