# Professor Gesture Control

An Android 9+ utility app with a lightweight custom Home screen.

## Implemented in this milestone

- Double-tap a marked area on the app's Home screen to lock the display.
- Uses Android Device Administrator only for the force-lock action. The user must explicitly activate this permission.
- Offers a setting shortcut to choose Professor Gesture Control as the default Home app.
- Shows launchable installed apps so the custom Home screen remains usable.
- Shake detection opens Android's native volume panel.
- Adjustable shake sensitivity.
- Basic permission and sensor status dashboard.
- GitHub Actions workflow builds a debug APK on pushes and pull requests.

## Important limitations

- Double-tap-to-lock works in the marked gesture area of this app when it is selected as the default Home app. It does not capture double taps on other launchers' wallpaper.
- Double-tap-to-wake while the display is fully off is controlled by phone firmware and cannot be implemented reliably by a normal third-party app. Use the phone's built-in wake gesture if available.
- This source has been pushed, but it still needs a successful CI build and on-device testing before it should be considered release-ready.

## Build

Open the repository in Android Studio and sync Gradle. Use JDK 17 and Gradle 8.9 or compatible wrapper. Minimum Android version: Android 9 (API 28).

## First-time setup on the phone

1. Install the debug APK.
2. Open Professor Gesture Control.
3. Tap **Allow screen-lock permission** and approve the Android Device Admin prompt.
4. Tap **Set as default Home app** and select Professor Gesture Control.
5. Double-tap the marked blue area to lock the screen.
6. Enable shake volume control and adjust sensitivity as needed.

To return to the previous launcher, open Android Settings and change the default Home app.
