package M.health;

import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.widget.*;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import java.util.ArrayList;
import java.util.List;

public class ManageUsersActivity extends AppCompatActivity {
    private DatabaseHelper dbHelper;
    private AuthManager authManager;
    private ListView usersListView;
    private ArrayAdapter<String> adapter;
    private List<User> users;
    private TextView totalUsersText, doctorsCountText, patientsCountText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_users);

        dbHelper = new DatabaseHelper(this);
        authManager = AuthManager.getInstance(this);
        
        if (!authManager.isLoggedIn() || !authManager.validateSession()) {
            Toast.makeText(this, "Session expirée", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        if (!authManager.hasPermission("admin_manage_users")) {
            Toast.makeText(this, "Accès refusé", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        usersListView = findViewById(R.id.usersListView);
        totalUsersText = findViewById(R.id.totalUsersText);
        doctorsCountText = findViewById(R.id.doctorsCountText);
        patientsCountText = findViewById(R.id.patientsCountText);
        users = new ArrayList<>();

        LinearLayout addUserBtn = findViewById(R.id.addUserBtn);
        LinearLayout searchUserBtn = findViewById(R.id.searchUserBtn);
        
        addUserBtn.setOnClickListener(v -> showAddUserDialog());
        searchUserBtn.setOnClickListener(v -> showSearchDialog());

        usersListView.setOnItemClickListener((parent, view, position, id) -> 
            showUserOptionsDialog(users.get(position)));

        loadUsers();
        loadStatistics();
    }

    private void loadUsers() {
        users.clear();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT id, full_name, email, role FROM users ORDER BY full_name", null);
        
        List<String> userStrings = new ArrayList<>();
        while (cursor.moveToNext()) {
            User user = new User(cursor.getInt(0), cursor.getString(1), 
                               cursor.getString(2), cursor.getString(3));
            users.add(user);
            
            String roleDisplay = getRoleDisplay(user.role);
            String userDisplay = String.format("%-20s | %-10s", 
                user.fullName.length() > 18 ? user.fullName.substring(0, 18) + ".." : user.fullName,
                roleDisplay);
            userStrings.add(userDisplay);
        }
        cursor.close();

        adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, userStrings) {
            @Override
            public android.view.View getView(int position, android.view.View convertView, android.view.ViewGroup parent) {
                android.view.View view = super.getView(position, convertView, parent);
                TextView textView = view.findViewById(android.R.id.text1);
                textView.setTypeface(android.graphics.Typeface.MONOSPACE);
                textView.setTextSize(12);
                textView.setPadding(16, 12, 16, 12);
                return view;
            }
        };
        usersListView.setAdapter(adapter);
    }

    private void loadStatistics() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        
        // Total users
        Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM users", null);
        if (cursor.moveToFirst()) {
            totalUsersText.setText(String.valueOf(cursor.getInt(0)));
        }
        cursor.close();
        
        // Doctors count
        cursor = db.rawQuery("SELECT COUNT(*) FROM users WHERE role = 'doctor'", null);
        if (cursor.moveToFirst()) {
            doctorsCountText.setText(String.valueOf(cursor.getInt(0)));
        }
        cursor.close();
        
        // Patients count
        cursor = db.rawQuery("SELECT COUNT(*) FROM users WHERE role = 'patient'", null);
        if (cursor.moveToFirst()) {
            patientsCountText.setText(String.valueOf(cursor.getInt(0)));
        }
        cursor.close();
    }

    private String getRoleDisplay(String role) {
        switch (role) {
            case "admin": return "Admin";
            case "doctor": return "Médecin";
            case "patient": return "Patient";
            case "secretary": return "Secrétaire";
            default: return role;
        }
    }

    private void showSearchDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Rechercher Utilisateur");

        EditText searchEdit = new EditText(this);
        searchEdit.setHint("Nom ou email");
        builder.setView(searchEdit);

        builder.setPositiveButton("Rechercher", (dialog, which) -> {
            String query = searchEdit.getText().toString();
            if (!query.isEmpty()) {
                searchUsers(query);
            }
        });
        builder.setNegativeButton("Tout afficher", (dialog, which) -> loadUsers());
        builder.show();
    }

    private void searchUsers(String query) {
        users.clear();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery(
            "SELECT id, full_name, email, role FROM users WHERE full_name LIKE ? OR email LIKE ? ORDER BY full_name", 
            new String[]{"%" + query + "%", "%" + query + "%"});
        
        List<String> userStrings = new ArrayList<>();
        while (cursor.moveToNext()) {
            User user = new User(cursor.getInt(0), cursor.getString(1), 
                               cursor.getString(2), cursor.getString(3));
            users.add(user);
            
            String roleDisplay = getRoleDisplay(user.role);
            String userDisplay = String.format("%-20s | %-10s", 
                user.fullName.length() > 18 ? user.fullName.substring(0, 18) + ".." : user.fullName,
                roleDisplay);
            userStrings.add(userDisplay);
        }
        cursor.close();

        adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, userStrings) {
            @Override
            public android.view.View getView(int position, android.view.View convertView, android.view.ViewGroup parent) {
                android.view.View view = super.getView(position, convertView, parent);
                TextView textView = view.findViewById(android.R.id.text1);
                textView.setTypeface(android.graphics.Typeface.MONOSPACE);
                textView.setTextSize(12);
                textView.setPadding(16, 12, 16, 12);
                return view;
            }
        };
        usersListView.setAdapter(adapter);
        
        if (users.isEmpty()) {
            Toast.makeText(this, "Aucun utilisateur trouvé", Toast.LENGTH_SHORT).show();
        }
    }

    private void showAddUserDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Ajouter Utilisateur");

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 50, 50, 50);

        EditText nameEdit = new EditText(this);
        nameEdit.setHint("Nom complet");
        layout.addView(nameEdit);

        EditText emailEdit = new EditText(this);
        emailEdit.setHint("Email");
        emailEdit.setInputType(android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        layout.addView(emailEdit);

        EditText passwordEdit = new EditText(this);
        passwordEdit.setHint("Mot de passe");
        passwordEdit.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
        layout.addView(passwordEdit);

        Spinner roleSpinner = new Spinner(this);
        ArrayAdapter<String> roleAdapter = new ArrayAdapter<>(this, 
            android.R.layout.simple_spinner_item, 
            new String[]{"patient", "doctor", "admin", "secretary"});
        roleAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        roleSpinner.setAdapter(roleAdapter);
        layout.addView(roleSpinner);

        builder.setView(layout);
        builder.setPositiveButton("Ajouter", (dialog, which) -> {
            String name = nameEdit.getText().toString().trim();
            String email = emailEdit.getText().toString().trim();
            String password = passwordEdit.getText().toString().trim();
            String role = roleSpinner.getSelectedItem().toString();

            if (!name.isEmpty() && !email.isEmpty() && !password.isEmpty()) {
                if (isValidEmail(email)) {
                    addUser(name, email, password, role);
                } else {
                    Toast.makeText(this, "Email invalide", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this, "Veuillez remplir tous les champs", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("Annuler", null);
        builder.show();
    }

    private boolean isValidEmail(String email) {
        return email.contains("@") && email.contains(".");
    }

    private void addUser(String name, String email, String password, String role) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        
        // Check if email already exists
        Cursor cursor = db.rawQuery("SELECT id FROM users WHERE email = ?", new String[]{email});
        if (cursor.moveToFirst()) {
            cursor.close();
            Toast.makeText(this, "Cet email existe déjà", Toast.LENGTH_SHORT).show();
            return;
        }
        cursor.close();
        
        ContentValues values = new ContentValues();
        values.put("full_name", name);
        values.put("email", email);
        values.put("password_hash", password);
        values.put("role", role);
        values.put("role_id", getRoleId(role));
        values.put("is_active", 1);

        long result = db.insert("users", null, values);
        if (result != -1) {
            Toast.makeText(this, "Utilisateur ajouté avec succès", Toast.LENGTH_SHORT).show();
            loadUsers();
            loadStatistics();
        } else {
            Toast.makeText(this, "Erreur lors de l'ajout", Toast.LENGTH_SHORT).show();
        }
    }

    private void showUserOptionsDialog(User user) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(user.fullName + " (" + getRoleDisplay(user.role) + ")");
        
        String[] options = {"Voir détails", "Modifier", "Changer rôle", "Supprimer"};
        builder.setItems(options, (dialog, which) -> {
            switch (which) {
                case 0:
                    showUserDetails(user);
                    break;
                case 1:
                    showEditUserDialog(user);
                    break;
                case 2:
                    showChangeRoleDialog(user);
                    break;
                case 3:
                    confirmDeleteUser(user);
                    break;
            }
        });
        builder.show();
    }

    private void showUserDetails(User user) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        StringBuilder details = new StringBuilder();
        
        details.append("Nom: ").append(user.fullName).append("\n");
        details.append("Email: ").append(user.email).append("\n");
        details.append("Rôle: ").append(getRoleDisplay(user.role)).append("\n\n");
        
        if (user.role.equals("patient")) {
            Cursor cursor = db.rawQuery(
                "SELECT COUNT(*) FROM appointments WHERE patient_id = ?", 
                new String[]{String.valueOf(user.id)});
            if (cursor.moveToFirst()) {
                details.append("Rendez-vous: ").append(cursor.getInt(0)).append("\n");
            }
            cursor.close();
        } else if (user.role.equals("doctor")) {
            Cursor cursor = db.rawQuery(
                "SELECT specialization FROM doctors WHERE user_id = ?", 
                new String[]{String.valueOf(user.id)});
            if (cursor.moveToFirst()) {
                details.append("Spécialisation: ").append(cursor.getString(0)).append("\n");
            }
            cursor.close();
            
            cursor = db.rawQuery(
                "SELECT COUNT(*) FROM appointments WHERE doctor_id = ?", 
                new String[]{String.valueOf(user.id)});
            if (cursor.moveToFirst()) {
                details.append("Consultations: ").append(cursor.getInt(0)).append("\n");
            }
            cursor.close();
        }

        new AlertDialog.Builder(this)
            .setTitle("Détails Utilisateur")
            .setMessage(details.toString())
            .setPositiveButton("Fermer", null)
            .show();
    }

    private void showChangeRoleDialog(User user) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Changer le rôle de " + user.fullName);

        Spinner roleSpinner = new Spinner(this);
        String[] roles = {"patient", "doctor", "admin", "secretary"};
        ArrayAdapter<String> roleAdapter = new ArrayAdapter<>(this, 
            android.R.layout.simple_spinner_item, roles);
        roleAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        roleSpinner.setAdapter(roleAdapter);
        
        // Set current role as selected
        for (int i = 0; i < roles.length; i++) {
            if (roles[i].equals(user.role)) {
                roleSpinner.setSelection(i);
                break;
            }
        }
        
        builder.setView(roleSpinner);
        builder.setPositiveButton("Changer", (dialog, which) -> {
            String newRole = roleSpinner.getSelectedItem().toString();
            if (!newRole.equals(user.role)) {
                updateUserRole(user.id, newRole);
            }
        });
        builder.setNegativeButton("Annuler", null);
        builder.show();
    }

    private void updateUserRole(int userId, String newRole) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("role", newRole);
        
        // Update role_id based on role name
        int roleId = getRoleId(newRole);
        values.put("role_id", roleId);

        int result = db.update("users", values, "id = ?", new String[]{String.valueOf(userId)});
        if (result > 0) {
            Toast.makeText(this, "Rôle modifié avec succès", Toast.LENGTH_SHORT).show();
            // Refresh permissions for all users if current user role changed
            authManager.refreshPermissions();
            loadUsers();
            loadStatistics();
        } else {
            Toast.makeText(this, "Erreur lors de la modification", Toast.LENGTH_SHORT).show();
        }
    }
    
    private int getRoleId(String roleName) {
        switch (roleName) {
            case "admin": return 1;
            case "doctor": return 2;
            case "patient": return 3;
            case "secretary": return 4;
            default: return 3; // default to patient
        }
    }

    private void showEditUserDialog(User user) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Modifier Utilisateur");

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 50, 50, 50);

        EditText nameEdit = new EditText(this);
        nameEdit.setText(user.fullName);
        nameEdit.setHint("Nom complet");
        layout.addView(nameEdit);

        EditText emailEdit = new EditText(this);
        emailEdit.setText(user.email);
        emailEdit.setHint("Email");
        emailEdit.setInputType(android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        layout.addView(emailEdit);

        builder.setView(layout);
        builder.setPositiveButton("Modifier", (dialog, which) -> {
            String name = nameEdit.getText().toString().trim();
            String email = emailEdit.getText().toString().trim();
            
            if (!name.isEmpty() && !email.isEmpty()) {
                if (isValidEmail(email)) {
                    updateUser(user.id, name, email);
                } else {
                    Toast.makeText(this, "Email invalide", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this, "Veuillez remplir tous les champs", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("Annuler", null);
        builder.show();
    }

    private void updateUser(int id, String name, String email) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        
        // Check if email already exists for another user
        Cursor cursor = db.rawQuery("SELECT id FROM users WHERE email = ? AND id != ?", 
            new String[]{email, String.valueOf(id)});
        if (cursor.moveToFirst()) {
            cursor.close();
            Toast.makeText(this, "Cet email est déjà utilisé", Toast.LENGTH_SHORT).show();
            return;
        }
        cursor.close();
        
        ContentValues values = new ContentValues();
        values.put("full_name", name);
        values.put("email", email);

        int result = db.update("users", values, "id = ?", new String[]{String.valueOf(id)});
        if (result > 0) {
            Toast.makeText(this, "Utilisateur modifié avec succès", Toast.LENGTH_SHORT).show();
            loadUsers();
        } else {
            Toast.makeText(this, "Erreur lors de la modification", Toast.LENGTH_SHORT).show();
        }
    }

    private void confirmDeleteUser(User user) {
        new AlertDialog.Builder(this)
            .setTitle("Confirmer la suppression")
            .setMessage("Supprimer " + user.fullName + " ?\n\nCette action est irréversible.")
            .setPositiveButton("Supprimer", (dialog, which) -> deleteUser(user.id))
            .setNegativeButton("Annuler", null)
            .show();
    }

    private void deleteUser(int id) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        int result = db.delete("users", "id = ?", new String[]{String.valueOf(id)});
        if (result > 0) {
            Toast.makeText(this, "Utilisateur supprimé avec succès", Toast.LENGTH_SHORT).show();
            loadUsers();
            loadStatistics();
        } else {
            Toast.makeText(this, "Erreur lors de la suppression", Toast.LENGTH_SHORT).show();
        }
    }

    private static class User {
        int id;
        String fullName, email, role;

        User(int id, String fullName, String email, String role) {
            this.id = id;
            this.fullName = fullName;
            this.email = email;
            this.role = role;
        }
    }
}