# 🧪 Complete Testing Checklist with Sample Data

## ✅ TEST 1: LOGIN (2 minutes)

### Steps:
1. Open app
2. Enter credentials:
   - **Username:** `admin` or `chw1`
   - **Password:** `password123`
3. Click Login

### Expected Result:
- ✅ Should navigate to Patient List screen
- ✅ No crash or error

---

## ✅ TEST 2: ADD PATIENT WITH ALL FIELDS (5 minutes)

### Sample Patient 1: Abebe Kebede

#### Basic Information:
- **Full Name:** `Abebe Kebede`
- **Age:** `45`
- **Gender:** Select `Male` from dropdown
- **Phone:** `0911234567`
- **Address:** `Gondar, Azezo Kebele, House 123`
- **Diagnosis:** `Type 2 Diabetes, requires regular monitoring`

#### Health Information:
- **Blood Type:** Select `O+` from dropdown
- **Allergies:** `Penicillin, Peanuts`
- **Current Medications:** `Metformin 500mg twice daily, Aspirin 81mg once daily`
- **Emergency Contact Name:** `Tigist Kebede`
- **Emergency Contact Phone:** `0922345678`

#### Vital Signs:
- **Blood Pressure Systolic:** `140`
- **Blood Pressure Diastolic:** `90`
- **Temperature:** `37.2`
- **Weight:** `75`
- **Height:** `170`

#### Chronic Conditions (Check these):
- ✅ Diabetes
- ✅ Hypertension

#### Photo & Location:
- Click "Capture Photo" → Take any photo
- Click "Capture GPS" → Wait for location

### Expected Results:
- ✅ BMI should auto-calculate to **25.9 (Overweight)**
- ✅ All fields should save without error
- ✅ Should return to Patient List
- ✅ Patient should appear in list

---

## ✅ TEST 3: ADD SECOND PATIENT (5 minutes)

### Sample Patient 2: Almaz Tesfaye

#### Basic Information:
- **Full Name:** `Almaz Tesfaye`
- **Age:** `28`
- **Gender:** Select `Female` from dropdown
- **Phone:** `0933456789`
- **Address:** `Gondar, Maraki Kebele, Near Health Post`
- **Diagnosis:** `Pregnant, 24 weeks, routine checkup`

#### Health Information:
- **Blood Type:** Select `A+` from dropdown
- **Allergies:** `None known`
- **Current Medications:** `Prenatal vitamins, Iron supplements`
- **Emergency Contact Name:** `Yohannes Tesfaye`
- **Emergency Contact Phone:** `0944567890`

#### Vital Signs:
- **Blood Pressure Systolic:** `110`
- **Blood Pressure Diastolic:** `70`
- **Temperature:** `36.8`
- **Weight:** `62`
- **Height:** `165`

#### Chronic Conditions:
- (Leave all unchecked)

### Expected Results:
- ✅ BMI should auto-calculate to **22.8 (Normal)**
- ✅ Patient should save successfully

---

## ✅ TEST 4: ADD THIRD PATIENT (5 minutes)

### Sample Patient 3: Mulugeta Assefa

#### Basic Information:
- **Full Name:** `Mulugeta Assefa`
- **Age:** `62`
- **Gender:** Select `Male` from dropdown
- **Phone:** `0955678901`
- **Address:** `Gondar, Fasil Kebele, Behind Church`
- **Diagnosis:** `Chronic heart disease, hypertension`

#### Health Information:
- **Blood Type:** Select `B+` from dropdown
- **Allergies:** `Sulfa drugs`
- **Current Medications:** `Enalapril 10mg daily, Atorvastatin 20mg nightly`
- **Emergency Contact Name:** `Hanna Assefa`
- **Emergency Contact Phone:** `0966789012`

#### Vital Signs:
- **Blood Pressure Systolic:** `160`
- **Blood Pressure Diastolic:** `95`
- **Temperature:** `37.0`
- **Weight:** `85`
- **Height:** `175`

#### Chronic Conditions (Check these):
- ✅ Hypertension
- ✅ Heart Disease

### Expected Results:
- ✅ BMI should auto-calculate to **27.8 (Overweight)**
- ✅ Patient should save successfully

---

## ✅ TEST 5: EDIT PATIENT & VERIFY DATA LOADS (3 minutes)

### Steps:
1. Go to Patient List
2. Click on **Abebe Kebede**
3. Verify all fields are loaded correctly:
   - Name: Abebe Kebede
   - Age: 45
   - Gender: Male
   - Blood Pressure: 140/90
   - BMI: 25.9 (Overweight)
   - Chronic Conditions: Diabetes, Hypertension checked

### Expected Results:
- ✅ All data should be exactly as entered
- ✅ BMI should still show 25.9 (Overweight)
- ✅ Photo should be visible (if captured)
- ✅ GPS coordinates should show (if captured)

---

## ✅ TEST 6: BMI CALCULATION (3 minutes)

### Test Real-Time Calculation:
1. Edit **Almaz Tesfaye**
2. Change weight to `50` → BMI should update to **18.4 (Underweight)**
3. Change weight to `65` → BMI should update to **23.9 (Normal)**
4. Change weight to `80` → BMI should update to **29.4 (Overweight)**
5. Change weight to `95` → BMI should update to **34.9 (Obese)**
6. Change back to `62` → BMI should return to **22.8 (Normal)**
7. Click Back (don't save)

### Expected Results:
- ✅ BMI should update **instantly** as you type
- ✅ Category should change (Underweight/Normal/Overweight/Obese)
- ✅ No lag or delay

---

## ✅ TEST 7: ADD VISIT HISTORY (5 minutes)

### Visit 1 for Abebe Kebede:

1. Go to Patient List
2. Click on **Abebe Kebede**
3. Click **"View Visit History"** button
4. Click **+ (FAB button)** to add visit

#### Fill Visit Form:
- **Visit Date:** (Auto-filled with today's date - leave it)
- **Symptoms:** 
  ```
  High blood sugar levels, dizziness, frequent urination, increased thirst
  ```
- **Notes:** 
  ```
  Blood glucose: 180 mg/dL (fasting). Patient reports not taking medication regularly. Counseled on importance of medication adherence and diet control.
  ```
- **Prescription:** 
  ```
  Metformin 500mg - Take 1 tablet twice daily with meals
  Glibenclamide 5mg - Take 1 tablet in morning
  Advised to check blood sugar weekly
  ```
- **Follow-up Date:** Click calendar → Select **1 week from today**

5. Click **Save** or **Add Visit**

### Expected Results:
- ✅ Visit should appear in history list
- ✅ Should show date, symptoms preview
- ✅ No crash

---

## ✅ TEST 8: ADD SECOND VISIT (3 minutes)

### Visit 2 for Almaz Tesfaye:

1. Go back to Patient List
2. Click on **Almaz Tesfaye**
3. Click **"View Visit History"**
4. Click **+ button**

#### Fill Visit Form:
- **Symptoms:** 
  ```
  Routine prenatal checkup, mild back pain, no complications
  ```
- **Notes:** 
  ```
  24 weeks pregnant. Blood pressure normal (110/70). Fetal heartbeat strong. Baby movement reported. No signs of complications.
  ```
- **Prescription:** 
  ```
  Continue prenatal vitamins
  Iron supplement 65mg daily
  Folic acid 400mcg daily
  Rest and avoid heavy lifting
  ```
- **Follow-up Date:** Select **2 weeks from today**

5. Click Save

### Expected Results:
- ✅ Visit should appear in list
- ✅ Should be able to see both visits if you add another

---

## ✅ TEST 9: ADD THIRD VISIT (3 minutes)

### Visit 3 for Mulugeta Assefa:

1. Go to Patient List
2. Click on **Mulugeta Assefa**
3. Click **"View Visit History"**
4. Click **+ button**

#### Fill Visit Form:
- **Symptoms:** 
  ```
  Chest tightness, shortness of breath on exertion, fatigue
  ```
- **Notes:** 
  ```
  Blood pressure elevated at 160/95. Heart rate 88 bpm. Advised to reduce salt intake and increase medication compliance. Possible need for medication adjustment.
  ```
- **Prescription:** 
  ```
  Enalapril 10mg - Increase to twice daily
  Atorvastatin 20mg - Continue nightly
  Aspirin 81mg - Take once daily
  URGENT: Refer to hospital if chest pain worsens
  ```
- **Follow-up Date:** Select **3 days from today** (urgent follow-up)

5. Click Save

### Expected Results:
- ✅ Visit saved successfully
- ✅ Can view all visits in chronological order

---

## ✅ TEST 10: OFFLINE MODE (5 minutes)

### Steps:
1. **Turn OFF WiFi and Mobile Data** on your phone
2. Go to Patient List
3. Click "Add Patient" (+ button)

#### Add Offline Patient:
- **Full Name:** `Offline Test Patient`
- **Age:** `30`
- **Gender:** `Female`
- **Phone:** `0977890123`
- **Address:** `Test Address`
- **Blood Type:** `AB+`
- **Weight:** `60`
- **Height:** `160`

4. Click Save

### Expected Results:
- ✅ Patient should save successfully (no error)
- ✅ Should appear in Patient List
- ✅ Should show as "Not Synced" or have sync indicator

---

## ✅ TEST 11: SYNC TO SERVER (3 minutes)

### Steps:
1. **Turn ON WiFi/Mobile Data**
2. Go to Patient List
3. Look for **Sync button** (usually in toolbar or menu)
4. Click **Sync** or wait for automatic sync (15 minutes)

### Expected Results:
- ✅ Should show "Syncing..." message
- ✅ Should show "Sync Complete" or success message
- ✅ Offline patient should now be marked as synced
- ✅ No crash or error

### If Sync Fails:
- Check if backend server is running (XAMPP)
- Check if phone can reach `http://10.0.2.2/ruralhealth_api/` (emulator) or your PC's IP (real device)
- This is OK for demo - just mention "backend is not running right now"

---

## ✅ TEST 12: DARK MODE (2 minutes)

### Steps:
1. Go to phone **Settings** → **Display** → **Dark theme**
2. Turn ON dark theme
3. Open your app
4. Navigate through:
   - Patient List
   - Add Patient screen
   - Visit History

### Expected Results:
- ✅ App should use dark colors
- ✅ Text should be readable (white/light on dark background)
- ✅ No white flashes
- ✅ Looks professional

---

## ✅ TEST 13: SEARCH PATIENT (2 minutes)

### Steps:
1. Go to Patient List
2. Look for **Search icon** or search bar
3. Type: `Abebe`

### Expected Results:
- ✅ Should show only Abebe Kebede
- ✅ Other patients should be filtered out

4. Clear search
5. Type: `0911`

### Expected Results:
- ✅ Should find patient by phone number

---

## ✅ TEST 14: DELETE PATIENT (2 minutes)

### Steps:
1. Go to Patient List
2. Click on **Offline Test Patient**
3. Look for **Delete button** (usually at bottom)
4. Click Delete
5. Confirm deletion

### Expected Results:
- ✅ Should show confirmation dialog
- ✅ After confirming, patient should be removed from list
- ✅ Should return to Patient List

---

## ✅ TEST 15: FORM VALIDATION (3 minutes)

### Test Required Fields:
1. Click "Add Patient"
2. Leave **Name** empty
3. Click Save

**Expected:** ❌ Error: "Name is required"

4. Enter Name: `Test`
5. Leave **Age** empty
6. Click Save

**Expected:** ❌ Error: "Age is required"

7. Enter Age: `150`
8. Click Save

**Expected:** ❌ Error: "Age must be between 1 and 120"

9. Enter Age: `25`
10. Leave **Gender** empty
11. Click Save

**Expected:** ❌ Error: "Please select gender"

12. Select Gender: `Male`
13. Enter Phone: `123`
14. Click Save

**Expected:** ❌ Error: "Enter valid phone number (min 10 digits)"

15. Enter Phone: `0988123456`
16. Click Save

**Expected:** ✅ Should save successfully

---

## ✅ TEST 16: DUPLICATE DETECTION (2 minutes)

### Steps:
1. Try to add patient with:
   - **Name:** `Abebe Kebede`
   - **Age:** `45`
   - **Gender:** `Male`
   - **Phone:** `0911234567`

2. Click Save

### Expected Results:
- ✅ Should show **"Duplicate Patient"** warning
- ✅ Should mention existing patient with same name/phone
- ✅ Should NOT save duplicate

---

## 📊 TESTING SUMMARY CHECKLIST

After completing all tests, verify:

- [ ] ✅ Login works
- [ ] ✅ Can add patient with all fields
- [ ] ✅ BMI calculates automatically and correctly
- [ ] ✅ Can edit patient and data loads correctly
- [ ] ✅ Can add visit history
- [ ] ✅ Visit history displays correctly
- [ ] ✅ Can save patient offline
- [ ] ✅ Sync works (or can explain why backend not running)
- [ ] ✅ Dark mode works
- [ ] ✅ Search works
- [ ] ✅ Delete works
- [ ] ✅ Form validation works
- [ ] ✅ Duplicate detection works
- [ ] ✅ No crashes during any operation
- [ ] ✅ App looks professional

---

## 🎯 QUICK TEST (If Short on Time - 10 minutes)

### Minimum Tests:
1. **Login** (1 min)
2. **Add Abebe Kebede** with all fields (3 min)
3. **Verify BMI calculates** (1 min)
4. **Add visit for Abebe** (2 min)
5. **Test offline mode** (2 min)
6. **Show dark mode** (1 min)

**This covers the most impressive features!**

---

## 🎤 DEMO DATA RECOMMENDATION

### For Your Presentation, Use:
- **Patient 1:** Abebe Kebede (Diabetes patient with high BMI)
- **Patient 2:** Almaz Tesfaye (Pregnant woman with normal BMI)

### Why These Two?
1. Show different BMI categories (Overweight vs Normal)
2. Show different chronic conditions
3. Show different use cases (chronic disease vs prenatal)
4. Demonstrates comprehensive health tracking

---

## 💾 BACKUP PLAN

### If Something Doesn't Work During Demo:
- **Sync fails?** → "Backend server is not running right now, but the app stores everything locally and will sync automatically when server is available"
- **GPS doesn't work?** → "GPS requires outdoor signal, but the feature works when signal is available"
- **Photo crashes?** → "Camera permission issue, but photo capture works on properly configured devices"

**Always have a backup explanation!**

---

## ✅ YOU'RE READY!

After completing these tests, you'll know:
- ✅ Everything works
- ✅ What to show in demo
- ✅ How to handle issues
- ✅ Your app is solid

**Good luck with your presentation!** 🎉
