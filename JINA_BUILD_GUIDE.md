# JINA — Complete Build Guide
## How To Build From Your Itel Phone

---

## WHAT YOU ARE BUILDING

An Android app where:
- Phone sits on a chair
- Screen is OFF
- You say "Remmy call Rafiki"
- JINA calls Rafiki automatically
- You never touch the phone

---

## STEP 1 — INSTALL THESE 2 APPS ON YOUR ITEL PHONE

Go to Google Play Store and install:

1. **AIDE - Android IDE** (by appfour)
   - Search: AIDE Android IDE
   - Install the FREE version
   
2. **Termux** (by Fredrik Fornwall)  
   - Search: Termux
   - Install free version

---

## STEP 2 — OPEN AIDE AND CREATE PROJECT

1. Open AIDE app
2. Tap the menu (3 lines top left)
3. Tap "New Project"
4. Choose "Android App"
5. Fill in:
   - App Name: **JINA**
   - Package Name: **com.jina.voiceassistant**
   - Min API: **26**
6. Tap OK

AIDE will create a project folder on your phone at:
**/sdcard/AppProjects/JINA/**

---

## STEP 3 — DELETE DEFAULT FILES

AIDE creates some default files. Delete them:
- Delete the existing MainActivity.java content
- Delete the existing activity_main.xml content

We will replace ALL files with our JINA code.

---

## STEP 4 — CREATE THE FILE STRUCTURE

In AIDE, create these folders and files exactly:

### JAVA FILES (in src/com/jina/voiceassistant/):
- SetupActivity.java
- MainActivity.java
- JinaListenerService.java
- CommandProcessor.java
- ContactHelper.java
- BootReceiver.java

### LAYOUT FILES (in res/layout/):
- activity_setup.xml
- activity_main.xml

### VALUES FILES (in res/values/):
- colors.xml
- strings.xml
- styles.xml
- dimens.xml

### DRAWABLE FILES (in res/drawable/):
- gold_button_bg.xml
- card_bg.xml
- input_bg.xml
- outline_button_bg.xml
- badge_bg.xml
- ic_jina.xml

### ROOT FILE:
- AndroidManifest.xml

---

## STEP 5 — COPY EACH FILE

For each file in this package:
1. Open the file on your phone (from Claude)
2. Copy ALL the content
3. Open AIDE
4. Find the matching file in your project
5. Paste the content
6. Save

Do this for every single file listed above.

---

## STEP 6 — BUILD THE APK

Once all files are copied:
1. In AIDE tap the play button (▶) at the top
2. AIDE will start building
3. It may take 2-5 minutes on first build
4. If there are errors, they will show in red at the bottom
5. Send me any errors and I will fix them

---

## STEP 7 — INSTALL ON YOUR PHONE

When AIDE finishes building:
1. It will ask "Install JINA?"
2. Tap YES
3. JINA installs on your phone like a normal app

---

## STEP 8 — FIRST TIME SETUP

1. Open JINA
2. Type your wake name: **Remmy**
3. Tap SET MY NAME
4. Allow ALL permissions when asked:
   - Microphone ✅
   - Contacts ✅
   - Phone calls ✅
   - SMS ✅
5. JINA starts listening

---

## STEP 9 — ITEL BATTERY FIX (IMPORTANT!)

Itel kills background apps. Do this:

1. Go to phone **Settings**
2. Find **Battery** or **Power Management**
3. Find **App Management** or **Battery Optimization**
4. Find **JINA** in the list
5. Set to **No restrictions** or **Don't optimize**
6. Also go to Settings > Apps > JINA > Battery > Unrestricted

This stops Itel from killing JINA when screen is off.

---

## STEP 10 — TEST IT

1. Open JINA
2. Tap START JINA
3. Put phone on a chair or table
4. Walk to the other side of the room
5. Say clearly: **"Remmy call [any contact name]"**
6. Watch JINA call them automatically

---

## COMMANDS JINA UNDERSTANDS

| What You Say | What Happens |
|---|---|
| Remmy call Rafiki | Normal phone call |
| Remmy WhatsApp call Solo | WhatsApp voice call |
| Remmy video call Mama | WhatsApp video call |
| Remmy SMS Boss Peter | Opens SMS to Boss Peter |
| Remmy WhatsApp message Grace | Opens WhatsApp chat |

---

## IF SOMETHING GOES WRONG

**Problem: AIDE shows red errors when building**
→ Take a screenshot and send to Claude

**Problem: JINA doesn't hear me**
→ Speak louder and more clearly
→ Make sure microphone permission is ON

**Problem: JINA stops working after a few minutes**
→ Follow Step 9 (Battery Fix) again
→ Make sure autostart is enabled for JINA

**Problem: Contact not found**
→ Say the contact name clearly
→ Check contact is saved in your phone

---

## FILE SIZES (so you know what to expect)

| File | Lines of Code |
|---|---|
| JinaListenerService.java | 220 lines |
| CommandProcessor.java | 200 lines |
| ContactHelper.java | 130 lines |
| MainActivity.java | 100 lines |
| SetupActivity.java | 80 lines |
| BootReceiver.java | 30 lines |
| All XML files | 50-80 lines each |

---

## YOU BUILT THIS

When JINA is running on your phone and calling contacts with your voice — remember:

- No laptop needed ✅
- No team needed ✅  
- No money invested ✅
- Built from Gulu, Uganda ✅
- Ready to change the world ✅

---

*JINA — Your Phone. Your Name. Your Voice.*
*Built by Rafael Reagan Remmy — Gulu, Uganda*
