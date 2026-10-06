# Hide and Seek: Location-Based Tag Game

An Android hide-and-seek game that uses real-world GPS locations. Hiders try to stay out of sight while the seeker gets location pings to track them down.

Built in 36 hours at **Bitcamp 2026** (University of Maryland) by a team of four.

## How it works

- **Location pings:** The app uses Android's location services to sample each hider's position every 5 minutes. Those positions are sent to the seeker as pings through Firebase.
- **Seeker tracker powerup:** A powerup gives hiders live tracking of the seeker for 60 seconds, with location updates every second.
- **Backend:** Firebase handles storing player locations and delivering updates to other players.

## Tech stack

- Android SDK (Java)
- Firebase (Realtime Database)
- Android location services
- Git and GitHub
- Firebase Cloud Functions

AI-assisted development: We used Claude Code during the build.

## Getting started

### Requirements
- Android Studio Panda 3
- An Android device or emulator running Android 9 (API 28) or higher
- A Firebase project of your own (see below)

### Setup
1. Clone the repo:
   ```
   git clone https://github.com/adithyavadivel-umdcs/Bitcamp26.git
   ```
2. Create a Firebase project and add an Android app to it.
3. Download your `google-services.json` and place it in the `app/` folder. This file is not included in the repo.
4. Open the project in Android Studio and let Gradle sync.
5. Run the app on a device or emulator. Location features work best on a real device.

## Playtesting

We playtested with 4 users and adjusted the ping timing to keep location updates accurate.

## Known issues

This was a 36-hour hackathon build, so there are rough edges:

- Location updates sometimes lag when the app is in the background
- Power-ups can be stacked, so the game can be unbalanced at times
- Being caught relies on the honor system.

## What we'd improve next

- Fix the known issues above
- Adjust ping timing based on latency

## Team

- Raghav Patil (backend, Android integration, location tracking)
- Adithya Vadivel (match screen and results)
- Abhiram Kuuram (game logic, Cloud Functions, map)
- Armaan Ivaturi (UI and game flow)
