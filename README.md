# Full Stack Ktor Talk

This repository contains an example project that was used for Full Stack talk:
The slides can be found here [Full stack Kotlin with Ktor.pdf](Full%20stack%20Kotlin%20with%20Ktor.pdf).

## Kotlin Multiplatform Demo with Google Authentication

This is a Kotlin Multiplatform project focusing on Android and Server with Google Sign-In authentication and real-time chat functionality.

* `/composeApp` is for code that will be shared across your Compose Multiplatform applications.
  It contains several subfolders:
    - `commonMain` is for code that’s common for all targets.
    - Other folders are for Kotlin code that will be compiled for only the platform indicated in the folder name.
      For example, if you want to use Apple’s CoreCrypto for the iOS part of your Kotlin app,
      `iosMain` would be the right folder for such calls.

* `/server` is for the Ktor server application that provides:
    - JWT authentication with Google Sign-In
    - User management API
    - Real-time chat using Server-Sent Events (SSE)
    - Integration with OpenAI for chat responses

* `/shared` is for the code that will be shared between all targets in the project.
  The most important subfolder is `commonMain`. If preferred, you can add code to the platform-specific folders here
  too.

## Key Features

* **Cross-Platform UI**: Shared UI implementation using Compose Multiplatform
* **Google Authentication**: OAuth2 implementation for both Android and Desktop
* **Streaming Responses**: Server-Sent Events (SSE) used for real-time streaming
* **Secure Token Management**: Encrypted storage and automatic token refresh
* **AI Integration**: OpenAI-powered responses
* **Travel Agent Planner**: Authenticated SSE endpoint at `POST /plan` that plans trips using tools (Maps, Weather)

## Google Sign-In Setup

**IMPORTANT**: The current `google-services.json` file contains placeholder values. To enable Google Sign-In:

1. Go to [Google Cloud Console](https://console.cloud.google.com/)
2. Create a new project or select an existing one
3. Enable the Google Sign-In API
4. Set up OAuth consent screen
5. Create OAuth client ID credentials:
   - **Android client**: For the Android app (package: `org.jetbrains.demo`)
   - **Desktop client**: For the desktop app (application type: "Desktop application")
   - **Web client**: For server-side token verification
6. Download the real `google-services.json` file and replace the placeholder file in `/composeApp/`
7. Update the `GOOGLE_CLIENT_ID` in your environment variables or `local.properties` with your actual client ID

### Desktop OAuth2 Setup

For the desktop application, you need to:

1. Create a **Desktop application** OAuth2 client in Google Cloud Console
2. **Important**: Add `http://localhost` as an authorized redirect URI (without specifying a port)
3. Set the client ID in your environment:
   ```bash
   export GOOGLE_CLIENT_ID="your-desktop-client-id.apps.googleusercontent.com"
   ```
   Or add it to `local.properties`:
   ```
   GOOGLE_CLIENT_ID=your-desktop-client-id.apps.googleusercontent.com
   ```
4. The desktop app will automatically open a browser for authentication and handle the OAuth2 flow

## Development Setup

### Server Connection Configuration

For Android development, ensure the `API_BASE_URL` in `local.properties` is correctly configured:

- **Android Emulator**: Use `http://10.0.2.2:8080` (10.0.2.2 is the special IP that maps to host machine's localhost)
- **Physical Device with USB**: Use `http://localhost:8080` with ADB port forwarding: `adb reverse tcp:8080 tcp:8080`
- **Physical Device on Network**: Use `http://[your-machine-ip]:8080` where [your-machine-ip] is your development machine's IP address

Example `local.properties`:
```
API_BASE_URL=http://10.0.2.2:8080
GOOGLE_CLIENT_ID=your-google-client-id
```

### Setting up local server environment

The server requires a PostgreSQL database, and MCP Servers. You can use the provided Docker Compose file:

```bash
cd docker
docker-compose --env-file ./path/to/.env up -d
```

Where the `.env` file contains the following:

```env
GOOGLE_MAPS_API_KEY=your-api-key
```

This will start a PostgreSQL, and Google Maps MCP instance with the correct configuration.

## HTTP Client with Authentication

The project now includes a fully configured Ktor HttpClient with automatic authentication and token refresh:

### Features

- **Automatic Token Loading**: Integrates with `TokenStorage` to automatically load authentication tokens
- **Token Refresh**: Automatically refreshes Google ID tokens when they become invalid
- **Bearer Authentication**: Uses Ktor's Auth plugin with Bearer token authentication
- **Content Negotiation**: Configured for JSON serialization/deserialization
- **Logging**: Comprehensive HTTP request/response logging
