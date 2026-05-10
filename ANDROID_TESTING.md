# Android testing

Run the critical Compose UI smoke suite before release builds:

```powershell
.\gradlew.bat :app:connectedDebugAndroidTest
```

This covers the auth, home, settings, and logout path on a connected emulator or device.
