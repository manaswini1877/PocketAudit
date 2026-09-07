# 🛡️ PocketAudit — On-Device UPI & Banking Scam Guard

> **Hackathon MVP**: Native Android application that automatically intercepts incoming banking, UPI, and SMS notifications in real time to detect fraud patterns — **100% on-device, zero network calls, total privacy guarantee**.

---

## 🎯 The Problem & Our Solution

### Problem
Fake UPI collect requests, phishing bank SMS, and silent subscription charges target millions of users daily. Existing anti-fraud tools require manually copying and pasting suspicious messages into web portals or sending sensitive banking notifications to third-party cloud servers.

### PocketAudit Differentiator
- **Automatic & Real-Time**: Listens to incoming notifications via native Android `NotificationListenerService` the instant they arrive.
- **100% On-Device**: Zero data leaves the user's phone. No internet permission (`android.permission.INTERNET`) is included in the manifest.
- **Offline Detection Engine**: Rule-based scam detection engine analyzing UPI collect tricks, SMS phishing links, and silent subscription traps locally.

---

## 🏗️ Architecture

```
[ Incoming Notification / Demo Simulator ]
                   │
                   ▼
  [ NotificationListenerService ]
        (App Package Filter)
                   │
                   ▼
         [ DetectionEngine ]
   ┌───────────────┼───────────────┐
   ▼               ▼               ▼
[Collect]      [Phishing]    [Subscription]
Detector       Detector        Detector
   └───────────────┬───────────────┘
                   │  RiskScore & Explanation
                   ▼
          [ AlertRepository ]
                   │
                   ▼
           [ Room Database ]
                   │
                   ▼
        [ Jetpack Compose UI ]
 (Dashboard | Alert Detail | Permissions | Demo Mode)
```

---

## 🔍 Core Scam Detectors

| Detector | What it Detects | Risk Indicators |
|---|---|---|
| **`CollectRequestDetector`** | Fake UPI Collect Requests | Flags "collect request", unusual VPA handles (`cashback.claim@ybl`), "Enter PIN to receive refund" tricks (remember: PIN always DEBITS money). |
| **`PhishingLinkDetector`** | Banking Phishing Links | Detects shortened URLs (`bit.ly`, `tinyurl`), raw IP URLs, suspicious TLDs (`.xyz`, `.site`), urgency panic ("account blocked in 24 hours"), spoofed bank domains (`sbi-verify-kyc.xyz`). |
| **`SubscriptionTrapDetector`** | Silent Subscription Traps & Duplicate Charges | Identifies recurring autopay mandates, unfamiliar descriptor merchants (`Unknown global services`), and duplicate charges within a short 5-minute window. |

---

## ⚡ OEM Battery Optimization & Live Testing (Important for iQOO 15 / Xiaomi / Vivo / Oppo)

> [!WARNING]
> **Background Service Killing on OEM Devices**:
> On devices running custom Android skins (e.g. **Vivo / iQOO 15** Funtouch OS / OriginOS, **Xiaomi** MIUI / HyperOS, **Oppo / Realme** ColorOS), Android aggressive battery optimization will automatically kill background notification listener services after a few minutes.

### Action Required for Live Hackathon Testing
1. Enable **Autostart** for PocketAudit in phone Settings.
2. Set Battery Saver / Optimization for PocketAudit to **Unrestricted** / **No restrictions**.
3. Use the in-app **"OEM Battery & Autostart Tip"** button on the permission onboarding screen to jump directly to system battery settings.

---

## 🎬 Hackathon Live Demo Mode

To demo PocketAudit live to hackathon judges without relying on a real bank SMS or live UPI payment arriving on stage:

1. Open PocketAudit and tap **"Demo Simulator Mode"** on the dashboard.
2. Tap any of the 4 interactive test scenario buttons:
   - 🚨 **Fake UPI Collect Request**: `₹4,999` refund trick from `cashback-claim@ybl`
   - ⚠️ **Phishing SMS Alert**: HDFC account blocked panic with `bit.ly` shortened link
   - 🔄 **Silent Subscription Trap**: `₹999` debited by `Unknown global services`
   - ✅ **Legitimate Payment**: Normal GPay transaction to Corner Bakery
3. Tap **"View Results on Live Dashboard"** to watch the alerts populate real-time in Room DB with color-coded risk cards (Red/Yellow/Green) and plain-language explanations.

---

## 🧪 Running Unit Tests

Run all detection engine unit tests locally via Gradle:

```bash
./gradlew test
```

Test files located in `app/src/test/java/com/pocketaudit/app/detection/`:
- `CollectRequestDetectorTest.kt`
- `PhishingLinkDetectorTest.kt`
- `SubscriptionTrapDetectorTest.kt`
- `DetectionEngineTest.kt`

---

## 🛡️ Privacy Guarantee

PocketAudit's `AndroidManifest.xml` explicitly omits network permissions:

```xml
<!-- Zero network/internet permission declared -->
```

All data processing, pattern matching, and persistence are performed exclusively on your local device.
"# PocketAudit" 
