feat: Complete database-driven CRUD operations for all user roles

## Secretary CRUD Operations:
- Convert FormRdvActivity to use database with patient/doctor selection
- Replace static data in RdvListActivity with real appointment CRUD
- Add SecretaryPatientManagementActivity for patient management
- Create database-driven patient add/edit/view functionality

## Doctor CRUD Operations:
- DoctorPatientsActivity: Full patient management with medical records and test results CRUD
- DoctorPrescriptionsActivity: Complete prescription management with refill request handling
- DoctorMedicalRecordsActivity: Medical record creation and viewing
- DoctorAppointmentsActivity: Appointment status management
- All doctor activities use real database queries, no mock data

## Patient CRUD Operations:
- PatientDashboardActivity: Real appointment loading from database
- book_appointment.java: Database-driven doctor selection and appointment booking
- page_medicament.java: Real prescription data loading, medication card management
- page_dossier_medical.java: Real patient data, calculated age, database-driven allergies and test results
- page_message.java: Dynamic conversation loading from database, removed hardcoded doctor names
- PatientMedicalRecordsActivity: Comprehensive medical records viewing with test results
- PatientAppointmentsActivity: Full appointment management with cancellation

## Admin Doctor Management:
- Add AdminDoctorManagementActivity with full doctor CRUD
- Create, Read, Update, Delete doctor profiles
- Toggle doctor active/inactive status
- Manage doctor specializations and license numbers

## Complete Mock Data Removal:
- **page_medicament.java:** Now loads real prescription data, dynamic medication cards
- **page_dossier_medical.java:** Real patient data, calculated age from birth date, database allergies, dynamic test result cards
- **page_message.java:** Dynamic conversation loading, removed hardcoded "Dr. Rachid Bennani", "Dr. Fatima Zahra", etc.
- **All patient activities:** Replaced static data with dynamic database queries
- **All cards and UI elements:** Show/hide based on actual data availability

## Database & Technical Improvements:
- Fix secretary permissions in database (added missing permissions)
- Add DoctorProfileFixer to handle missing doctor profiles
- Updated DatabaseHelper with missing secretary permissions
- Added LEFT JOIN queries to handle missing doctor profiles
- Created comprehensive layouts for all CRUD operations
- Added proper permission checks throughout all activities
- Implement appointment confirmation/cancellation with database updates
- Add sample doctor creation if none exist

## New Layouts & Components Created:
- activity_admin_doctor_management.xml
- activity_secretary_patient_management.xml
- activity_patient_medical_records.xml
- item_doctor_card.xml, item_patient_card.xml, item_medical_record_card.xml
- dialog_add_doctor.xml, dialog_add_patient.xml
- status_background.xml drawable

## Key Features Implemented:
- **Complete CRUD:** Create, Read, Update, Delete operations for all entities
- **Real-time Data:** All UI elements reflect actual database state
- **Dynamic UI:** Cards show/hide based on data availability
- **Permission-based Access:** Proper role-based access control
- **Data Relationships:** Proper foreign key relationships and JOIN queries
- **User Experience:** Detailed views, confirmation dialogs, error handling

This completes **100% database integration** with **comprehensive CRUD operations** for all user roles and **zero mock data** remaining in the application. All UI elements are now driven by real database queries with proper error handling and user feedback.