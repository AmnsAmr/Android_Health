package M.health;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.text.InputType;
import android.widget.*;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.List;

public class ManageUsersActivity extends AppCompatActivity {

    private DatabaseHelper dbHelper;
    private ListView usersListView;
    private ArrayAdapter<String> adapter;
    private final List<User> users = new ArrayList<>();

    private TextView totalUsersText, doctorsCountText, patientsCountText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_users);

        dbHelper = new DatabaseHelper(this);

        usersListView = findViewById(R.id.usersListView);
        totalUsersText = findViewById(R.id.totalUsersText);
        doctorsCountText = findViewById(R.id.doctorsCountText);
        patientsCountText = findViewById(R.id.patientsCountText);

        LinearLayout addUserBtn = findViewById(R.id.addUserBtn);
        LinearLayout searchUserBtn = findViewById(R.id.searchUserBtn);

        addUserBtn.setOnClickListener(v -> showAddUserDialog());
        searchUserBtn.setOnClickListener(v -> showSearchDialog());

        usersListView.setOnItemClickListener((parent, view, position, id) ->
                showUserOptionsDialog(users.get(position)));

        loadUsers();
        loadStatistics();
    }

    // ====================== LOAD USERS & STATS ======================

    private void loadUsers() {
        users.clear();

        SQLiteDatabase db = dbHelper.getReadableDatabase();

        // join doctors for specialization display when role = doctor
        String sql = "SELECT u.id, u.full_name, u.email, u.role, " +
                "COALESCE(d.specialization, '') AS specialization " +
                "FROM users u " +
                "LEFT JOIN doctors d ON u.id = d.user_id " +
                "ORDER BY u.full_name";

        Cursor cursor = db.rawQuery(sql, null);

        List<String> userStrings = new ArrayList<>();
        while (cursor.moveToNext()) {
            int id = cursor.getInt(0);
            String fullName = cursor.getString(1);
            String email = cursor.getString(2);
            String role = cursor.getString(3);
            String specialization = cursor.getString(4);

            User user = new User(id, fullName, email, role, specialization);
            users.add(user);

            String roleDisplay = getRoleDisplay(role);
            String extra = "";
            if ("doctor".equals(role) && specialization != null && !specialization.trim().isEmpty()) {
                extra = " (" + specialization + ")";
            }

            String nameShort = fullName.length() > 18 ? fullName.substring(0, 18) + ".." : fullName;
            String userDisplay = String.format("%-20s | %-10s | %s", nameShort, roleDisplay, "Actions") + extra;
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

        Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM users", null);
        if (cursor.moveToFirst()) totalUsersText.setText(String.valueOf(cursor.getInt(0)));
        cursor.close();

        cursor = db.rawQuery("SELECT COUNT(*) FROM users WHERE role = 'doctor'", null);
        if (cursor.moveToFirst()) doctorsCountText.setText(String.valueOf(cursor.getInt(0)));
        cursor.close();

        cursor = db.rawQuery("SELECT COUNT(*) FROM users WHERE role = 'patient'", null);
        if (cursor.moveToFirst()) patientsCountText.setText(String.valueOf(cursor.getInt(0)));
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

    // ====================== SEARCH ======================

    private void showSearchDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Rechercher Utilisateur");

        EditText searchEdit = new EditText(this);
        searchEdit.setHint("Nom ou email");
        builder.setView(searchEdit);

        builder.setPositiveButton("Rechercher", (dialog, which) -> {
            String query = searchEdit.getText().toString().trim();
            if (!query.isEmpty()) searchUsers(query);
        });

        builder.setNegativeButton("Tout afficher", (dialog, which) -> loadUsers());
        builder.show();
    }

    private void searchUsers(String query) {
        users.clear();

        SQLiteDatabase db = dbHelper.getReadableDatabase();

        String sql = "SELECT u.id, u.full_name, u.email, u.role, " +
                "COALESCE(d.specialization,'') AS specialization " +
                "FROM users u " +
                "LEFT JOIN doctors d ON u.id = d.user_id " +
                "WHERE u.full_name LIKE ? OR u.email LIKE ? " +
                "ORDER BY u.full_name";

        Cursor cursor = db.rawQuery(sql, new String[]{"%" + query + "%", "%" + query + "%"});

        List<String> userStrings = new ArrayList<>();
        while (cursor.moveToNext()) {
            User user = new User(
                    cursor.getInt(0),
                    cursor.getString(1),
                    cursor.getString(2),
                    cursor.getString(3),
                    cursor.getString(4)
            );
            users.add(user);

            String roleDisplay = getRoleDisplay(user.role);
            String extra = "";
            if ("doctor".equals(user.role) && user.specialization != null && !user.specialization.trim().isEmpty()) {
                extra = " (" + user.specialization + ")";
            }

            String nameShort = user.fullName.length() > 18 ? user.fullName.substring(0, 18) + ".." : user.fullName;
            String userDisplay = String.format("%-20s | %-10s | %s", nameShort, roleDisplay, "Actions") + extra;
            userStrings.add(userDisplay);
        }
        cursor.close();

        adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, userStrings);
        usersListView.setAdapter(adapter);

        if (users.isEmpty()) Toast.makeText(this, "Aucun utilisateur trouvé", Toast.LENGTH_SHORT).show();
    }

    // ====================== ADD USER ======================

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
        emailEdit.setInputType(InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        layout.addView(emailEdit);

        EditText passwordEdit = new EditText(this);
        passwordEdit.setHint("Mot de passe");
        passwordEdit.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        layout.addView(passwordEdit);

        Spinner roleSpinner = new Spinner(this);
        ArrayAdapter<String> roleAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                new String[]{"patient", "doctor", "admin", "secretary"}
        );
        roleAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        roleSpinner.setAdapter(roleAdapter);
        layout.addView(roleSpinner);

        builder.setView(layout);

        builder.setPositiveButton("Suivant", (dialog, which) -> {
            String name = nameEdit.getText().toString().trim();
            String email = emailEdit.getText().toString().trim();
            String password = passwordEdit.getText().toString().trim();
            String role = roleSpinner.getSelectedItem().toString();

            if (name.isEmpty() || email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Veuillez remplir tous les champs", Toast.LENGTH_SHORT).show();
                return;
            }
            if (!isValidEmail(email)) {
                Toast.makeText(this, "Email invalide", Toast.LENGTH_SHORT).show();
                return;
            }

            if ("doctor".equals(role)) {
                showDoctorExtraDialogThenAdd(name, email, password, role);
            } else {
                addUser(name, email, password, role, null, null);
            }
        });

        builder.setNegativeButton("Annuler", null);
        builder.show();
    }

    private void showDoctorExtraDialogThenAdd(String name, String email, String password, String role) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Infos Médecin");

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 50, 50, 50);

        EditText specEdit = new EditText(this);
        specEdit.setHint("Spécialisation (ex: Cardiologue)");
        layout.addView(specEdit);

        EditText licenseEdit = new EditText(this);
        licenseEdit.setHint("N° Licence (optionnel)");
        layout.addView(licenseEdit);

        builder.setView(layout);

        builder.setPositiveButton("Ajouter", (d, w) -> {
            String specialization = specEdit.getText().toString().trim();
            String license = licenseEdit.getText().toString().trim();

            if (specialization.isEmpty()) {
                Toast.makeText(this, "Spécialisation obligatoire pour un médecin", Toast.LENGTH_SHORT).show();
                return;
            }

            addUser(name, email, password, role, specialization, license.isEmpty() ? null : license);
        });

        builder.setNegativeButton("Annuler", null);
        builder.show();
    }

    private boolean isValidEmail(String email) {
        return email.contains("@") && email.contains(".");
    }

    private void addUser(String name, String email, String password, String role,
                         String specialization, String licenseNumber) {

        SQLiteDatabase db = dbHelper.getWritableDatabase();

        // Email unique
        Cursor cursor = db.rawQuery("SELECT id FROM users WHERE email = ?", new String[]{email});
        if (cursor.moveToFirst()) {
            cursor.close();
            Toast.makeText(this, "Cet email existe déjà", Toast.LENGTH_SHORT).show();
            return;
        }
        cursor.close();

        db.beginTransaction();
        try {
            ContentValues values = new ContentValues();
            values.put("full_name", name);
            values.put("email", email);
            values.put("password_hash", password);
            values.put("role", role);

            long userId = db.insert("users", null, values);
            if (userId == -1) throw new RuntimeException("Insertion user échouée");

            if ("doctor".equals(role)) {
                ContentValues docVals = new ContentValues();
                docVals.put("user_id", userId);
                docVals.put("specialization", specialization);
                if (licenseNumber != null) docVals.put("license_number", licenseNumber);

                long docIns = db.insert("doctors", null, docVals);
                if (docIns == -1) throw new RuntimeException("Insertion doctor échouée");
            }

            db.setTransactionSuccessful();
            Toast.makeText(this, "Utilisateur ajouté avec succès", Toast.LENGTH_SHORT).show();
            loadUsers();
            loadStatistics();

        } catch (Exception e) {
            Toast.makeText(this, "Erreur ajout: " + e.getMessage(), Toast.LENGTH_LONG).show();
        } finally {
            db.endTransaction();
        }
    }

    // ====================== OPTIONS (DETAIL / EDIT / ROLE / DELETE) ======================

    private void showUserOptionsDialog(User user) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(user.fullName + " (" + getRoleDisplay(user.role) + ")");

        String[] options = {"Voir détails", "Modifier", "Changer rôle", "Supprimer"};
        builder.setItems(options, (dialog, which) -> {
            switch (which) {
                case 0: showUserDetails(user); break;
                case 1: showEditUserDialog(user); break;
                case 2: showChangeRoleDialog(user); break;
                case 3: confirmDeleteUser(user); break;
            }
        });

        builder.show();
    }

    private void showUserDetails(User user) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        StringBuilder details = new StringBuilder();

        details.append("Nom: ").append(user.fullName).append("\n");
        details.append("Email: ").append(user.email).append("\n");
        details.append("Rôle: ").append(getRoleDisplay(user.role)).append("\n");

        if ("doctor".equals(user.role)) {
            details.append("Spécialisation: ").append(user.specialization == null ? "" : user.specialization).append("\n");
        }

        new AlertDialog.Builder(this)
                .setTitle("Détails Utilisateur")
                .setMessage(details.toString())
                .setPositiveButton("Fermer", null)
                .show();
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
        emailEdit.setInputType(InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        layout.addView(emailEdit);

        builder.setView(layout);

        builder.setPositiveButton("Modifier", (dialog, which) -> {
            String name = nameEdit.getText().toString().trim();
            String email = emailEdit.getText().toString().trim();

            if (name.isEmpty() || email.isEmpty()) {
                Toast.makeText(this, "Veuillez remplir tous les champs", Toast.LENGTH_SHORT).show();
                return;
            }
            if (!isValidEmail(email)) {
                Toast.makeText(this, "Email invalide", Toast.LENGTH_SHORT).show();
                return;
            }
            updateUser(user.id, name, email);
        });

        builder.setNegativeButton("Annuler", null);
        builder.show();
    }

    private void updateUser(int id, String name, String email) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();

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
            Toast.makeText(this, "Utilisateur modifié", Toast.LENGTH_SHORT).show();
            loadUsers();
            loadStatistics();
        } else {
            Toast.makeText(this, "Erreur modification", Toast.LENGTH_SHORT).show();
        }
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

        for (int i = 0; i < roles.length; i++) {
            if (roles[i].equals(user.role)) {
                roleSpinner.setSelection(i);
                break;
            }
        }

        builder.setView(roleSpinner);

        builder.setPositiveButton("Suivant", (dialog, which) -> {
            String newRole = roleSpinner.getSelectedItem().toString();
            if (newRole.equals(user.role)) return;

            if ("doctor".equals(newRole)) {
                showDoctorInfoForRoleChange(user.id, newRole);
            } else {
                updateUserRole(user.id, newRole, null, null);
            }
        });

        builder.setNegativeButton("Annuler", null);
        builder.show();
    }

    private void showDoctorInfoForRoleChange(int userId, String newRole) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Infos Médecin");

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 50, 50, 50);

        EditText specEdit = new EditText(this);
        specEdit.setHint("Spécialisation (obligatoire)");
        layout.addView(specEdit);

        EditText licenseEdit = new EditText(this);
        licenseEdit.setHint("N° Licence (optionnel)");
        layout.addView(licenseEdit);

        builder.setView(layout);

        builder.setPositiveButton("Changer", (d, w) -> {
            String specialization = specEdit.getText().toString().trim();
            String license = licenseEdit.getText().toString().trim();

            if (specialization.isEmpty()) {
                Toast.makeText(this, "Spécialisation obligatoire", Toast.LENGTH_SHORT).show();
                return;
            }
            updateUserRole(userId, newRole, specialization, license.isEmpty() ? null : license);
        });

        builder.setNegativeButton("Annuler", null);
        builder.show();
    }

    private void updateUserRole(int userId, String newRole, String specialization, String licenseNumber) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        db.beginTransaction();
        try {
            // Update role in users
            ContentValues values = new ContentValues();
            values.put("role", newRole);

            int result = db.update("users", values, "id = ?", new String[]{String.valueOf(userId)});
            if (result <= 0) throw new RuntimeException("Update role échoué");

            // Maintain doctors table
            if ("doctor".equals(newRole)) {
                // insert or replace doctor row
                ContentValues docVals = new ContentValues();
                docVals.put("user_id", userId);
                docVals.put("specialization", specialization);
                if (licenseNumber != null) docVals.put("license_number", licenseNumber);

                // if exists -> update, else insert
                Cursor c = db.rawQuery("SELECT user_id FROM doctors WHERE user_id = ?", new String[]{String.valueOf(userId)});
                boolean exists = c.moveToFirst();
                c.close();

                if (exists) {
                    db.update("doctors", docVals, "user_id = ?", new String[]{String.valueOf(userId)});
                } else {
                    long ins = db.insert("doctors", null, docVals);
                    if (ins == -1) throw new RuntimeException("Insertion doctor échouée");
                }
            } else {
                // if leaving doctor role -> delete doctor profile
                db.delete("doctors", "user_id = ?", new String[]{String.valueOf(userId)});
            }

            db.setTransactionSuccessful();
            Toast.makeText(this, "Rôle modifié", Toast.LENGTH_SHORT).show();
            loadUsers();
            loadStatistics();

        } catch (Exception e) {
            Toast.makeText(this, "Erreur rôle: " + e.getMessage(), Toast.LENGTH_LONG).show();
        } finally {
            db.endTransaction();
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

        db.beginTransaction();
        try {
            // safety: delete doctor profile if exists
            db.delete("doctors", "user_id = ?", new String[]{String.valueOf(id)});
            db.delete("patients", "user_id = ?", new String[]{String.valueOf(id)});

            int result = db.delete("users", "id = ?", new String[]{String.valueOf(id)});
            if (result <= 0) throw new RuntimeException("Suppression échouée");

            db.setTransactionSuccessful();
            Toast.makeText(this, "Utilisateur supprimé", Toast.LENGTH_SHORT).show();
            loadUsers();
            loadStatistics();

        } catch (Exception e) {
            Toast.makeText(this, "Erreur suppression: " + e.getMessage(), Toast.LENGTH_LONG).show();
        } finally {
            db.endTransaction();
        }
    }

    // ✅ IMPORTANT: public static (sinon: Cannot access ...)
    public static class User {
        public int id;
        public String fullName;
        public String email;
        public String role;
        public String specialization;

        public User(int id, String fullName, String email, String role, String specialization) {
            this.id = id;
            this.fullName = fullName;
            this.email = email;
            this.role = role;
            this.specialization = specialization;
        }
    }
}
