# Focus Launcher — Minimalist Phone Experience

Focus Launcher is a clean, premium, and distraction-free productivity launcher designed to help you regain control over your screen time. It replaces chaotic icon-centric home screens with elegant, text-only list interfaces and adds mindful friction to reduce phone addiction.

---

## 🛠️ System Privileges & USB Debugging Guide

To unlock the launcher's advanced native features—such as hiding the system status bar, absolute grayscale, and incoming distraction mutes—follow these steps:

### 1. Enable Developer Options
1. Open your phone's **Settings**.
2. Navigate to **About Phone** (or **System -> About Device**).
3. Scroll down and tap **Build Number** exactly **7 times**.
4. Enter your PIN/Password when prompted. You will see a toast saying: *"You are now a developer!"*

### 2. Activate USB Debugging
1. Go back to main **Settings** page -> search for **Developer Options** (usually found in **System** or **Additional Settings**).
2. Scroll to find **USB Debugging** and toggle it **ON**.
3. **Xiaomi/Redmi/POCO/Realme users (CRITICAL):** Enable **USB Debugging (Security Settings)**. This is required to allow inputting/setting system configs via ADB.

### 3. Grant Natively Secure Writing Privileges (via ADB)
Connect your phone to a laptop/computer using a high-quality USB cable. Make sure you have installed standard ADB platform tools on your computer.

1. Open your computer's terminal (Command Prompt, PowerShell, or macOS Terminal).
2. Type command `adb devices` to verify authorization (allow key pairing on your phone screen when prompted).
3. Run this exact command to grant secure settings control:
   ```bash
   adb shell pm grant com.aistudio.minimalist android.permission.WRITE_SECURE_SETTINGS
   ```
4. Now, the **Hide Status Bar** feature can lock and hide signal/notification clutter natively on vanilla Android!

### 4. Enable Notification Access (Muted Alerts Dashboard)
To display and collect incoming alerts safely inside the launcher's *Muted Notification* panel, follow:
1. Go to phone **Settings**.
2. Search for **Device & App Notifications** (or **Notification Access**).
3. Locate **Focus Launcher** and toggle access to **ALLOWED**.

---

## ✨ Features Built

* 📱 **Text-Based Home Screen:** Zero visual grid noise. Beautiful, left-aligned, pure text-based list layout.
* 🧘 **Minimalist Lockscreen Friction:** A gorgeous lockscreen cover that intercepts mindless scrolling attempts and forces a peaceful 1.2-second breathing pause ("Inhale...", "Exhale...") before unlocking.
* 🕶️ **Grayscale Mode:** Dynamically desaturates the screen under customization preferences.
* ⏳ **Focus Sessions:** Turns off distracting elements and counts down focus slots with ambient statistics.
* 📵 **Distraction Shield:** Blocks high-trigger applications (such as TikTok, Instagram, and YouTube) with elegant overlay warning messages.
