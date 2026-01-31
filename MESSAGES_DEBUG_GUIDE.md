# Messages Page Debugging Guide

## Problem
Doctor's messages page shows "Erreur lors du chargement des conversations" and displays empty.

## Root Cause Analysis
The original query in `DoctorMessagesActivity.java` was trying to access a non-existent column `medical_history` from the `patients` table, causing the SQL query to fail.

## Fixes Applied

### 1. Fixed SQL Query (DoctorMessagesActivity.java)
- **Removed**: Reference to non-existent `medical_history` column
- **Removed**: Unnecessary LEFT JOIN with patients table
- **Added**: `has_urgent` field to identify urgent messages from secretaries
- **Added**: Proper ordering to show urgent messages first
- **Added**: Warning emoji (⚠️) for conversations with urgent messages

### 2. Added Comprehensive Logging
Added debug logging to track:
- Total messages in database for the doctor
- Number of conversations found
- Details of each conversation loaded
- Message insertion from patients and secretaries

### 3. Created Diagnostic Tools

#### DatabaseDiagnosticActivity.java
A comprehensive diagnostic screen that shows:
- Database file path
- Current logged-in user
- All users in the system
- All messages with sender/receiver details
- Test query results for doctor messages

**How to access**: Long-press the Messages card on the Doctor Dashboard

#### check_messages.sql
SQL queries to manually verify database contents

## How to Debug

### Step 1: Check if messages exist
1. Login as doctor
2. Go to Doctor Dashboard
3. **Long-press** (not click) the Messages card
4. This opens the Database Diagnostic screen
5. Check:
   - Are there any messages in the database?
   - What are the sender_id and receiver_id values?
   - Does the doctor's ID match any sender_id or receiver_id?

### Step 2: Check Logcat
When you click the Messages card (normal click), check Android Logcat for:
```
Tag: DoctorMessages
- "Loading messages for doctor ID: X"
- "Total messages involving this doctor: X"
- "Query returned X conversations"
- "Conversation with: [name]..."
```

### Step 3: Test Message Sending

#### From Patient:
1. Login as patient
2. Go to Messages/Compose
3. Select a doctor
4. Send a message
5. Check Logcat for:
   ```
   Tag: ComposeMessage
   - "Sending message from patient X to doctor Y"
   - "Insert result: [number]" (should be > 0)
   ```

#### From Secretary (Urgent):
1. Login as secretary
2. Go to Urgent Requests
3. Send urgent message to doctor
4. Check Logcat for:
   ```
   Tag: UrgentRequests
   - "Sending urgent message from secretary X to doctor Y"
   - "Insert result: [number]" (should be > 0)
   ```

### Step 4: Verify Database Schema
The messages table should have these columns:
- id (INTEGER PRIMARY KEY)
- sender_id (INTEGER)
- receiver_id (INTEGER)
- message (TEXT)
- is_urgent (INTEGER, 0 or 1)
- sent_at (DATETIME)

## Common Issues to Check

### Issue 1: No messages in database
**Symptom**: Diagnostic shows "NO MESSAGES FOUND"
**Cause**: Messages aren't being saved
**Solution**: Check if message sending returns error, verify database write permissions

### Issue 2: Wrong doctor ID
**Symptom**: Messages exist but not for this doctor
**Cause**: Doctor ID mismatch
**Solution**: Verify the logged-in doctor's ID matches the receiver_id or sender_id in messages

### Issue 3: Database version mismatch
**Symptom**: App crashes or schema errors
**Cause**: Old database version
**Solution**: Uninstall and reinstall app, or increment DATABASE_VERSION in DatabaseHelper.java

### Issue 4: Multiple database instances
**Symptom**: Messages saved but not visible
**Cause**: App creating multiple database files
**Solution**: Check database path in diagnostic, ensure only one database file exists

## Expected Behavior

### For Doctors:
- Should see conversations with patients (is_urgent = 0)
- Should see conversations with secretaries (is_urgent = 1) with ⚠️ icon
- Urgent messages appear first
- Most recent conversations appear at top

### Message Flow:
1. **Patient → Doctor**: Normal message (is_urgent = 0)
2. **Secretary → Doctor**: Urgent message (is_urgent = 1)
3. **Doctor → Patient/Secretary**: Reply (is_urgent = 0)

## Files Modified
1. `DoctorMessagesActivity.java` - Fixed query, added logging
2. `ComposeMessageActivity.java` - Added logging
3. `UrgentRequestsActivity.java` - Added logging, cleaned imports
4. `DoctorDashboardActivity.java` - Added diagnostic long-press
5. `DatabaseDiagnosticActivity.java` - NEW diagnostic tool
6. `check_messages.sql` - NEW SQL verification queries

## Next Steps
1. Run the app and check the diagnostic screen
2. Review Logcat output
3. If no messages exist, try sending a test message
4. If messages exist but don't show, verify the doctor ID
5. Report findings with Logcat output
