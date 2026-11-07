# Professor Gesture Control

An Android 9+ gesture utility app.

## Current project status

This repository starts with the Android project foundation. The first implementation milestone focuses on:
- A light Material-style settings dashboard.
- Adjustable shake sensitivity.
- A foreground sensor service that opens the native Android volume UI.
- A permission dashboard that reports actual runtime/device-admin status.

## Important platform limitation

A regular third-party app cannot reliably wake a fully powered-off display by detecting a double tap. Android stops normal app touch processing while the display is off, and wake gestures are normally handled by device firmware. A Home-screen double-tap-to-lock feature also needs a launcher or a user-enabled accessibility integration. We will not claim either feature is active until its implementation and device behavior are verified.

## Build

Open this repository in Android Studio and let Gradle sync. Minimum Android version: Android 9 (API 28).

The project is Kotlin-based and uses Gradle Kotlin DSL.
