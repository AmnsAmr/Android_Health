package M.health;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class TestResultsActivity extends AppCompatActivity {
    private DatabaseHelper dbHelper;
    private AuthManager authManager;
    private LinearLayout resultsContainer;
    private int doctorId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test_results);

        dbHelper = new DatabaseHelper(this);
        authManager = AuthManager.getInstance(this);
        
        if (!authManager.isLoggedIn()) {
            finish();
            return;
        }

        doctorId = authManager.getCurrentUser().id;
        
        View headerView = findViewById(R.id.headerLayout);
        UIHelper.setupHeaderWithSignOut(this, headerView, "Résultats Tests", authManager);
        
        resultsContainer = findViewById(R.id.resultsContainer);
        loadTestResults();
    }

    private void loadTestResults() {
        resultsContainer.removeAllViews();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        
        Cursor cursor = db.rawQuery(
            "SELECT t.id, t.test_name, t.result, t.test_date, u.full_name " +
            "FROM test_results t " +
            "JOIN users u ON t.patient_id = u.id " +
            "WHERE t.doctor_id = ? ORDER BY t.test_date DESC", 
            new String[]{String.valueOf(doctorId)});

        while (cursor.moveToNext()) {
            addTestResultCard(
                cursor.getInt(0),
                cursor.getString(1),
                cursor.getString(2),
                cursor.getString(3),
                cursor.getString(4)
            );
        }
        cursor.close();
    }

    private void addTestResultCard(int testId, String testName, String result, 
                                 String testDate, String patientName) {
        View cardView = getLayoutInflater().inflate(R.layout.item_test_result_card, null);
        
        TextView tvTestName = cardView.findViewById(R.id.tvTestName);
        TextView tvResult = cardView.findViewById(R.id.tvResult);
        TextView tvDate = cardView.findViewById(R.id.tvDate);
        TextView tvPatient = cardView.findViewById(R.id.tvPatient);
        TextView tvComments = cardView.findViewById(R.id.tvComments);
        EditText etNewComment = cardView.findViewById(R.id.etNewComment);
        
        tvTestName.setText(testName);
        tvResult.setText("Résultat: " + result);
        tvDate.setText("Date: " + testDate);
        tvPatient.setText("Patient: " + patientName);
        
        loadComments(testId, tvComments);
        
        cardView.findViewById(R.id.btnAddComment).setOnClickListener(v -> {
            String comment = etNewComment.getText().toString().trim();
            if (!comment.isEmpty()) {
                addComment(testId, comment, etNewComment, tvComments);
            } else {
                Toast.makeText(this, "Veuillez saisir un commentaire", Toast.LENGTH_SHORT).show();
            }
        });
        
        resultsContainer.addView(cardView);
    }

    private void loadComments(int testId, TextView tvComments) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery(
            "SELECT c.comment, c.created_at, u.full_name " +
            "FROM test_result_comments c " +
            "JOIN users u ON c.doctor_id = u.id " +
            "WHERE c.test_result_id = ? ORDER BY c.created_at DESC", 
            new String[]{String.valueOf(testId)});

        StringBuilder comments = new StringBuilder();
        while (cursor.moveToNext()) {
            comments.append("Dr. ").append(cursor.getString(2))
                   .append(" (").append(cursor.getString(1)).append("):\n")
                   .append(cursor.getString(0)).append("\n\n");
        }
        cursor.close();
        
        if (comments.length() > 0) {
            tvComments.setText(comments.toString());
            tvComments.setVisibility(View.VISIBLE);
        } else {
            tvComments.setText("Aucun commentaire");
            tvComments.setVisibility(View.VISIBLE);
        }
    }

    private void addComment(int testId, String comment, EditText etNewComment, TextView tvComments) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("test_result_id", testId);
        values.put("doctor_id", doctorId);
        values.put("comment", comment);
        
        long result = db.insert("test_result_comments", null, values);
        
        if (result != -1) {
            etNewComment.setText("");
            loadComments(testId, tvComments);
            
            // Notify patient
            notifyPatient(testId, comment);
            
            Toast.makeText(this, "Commentaire ajouté", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Erreur lors de l'ajout", Toast.LENGTH_SHORT).show();
        }
    }

    private void notifyPatient(int testId, String comment) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        
        // Get patient ID from test result
        Cursor cursor = db.rawQuery(
            "SELECT patient_id, test_name FROM test_results WHERE id = ?", 
            new String[]{String.valueOf(testId)});
        
        if (cursor.moveToFirst()) {
            int patientId = cursor.getInt(0);
            String testName = cursor.getString(1);
            
            ContentValues values = new ContentValues();
            values.put("user_id", patientId);
            values.put("content", "Nouveau commentaire sur votre test: " + testName);
            values.put("is_read", 0);
            
            db.insert("notifications", null, values);
        }
        cursor.close();
    }
}