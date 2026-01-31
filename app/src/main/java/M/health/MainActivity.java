package M.health;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Force database creation
        DatabaseHelper dbHelper = new DatabaseHelper(this);
        dbHelper.getWritableDatabase();
        
        // Fix secretary permissions
        PermissionFixer.fixSecretaryPermissions(this);
        
        // Fix missing doctor profiles
        DoctorProfileFixer.fixMissingDoctorProfiles(this);
        
        // Redirect to login
        startActivity(new Intent(this, LoginActivity.class));
        finish();
    }
}