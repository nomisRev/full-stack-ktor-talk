# Kotlin Multiplatform Demo with Google Authentication

This is a Kotlin Multiplatform project targeting Android and Server with Google Sign-In authentication and real-time chat functionality.

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

* `/docs` contains project documentation and recommendations for improvements.

## Key Features

* **Cross-Platform UI**: Shared UI implementation using Compose Multiplatform
* **Google Authentication**: OAuth2 implementation for both Android and Desktop
* **Real-time Chat**: Server-Sent Events (SSE) for streaming chat responses
* **Secure Token Management**: Encrypted storage and automatic token refresh
* **AI Integration**: OpenAI-powered chat responses

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

### Database Setup

The server requires a PostgreSQL database. You can use the provided Docker Compose file:

```bash
cd docker
docker-compose up -d
```

This will start a PostgreSQL instance with the correct configuration.

## HTTP Client with Authentication

The project now includes a fully configured Ktor HttpClient with automatic authentication and token refresh:

### Features

- **Automatic Token Loading**: Integrates with `TokenStorage` to automatically load authentication tokens
- **Token Refresh**: Automatically refreshes Google ID tokens when they become invalid
- **Bearer Authentication**: Uses Ktor's Auth plugin with Bearer token authentication
- **Content Negotiation**: Configured for JSON serialization/deserialization
- **Logging**: Comprehensive HTTP request/response logging

### Token Refresh Flow

1. HTTP request is made with current token from `TokenStorage`
2. If server returns 401 (Unauthorized), Ktor Auth plugin triggers token refresh
3. `TokenProvider.refreshToken()` gets a new Google ID token
4. New token is saved to `TokenStorage`
5. Original request is retried with the new token

Learn more about [Kotlin Multiplatform](https://www.jetbrains.com/help/kotlin-multiplatform-dev/get-started.html)…
