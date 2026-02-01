# ANNEXE TECHNIQUE - EXEMPLES DE CODE ET REQUÊTES

## TABLE DES MATIÈRES
1. [Exemples de Code par Fonctionnalité](#1-exemples-de-code-par-fonctionnalité)
2. [Requêtes SQL Complexes](#2-requêtes-sql-complexes)
3. [Patterns d'Implémentation](#3-patterns-dimplémentation)
4. [Gestion des Erreurs](#4-gestion-des-erreurs)
5. [Optimisations Techniques](#5-optimisations-techniques)

---

## 1. EXEMPLES DE CODE PAR FONCTIONNALITÉ

### 1.1 Authentification et Gestion des Sessions

#### AuthManager - Connexion Utilisateur
```java
public User login(String email, String password) {
    SQLiteDatabase db = dbHelper.getReadableDatabase();
    Cursor cursor = db.rawQuery(
        "SELECT u.id, u.full_name, u.email, u.role, u.role_id, u.is_active, r.name as role_name " +
        "FROM users u LEFT JOIN roles r ON u.role_id = r.id " +
        "WHERE u.email = ? AND u.password_hash = ? AND u.is_active = 1", 
        new String[]{email, password});

    if (cursor.moveToFirst()) {
        currentUser = new User(
            cursor.getInt(0),      // id
            cursor.getString(1),   // full_name
            cursor.getString(2),   // email
            cursor.getString(3),   // role
            cursor.getInt(4),      // role_id
            cursor.getInt(5) == 1  // is_active
        );
        cursor.close();
        
        updateLastLogin(currentUser.id);
        loadUserPermissions();
        return currentUser;
    }
    cursor.close();
    return null;
}
```

#### Validation des Permissions
```java
private void loadUserPermissions() {
    if (currentUser == null) return;
    
    userPermissions.clear();
    SQLiteDatabase db = dbHelper.getReadableDatabase();
    Cursor cursor = db.rawQuery(
        "SELECT permission_key FROM permissions WHERE role = ? AND role_required = 1", 
        new String[]{currentUser.role});
    
    while (cursor.moveToNext()) {
        userPermissions.add(cursor.getString(0));
    }
    cursor.close();
}

public boolean hasPermission(String permissionKey) {
    return currentUser != null && 
           currentUser.isActive && 
           userPermissions.contains(permissionKey);
}
```

### 1.2 Gestion des Rendez-vous

#### Création de Rendez-vous (Secretary)
```java
// FormRdvActivity.java - Méthode complète de création
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
```

#### Chargement des Médecins avec Spécialisation
```java
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
```

### 1.3 Gestion des Prescriptions

#### CRUD Complet des Prescriptions (Doctor)
```java
// DoctorPrescriptionsActivity.java - Ajout de prescription
private void addPrescription(int patientId, String medication, String dosage, String instructions) {
    SQLiteDatabase db = dbHelper.getWritableDatabase();
    ContentValues values = new ContentValues();
    values.put("patient_id", patientId);
    values.put("doctor_id", doctorId);
    values.put("medication", medication);
    values.put("dosage", dosage);
    if (!instructions.isEmpty()) {
        values.put("instructions", instructions);
    }

    long result = db.insert("prescriptions", null, values);
    if (result != -1) {
        Toast.makeText(this, "Prescription ajoutée avec succès", Toast.LENGTH_SHORT).show();
        loadPrescriptions();
        loadStatistics();
    } else {
        Toast.makeText(this, "Erreur lors de l'ajout", Toast.LENGTH_SHORT).show();
    }
}

// Modification de prescription
private void updatePrescription(int prescriptionId, String medication, String dosage, String instructions) {
    SQLiteDatabase db = dbHelper.getWritableDatabase();
    ContentValues values = new ContentValues();
    values.put("medication", medication);
    values.put("dosage", dosage);
    values.put("instructions", instructions);

    int result = db.update("prescriptions", values, "id = ?", new String[]{String.valueOf(prescriptionId)});
    if (result > 0) {
        Toast.makeText(this, "Prescription modifiée avec succès", Toast.LENGTH_SHORT).show();
        loadPrescriptions();
    } else {
        Toast.makeText(this, "Erreur lors de la modification", Toast.LENGTH_SHORT).show();
    }
}
```

#### Gestion des Demandes de Renouvellement
```java
// page_medicament.java - Demande de renouvellement côté patient
private void renouvellerOrdonnance(String medicamentNom) {
    SQLiteDatabase db = dbHelper.getWritableDatabase();
    AuthManager.User currentUser = authManager.getCurrentUser();
    
    try {
        // Recherche de la prescription
        Cursor cursor = db.rawQuery(
            "SELECT id FROM prescriptions WHERE patient_id = ? AND medication = ? ORDER BY created_at DESC LIMIT 1",
            new String[]{String.valueOf(currentUser.id), medicamentNom});
        
        if (cursor.moveToFirst()) {
            int prescriptionId = cursor.getInt(0);
            cursor.close();
            
            // Vérification d'une demande existante
            Cursor existingRequest = db.rawQuery(
                "SELECT id FROM prescription_refill_requests WHERE prescription_id = ? AND status = 'pending'",
                new String[]{String.valueOf(prescriptionId)});
            
            if (existingRequest.moveToFirst()) {
                existingRequest.close();
                Toast.makeText(this, "Une demande de renouvellement est déjà en cours", Toast.LENGTH_SHORT).show();
                return;
            }
            existingRequest.close();
            
            // Création de la nouvelle demande
            ContentValues values = new ContentValues();
            values.put("prescription_id", prescriptionId);
            values.put("status", "pending");
            
            long result = db.insert("prescription_refill_requests", null, values);
            
            if (result != -1) {
                Toast.makeText(this, "✓ Demande de renouvellement envoyée pour " + medicamentNom, Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(this, "Erreur lors de l'envoi de la demande", Toast.LENGTH_SHORT).show();
            }
        } else {
            cursor.close();
            Toast.makeText(this, "Prescription non trouvée", Toast.LENGTH_SHORT).show();
        }
        
    } catch (Exception e) {
        e.printStackTrace();
        Toast.makeText(this, "Erreur: " + e.getMessage(), Toast.LENGTH_SHORT).show();
    }
}
```

### 1.4 Gestion des Dossiers Médicaux

#### Chargement Dynamique des Informations Patient
```java
// page_dossier_medical.java - Chargement complet du profil
private void loadPatientData() {
    SQLiteDatabase db = dbHelper.getReadableDatabase();
    Cursor cursor = null;

    try {
        // Chargement des informations de base
        cursor = db.rawQuery(
            "SELECT u.full_name, u.email, p.date_of_birth, p.blood_type, p.emergency_contact " +
            "FROM users u LEFT JOIN patients p ON u.id = p.user_id " +
            "WHERE u.id = ?",
            new String[]{String.valueOf(patientId)}
        );

        if (cursor.moveToFirst()) {
            String name = cursor.getString(0);
            String birthDate = cursor.getString(2);
            String bloodType = cursor.getString(3);
            
            tvPatientName.setText(name);
            tvBloodType.setText(bloodType != null ? bloodType : "Non renseigné");
            
            // Calcul de l'âge
            if (birthDate != null && !birthDate.isEmpty()) {
                try {
                    String[] dateParts = birthDate.split("-");
                    if (dateParts.length == 3) {
                        int birthYear = Integer.parseInt(dateParts[0]);
                        int currentYear = Calendar.getInstance().get(Calendar.YEAR);
                        int age = currentYear - birthYear;
                        tvPatientAge.setText(age + " ans");
                    } else {
                        tvPatientAge.setText("Non renseigné");
                    }
                } catch (Exception e) {
                    tvPatientAge.setText("Non renseigné");
                }
            } else {
                tvPatientAge.setText("Non renseigné");
            }
            
            loadAllergies();
        }

    } catch (Exception e) {
        e.printStackTrace();
        Toast.makeText(this, "Erreur chargement données", Toast.LENGTH_SHORT).show();
    } finally {
        if (cursor != null) cursor.close();
    }
}
```

#### Chargement des Résultats de Tests avec Commentaires
```java
private void loadRecentTestResults() {
    SQLiteDatabase db = dbHelper.getReadableDatabase();
    
    // Masquer toutes les cartes initialement
    cardResultat1.setVisibility(View.GONE);
    cardResultat2.setVisibility(View.GONE);
    cardResultat3.setVisibility(View.GONE);
    
    try {
        Cursor cursor = db.rawQuery(
            "SELECT tr.id, tr.test_name, tr.result, tr.test_date " +
            "FROM test_results tr " +
            "WHERE tr.patient_id = ? " +
            "ORDER BY tr.test_date DESC LIMIT 3",
            new String[]{String.valueOf(patientId)}
        );
        
        CardView[] cards = {cardResultat1, cardResultat2, cardResultat3};
        int cardIndex = 0;
        
        while (cursor.moveToNext() && cardIndex < 3) {
            final int testId = cursor.getInt(0);
            final String testName = cursor.getString(1);
            final String result = cursor.getString(2);
            final String testDate = cursor.getString(3);
            
            cards[cardIndex].setVisibility(View.VISIBLE);
            cards[cardIndex].setOnClickListener(v -> showTestResultDetails(testId, testName, result, testDate));
            
            cardIndex++;
        }
        cursor.close();
        
    } catch (Exception e) {
        e.printStackTrace();
    }
}

private void showTestResultDetails(int testId, String testName, String result, String testDate) {
    SQLiteDatabase db = dbHelper.getReadableDatabase();
    StringBuilder details = new StringBuilder();
    
    details.append("TEST: ").append(testName).append("\n\n");
    details.append("RÉSULTAT:\n").append(result).append("\n\n");
    details.append("DATE: ").append(testDate != null ? testDate : "Non spécifiée").append("\n\n");
    
    // Chargement des commentaires médicaux
    Cursor cursor = db.rawQuery(
        "SELECT trc.comment, u.full_name " +
        "FROM test_result_comments trc " +
        "JOIN users u ON trc.doctor_id = u.id " +
        "WHERE trc.test_result_id = ? " +
        "ORDER BY trc.created_at DESC",
        new String[]{String.valueOf(testId)}
    );
    
    if (cursor.moveToFirst()) {
        details.append("COMMENTAIRES MÉDICAUX:\n");
        do {
            details.append("• Dr. ").append(cursor.getString(1)).append(": ");
            details.append(cursor.getString(0)).append("\n");
        } while (cursor.moveToNext());
    }
    cursor.close();
    
    new AlertDialog.Builder(this)
        .setTitle("Détails du Test")
        .setMessage(details.toString())
        .setPositiveButton("Fermer", null)
        .show();
}
```

---

## 2. REQUÊTES SQL COMPLEXES

### 2.1 Requêtes de Jointure Avancées

#### Chargement des Conversations avec Derniers Messages
```sql
-- Requête complexe pour la messagerie (page_message.java)
SELECT DISTINCT 
    CASE WHEN m.sender_id = ? THEN m.receiver_id ELSE m.sender_id END as other_user_id,
    u.full_name,
    u.role,
    COALESCE(d.specialization, 'Secrétaire') as specialization,
    MAX(m.sent_at) as last_message_time,
    (SELECT message FROM messages m2 
     WHERE (m2.sender_id = ? AND m2.receiver_id = other_user_id) 
        OR (m2.receiver_id = ? AND m2.sender_id = other_user_id)
     ORDER BY m2.sent_at DESC LIMIT 1) as last_message
FROM messages m 
JOIN users u ON (CASE WHEN m.sender_id = ? THEN m.receiver_id ELSE m.sender_id END) = u.id 
LEFT JOIN doctors d ON u.id = d.user_id 
WHERE m.sender_id = ? OR m.receiver_id = ? 
GROUP BY other_user_id 
ORDER BY last_message_time DESC LIMIT 4;
```

#### Statistiques Médecin avec Agrégations
```sql
-- Requête pour les statistiques du tableau de bord médecin
SELECT 
    COUNT(DISTINCT a.patient_id) as total_patients,
    COUNT(DISTINCT CASE WHEN DATE(a.appointment_datetime) = DATE('now') THEN a.id END) as today_appointments,
    COUNT(DISTINCT mr.id) as total_medical_records,
    COUNT(DISTINCT p.id) as total_prescriptions,
    COUNT(DISTINCT CASE WHEN prr.status = 'pending' THEN prr.id END) as pending_refills
FROM appointments a
LEFT JOIN medical_records mr ON a.doctor_id = mr.doctor_id
LEFT JOIN prescriptions p ON a.doctor_id = p.doctor_id
LEFT JOIN prescription_refill_requests prr ON p.id = prr.prescription_id
WHERE a.doctor_id = ?;
```

### 2.2 Requêtes d'Optimisation

#### Requête avec Index Implicites
```sql
-- Optimisation pour la recherche de patients (DoctorPatientsActivity)
SELECT DISTINCT 
    u.id, 
    u.full_name, 
    p.date_of_birth, 
    p.blood_type,
    COUNT(mr.id) as medical_records_count,
    MAX(a.appointment_datetime) as last_appointment
FROM users u 
JOIN patients p ON u.id = p.user_id 
JOIN appointments a ON u.id = a.patient_id 
LEFT JOIN medical_records mr ON u.id = mr.patient_id AND mr.doctor_id = ?
WHERE a.doctor_id = ? AND u.role = 'patient' 
GROUP BY u.id, u.full_name, p.date_of_birth, p.blood_type
ORDER BY last_appointment DESC;
```

#### Requête de Nettoyage et Maintenance
```sql
-- Requête pour nettoyer les sessions expirées
DELETE FROM user_sessions 
WHERE last_activity < datetime('now', '-24 hours');

-- Requête pour archiver les anciens rendez-vous
UPDATE appointments 
SET status = 'archived' 
WHERE appointment_datetime < datetime('now', '-1 year') 
  AND status = 'completed';
```

---

## 3. PATTERNS D'IMPLÉMENTATION

### 3.1 Pattern Adapter pour les Listes

#### Adapter Générique avec ViewHolder Pattern
```java
// Exemple d'adapter optimisé (PatientMedicalRecordsActivity)
private class MedicalRecordAdapter extends BaseAdapter {
    private List<MedicalRecord> records;
    private LayoutInflater inflater;
    
    public MedicalRecordAdapter(List<MedicalRecord> records) {
        this.records = records;
        this.inflater = LayoutInflater.from(PatientMedicalRecordsActivity.this);
    }
    
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        
        if (convertView == null) {
            convertView = inflater.inflate(R.layout.item_medical_record_card, parent, false);
            holder = new ViewHolder();
            holder.tvDiagnosis = convertView.findViewById(R.id.tvDiagnosis);
            holder.tvDoctor = convertView.findViewById(R.id.tvDoctor);
            holder.tvDate = convertView.findViewById(R.id.tvDate);
            holder.tvTreatment = convertView.findViewById(R.id.tvTreatment);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }
        
        MedicalRecord record = records.get(position);
        holder.tvDiagnosis.setText(record.diagnosis);
        holder.tvDoctor.setText("Dr. " + record.doctorName + 
            (record.specialization != null ? " (" + record.specialization + ")" : ""));
        holder.tvDate.setText(record.createdAt);
        
        // Traitement du texte long
        String treatment = record.treatment;
        if (treatment.length() > 100) {
            treatment = treatment.substring(0, 100) + "...";
        }
        holder.tvTreatment.setText(treatment);
        
        return convertView;
    }
    
    private class ViewHolder {
        TextView tvDiagnosis, tvDoctor, tvDate, tvTreatment;
    }
}
```

### 3.2 Pattern Singleton pour les Managers

#### AuthManager Singleton Thread-Safe
```java
public class AuthManager {
    private static volatile AuthManager instance;
    private static final Object LOCK = new Object();
    
    private AuthManager(Context context) {
        dbHelper = new DatabaseHelper(context.getApplicationContext());
        userPermissions = new ArrayList<>();
    }
    
    public static AuthManager getInstance(Context context) {
        if (instance == null) {
            synchronized (LOCK) {
                if (instance == null) {
                    instance = new AuthManager(context);
                }
            }
        }
        return instance;
    }
    
    // Méthodes thread-safe
    public synchronized boolean login(String email, String password) {
        // Implémentation thread-safe
    }
    
    public synchronized void logout() {
        currentUser = null;
        userPermissions.clear();
    }
}
```

### 3.3 Pattern Factory pour les Dialogues

#### Factory pour les Dialogues de Confirmation
```java
public class DialogFactory {
    
    public static AlertDialog createConfirmationDialog(Context context, String title, String message, 
                                                      Runnable onConfirm, Runnable onCancel) {
        return new AlertDialog.Builder(context)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("Confirmer", (dialog, which) -> {
                if (onConfirm != null) onConfirm.run();
            })
            .setNegativeButton("Annuler", (dialog, which) -> {
                if (onCancel != null) onCancel.run();
            })
            .create();
    }
    
    public static AlertDialog createDeleteConfirmationDialog(Context context, String itemName, 
                                                           Runnable onDelete) {
        return createConfirmationDialog(context, 
            "Confirmer la suppression",
            "Supprimer " + itemName + " ? Cette action est irréversible.",
            onDelete, null);
    }
}
```

---

## 4. GESTION DES ERREURS

### 4.1 Gestion Centralisée des Erreurs de Base de Données

#### Wrapper pour les Opérations de Base
```java
public class DatabaseOperations {
    
    public static class Result<T> {
        public final boolean success;
        public final T data;
        public final String error;
        
        private Result(boolean success, T data, String error) {
            this.success = success;
            this.data = data;
            this.error = error;
        }
        
        public static <T> Result<T> success(T data) {
            return new Result<>(true, data, null);
        }
        
        public static <T> Result<T> error(String error) {
            return new Result<>(false, null, error);
        }
    }
    
    public static Result<Long> insertWithErrorHandling(SQLiteDatabase db, String table, 
                                                      ContentValues values) {
        try {
            long result = db.insert(table, null, values);
            if (result != -1) {
                return Result.success(result);
            } else {
                return Result.error("Échec de l'insertion en base de données");
            }
        } catch (SQLException e) {
            Log.e("DatabaseOperations", "Erreur SQL lors de l'insertion", e);
            return Result.error("Erreur de base de données: " + e.getMessage());
        } catch (Exception e) {
            Log.e("DatabaseOperations", "Erreur inattendue lors de l'insertion", e);
            return Result.error("Erreur inattendue: " + e.getMessage());
        }
    }
    
    public static Result<Integer> updateWithErrorHandling(SQLiteDatabase db, String table, 
                                                         ContentValues values, String whereClause, 
                                                         String[] whereArgs) {
        try {
            int result = db.update(table, values, whereClause, whereArgs);
            return Result.success(result);
        } catch (SQLException e) {
            Log.e("DatabaseOperations", "Erreur SQL lors de la mise à jour", e);
            return Result.error("Erreur de base de données: " + e.getMessage());
        } catch (Exception e) {
            Log.e("DatabaseOperations", "Erreur inattendue lors de la mise à jour", e);
            return Result.error("Erreur inattendue: " + e.getMessage());
        }
    }
}
```

#### Utilisation du Wrapper
```java
// Exemple d'utilisation dans une activité
private void addPrescriptionWithErrorHandling(int patientId, String medication, String dosage) {
    SQLiteDatabase db = dbHelper.getWritableDatabase();
    ContentValues values = new ContentValues();
    values.put("patient_id", patientId);
    values.put("doctor_id", doctorId);
    values.put("medication", medication);
    values.put("dosage", dosage);
    
    DatabaseOperations.Result<Long> result = DatabaseOperations.insertWithErrorHandling(
        db, "prescriptions", values);
    
    if (result.success) {
        Toast.makeText(this, "Prescription ajoutée avec succès", Toast.LENGTH_SHORT).show();
        loadPrescriptions();
    } else {
        Toast.makeText(this, "Erreur: " + result.error, Toast.LENGTH_LONG).show();
        Log.e("DoctorPrescriptions", "Échec ajout prescription: " + result.error);
    }
}
```

### 4.2 Validation des Données d'Entrée

#### Classe de Validation
```java
public class InputValidator {
    
    public static class ValidationResult {
        public final boolean isValid;
        public final String errorMessage;
        
        private ValidationResult(boolean isValid, String errorMessage) {
            this.isValid = isValid;
            this.errorMessage = errorMessage;
        }
        
        public static ValidationResult valid() {
            return new ValidationResult(true, null);
        }
        
        public static ValidationResult invalid(String message) {
            return new ValidationResult(false, message);
        }
    }
    
    public static ValidationResult validateEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            return ValidationResult.invalid("L'email est obligatoire");
        }
        
        String emailPattern = "[a-zA-Z0-9._-]+@[a-z]+\\.+[a-z]+";
        if (!email.matches(emailPattern)) {
            return ValidationResult.invalid("Format d'email invalide");
        }
        
        return ValidationResult.valid();
    }
    
    public static ValidationResult validatePassword(String password) {
        if (password == null || password.length() < 6) {
            return ValidationResult.invalid("Le mot de passe doit contenir au moins 6 caractères");
        }
        
        return ValidationResult.valid();
    }
    
    public static ValidationResult validateDate(String date) {
        if (date == null || date.trim().isEmpty()) {
            return ValidationResult.invalid("La date est obligatoire");
        }
        
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            sdf.setLenient(false);
            sdf.parse(date);
            return ValidationResult.valid();
        } catch (ParseException e) {
            return ValidationResult.invalid("Format de date invalide (YYYY-MM-DD attendu)");
        }
    }
}
```

---

## 5. OPTIMISATIONS TECHNIQUES

### 5.1 Optimisation des Requêtes

#### Cache des Requêtes Fréquentes
```java
public class QueryCache {
    private static final Map<String, CacheEntry> cache = new HashMap<>();
    private static final long CACHE_DURATION = 5 * 60 * 1000; // 5 minutes
    
    private static class CacheEntry {
        final Object data;
        final long timestamp;
        
        CacheEntry(Object data) {
            this.data = data;
            this.timestamp = System.currentTimeMillis();
        }
        
        boolean isExpired() {
            return System.currentTimeMillis() - timestamp > CACHE_DURATION;
        }
    }
    
    @SuppressWarnings("unchecked")
    public static <T> T getCachedResult(String key, Class<T> type) {
        CacheEntry entry = cache.get(key);
        if (entry != null && !entry.isExpired()) {
            return (T) entry.data;
        }
        return null;
    }
    
    public static void cacheResult(String key, Object data) {
        cache.put(key, new CacheEntry(data));
    }
    
    public static void clearCache() {
        cache.clear();
    }
}
```

#### Utilisation du Cache
```java
// Exemple dans DoctorPatientsActivity
private void loadPatientsWithCache() {
    String cacheKey = "doctor_patients_" + doctorId;
    List<Patient> cachedPatients = QueryCache.getCachedResult(cacheKey, List.class);
    
    if (cachedPatients != null) {
        patients.clear();
        patients.addAll(cachedPatients);
        adapter.notifyDataSetChanged();
        return;
    }
    
    // Chargement depuis la base de données
    loadPatientsFromDatabase();
    
    // Mise en cache
    QueryCache.cacheResult(cacheKey, new ArrayList<>(patients));
}
```

### 5.2 Optimisation de l'Interface Utilisateur

#### Chargement Asynchrone avec AsyncTask
```java
private class LoadDataTask extends AsyncTask<Void, Void, List<MedicalRecord>> {
    private final WeakReference<PatientMedicalRecordsActivity> activityRef;
    
    LoadDataTask(PatientMedicalRecordsActivity activity) {
        this.activityRef = new WeakReference<>(activity);
    }
    
    @Override
    protected void onPreExecute() {
        PatientMedicalRecordsActivity activity = activityRef.get();
        if (activity != null) {
            // Afficher indicateur de chargement
            activity.showLoadingIndicator(true);
        }
    }
    
    @Override
    protected List<MedicalRecord> doInBackground(Void... voids) {
        PatientMedicalRecordsActivity activity = activityRef.get();
        if (activity == null) return null;
        
        // Chargement des données en arrière-plan
        return activity.loadMedicalRecordsFromDatabase();
    }
    
    @Override
    protected void onPostExecute(List<MedicalRecord> records) {
        PatientMedicalRecordsActivity activity = activityRef.get();
        if (activity != null && records != null) {
            activity.showLoadingIndicator(false);
            activity.updateMedicalRecordsList(records);
        }
    }
}
```

### 5.3 Gestion Mémoire

#### Nettoyage des Ressources
```java
public abstract class BaseActivity extends AppCompatActivity {
    protected DatabaseHelper dbHelper;
    protected AuthManager authManager;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        dbHelper = new DatabaseHelper(this);
        authManager = AuthManager.getInstance(this);
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        
        // Nettoyage des ressources
        if (dbHelper != null) {
            dbHelper.close();
        }
        
        // Nettoyage du cache si nécessaire
        if (isFinishing()) {
            QueryCache.clearCache();
        }
    }
    
    @Override
    public void onLowMemory() {
        super.onLowMemory();
        // Nettoyage en cas de mémoire faible
        QueryCache.clearCache();
        System.gc();
    }
}
```

---

Cette annexe technique complète le rapport principal en fournissant des exemples concrets d'implémentation, des patterns de développement utilisés, et des optimisations techniques appliquées dans l'application Android Health. Elle démontre la qualité du code et l'attention portée aux bonnes pratiques de développement Android.