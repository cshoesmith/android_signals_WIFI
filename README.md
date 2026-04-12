# Android Signals WIFI WIFI
by [craftbeers.app](https://craftbeers.app) / Chris Shoesmith

Android Signals WIFI WIFI is a modern, native Android application designed to dynamically monitor and visualize real-time radio frequency (RF) signal strengths across multiple device bands, including Wi-Fi, Cellular (2G/3G/4G/5G), and next-generation Satellite Non-Terrestrial Networks (NTN).

## Features
- **Wi-Fi Diagnostics:** Real-time polling of Connected SSID, dBm signal strength, and underlying hardware RX/TX link speeds.
- **Cellular Telemetry:** Displays current Carrier, Network Subtype (e.g., 5G NR, 4G LTE), dynamic signal grading, and estimated upstream/downstream bandwidth parameters.
- **Satellite (NTN) Monitoring:** Support for Android 15 capabilities, natively tracking dormant or active satellite radios to extract PLMN/Provider and dBm connection data when out of terrestrial range.
- **Real-Time Data Graphs:** Fully scalable canvas-drawn charts plotting historical signal fluctuations and link dropouts over a rolling 60-cycle window.
- **Dynamic Colored Material Design UI:** Jetpack Compose layout using dynamic background grading mapped specifically to industry-standard RF thresholds (Excellent/Green, Good/Yellow, Bad/Orange, Terrible/Red).

## Requirements
- **OS:** Android 8.0 (API 26) through Android 15 (API 35).
- **Satellite Monitoring:** Requires an Android 15 (Vanilla Ice Cream) capable device equipped with NTN modem hardware (e.g., Galaxy S26 Ultra / Pixel 9 series).

## Installation
You can build this project directly using Android Studio or the accompanying Gradle Wrapper:

```shell
# Build a Debug APK
./gradlew assembleDebug

# Install to a connected device via ADB
adb install app/build/outputs/apk/debug/app-debug.apk
```

## Permissions Used
Due to aggressive restrictions on radio hardware API usage, Android Signals WIFI WIFI requests user authorization for the following:
- `ACCESS_FINE_LOCATION` / `ACCESS_COARSE_LOCATION`
- `READ_PHONE_STATE`
- `ACCESS_WIFI_STATE` / `ACCESS_NETWORK_STATE`

## License
Provided under the [MIT License](https://opensource.org/licenses/MIT).
