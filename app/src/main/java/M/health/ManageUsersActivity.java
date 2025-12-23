package M.health;

import android.content.ContentValues;
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
    private ListView usersListView;
    private ArrayAdapter<String> adapter;
    private List<User> users;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_users);

        dbHelper = new DatabaseHelper(this);
        usersListView = findViewById(R.id.usersListView);
        users = new ArrayList<>();

        LinearLayout addUserBtn = findViewById(R.id.addUserBtn);
        addUserBtn.setOnClickListener(v -> showAddUserDialog());

        usersListView.setOnItemClickListener((parent, view, position, id) -> 
            showUserOptionsDialog(users.get(position)));

        loadUsers();
    }

    private void loadUsers() {
        users.clear();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT id, full_name, email, role FROM users", null);
        
        List<String> userStrings = new ArrayList<>();
        while (cursor.moveToNext()) {
            User user = new User(cursor.getInt(0), cursor.getString(1), 
                               cursor.getString(2), cursor.getString(3));
            users.add(user);
            userStrings.add(user.fullName + " (" + user.role + ")");
        }
        cursor.close();

        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, userStrings);
        usersListView.setAdapter(adapter);
    }

    private void showAddUserDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Ajouter Utilisateur");

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);

        EditText nameEdit = new EditText(this);
        nameEdit.setHint("Nom complet");
        layout.addView(nameEdit);

        EditText emailEdit = new EditText(this);
        emailEdit.setHint("Email");
        layout.addView(emailEdit);

        EditText passwordEdit = new EditText(this);
        passwordEdit.setHint("Mot de passe");
        layout.addView(passwordEdit);

        Spinner roleSpinner = new Spinner(this);
        ArrayAdapter<String> roleAdapter = new ArrayAdapter<>(this, 
            android.R.layout.simple_spinner_item, 
            new String[]{"patient", "doctor", "admin", "secretary"});
        roleSpinner.setAdapter(roleAdapter);
        layout.addView(roleSpinner);

        builder.setView(layout);
        builder.setPositiveButton("Ajouter", (dialog, which) -> {
            String name = nameEdit.getText().toString();
            String email = emailEdit.getText().toString();
            String password = passwordEdit.getText().toString();
            String role = roleSpinner.getSelectedItem().toString();

            if (!name.isEmpty() && !email.isEmpty() && !password.isEmpty()) {
                addUser(name, email, password, role);
            }
        });
        builder.setNegativeButton("Annuler", null);
        builder.show();
    }

    private void addUser(String name, String email, String password, String role) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("full_name", name);
        values.put("email", email);
        values.put("password_hash", password);
        values.put("role", role);

        long result = db.insert("users", null, values);
        if (result != -1) {
            Toast.makeText(this, "Utilisateur ajouté", Toast.LENGTH_SHORT).show();
            loadUsers();
        } else {
            Toast.makeText(this, "Erreur lors de l'ajout", Toast.LENGTH_SHORT).show();
        }
    }

    private void showUserOptionsDialog(User user) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(user.fullName);
        builder.setItems(new String[]{"Modifier", "Supprimer"}, (dialog, which) -> {
            if (which == 0) {
                showEditUserDialog(user);
            } else {
                deleteUser(user.id);
            }
        });
        builder.show();
    }

    private void showEditUserDialog(User user) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Modifier Utilisateur");

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);

        EditText nameEdit = new EditText(this);
        nameEdit.setText(user.fullName);
        layout.addView(nameEdit);

        EditText emailEdit = new EditText(this);
        emailEdit.setText(user.email);
        layout.addView(emailEdit);

        Spinner roleSpinner = new Spinner(this);
        ArrayAdapter<String> roleAdapter = new ArrayAdapter<>(this, 
            android.R.layout.simple_spinner_item, 
            new String[]{"patient", "doctor", "admin", "secretary"});
        roleSpinner.setAdapter(roleAdapter);
        roleSpinner.setSelection(roleAdapter.getPosition(user.role));
        layout.addView(roleSpinner);

        builder.setView(layout);
        builder.setPositiveButton("Modifier", (dialog, which) -> {
            String name = nameEdit.getText().toString();
            String email = emailEdit.getText().toString();
            String role = roleSpinner.getSelectedItem().toString();
            updateUser(user.id, name, email, role);
        });
        builder.setNegativeButton("Annuler", null);
        builder.show();
    }

    private void updateUser(int id, String name, String email, String role) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("full_name", name);
        values.put("email", email);
        values.put("role", role);

        int result = db.update("users", values, "id = ?", new String[]{String.valueOf(id)});
        if (result > 0) {
            Toast.makeText(this, "Utilisateur modifié", Toast.LENGTH_SHORT).show();
            loadUsers();
        }
    }

    private void deleteUser(int id) {
        new AlertDialog.Builder(this)
            .setTitle("Confirmer")
            .setMessage("Supprimer cet utilisateur?")
            .setPositiveButton("Oui", (dialog, which) -> {
                SQLiteDatabase db = dbHelper.getWritableDatabase();
                int result = db.delete("users", "id = ?", new String[]{String.valueOf(id)});
                if (result > 0) {
                    Toast.makeText(this, "Utilisateur supprimé", Toast.LENGTH_SHORT).show();
                    loadUsers();
                }
            })
            .setNegativeButton("Non", null)
            .show();
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