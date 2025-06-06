# Desktop Google OAuth2 Implementation

This document describes the Google OAuth2 implementation for the desktop application.

## Overview

The desktop implementation uses the OAuth2 Authorization Code flow with PKCE (Proof Key for Code Exchange) for enhanced security. The flow involves:

1. **Local HTTP Server**: A temporary HTTP server is started on `localhost:8080` to handle OAuth callbacks
2. **Browser Authentication**: The default browser opens to Google's OAuth2 authorization page
3. **Authorization Code Exchange**: The received authorization code is exchanged for an ID token
4. **Token Storage**: The ID token is securely stored in the user's home directory

## Architecture

### Key Components

- **`DesktopTokenProvider`**: Main implementation of the `TokenProvider` interface
- **Local HTTP Server**: Handles OAuth2 callbacks from Google
- **Ktor HTTP Client**: Used for token exchange requests
- **PKCE Security**: Implements Proof Key for Code Exchange for enhanced security

### Security Features

- **PKCE (Proof Key for Code Exchange)**: Protects against authorization code interception attacks
- **State Parameter**: Prevents CSRF attacks by validating state parameter
- **Secure Random Generation**: Uses `SecureRandom` for generating state and code verifier
- **Local File Storage**: Tokens are stored in user's home directory (`.demo_app_token`)

## OAuth2 Flow Details

### 1. Authorization Request

The application builds an authorization URL with the following parameters:

```
https://accounts.google.com/o/oauth2/v2/auth?
  client_id=YOUR_CLIENT_ID&
  redirect_uri=http://localhost:DYNAMIC_PORT/callback&
  response_type=code&
  scope=openid email profile&
  state=RANDOM_STATE&
  code_challenge=CODE_CHALLENGE&
  code_challenge_method=S256&
  access_type=offline&
  prompt=consent
```

Note: The redirect URI uses a dynamically assigned port to avoid conflicts.

### 2. Local Server Setup

A temporary HTTP server is created on an available port to handle the OAuth callback:

```kotlin
val server = HttpServer.create(InetSocketAddress("localhost", 0), 0)
val actualPort = server.address.port
redirectUri = "http://localhost:$actualPort/callback"
server.createContext("/callback") { exchange ->
    handleOAuthCallback(exchange, authCodeFuture, state)
}
```

### 3. Token Exchange

Once the authorization code is received, it's exchanged for tokens:

```kotlin
val response = httpClient.submitForm(
    url = "https://oauth2.googleapis.com/token",
    formParameters = parameters {
        append("client_id", appConfig.googleClientId)
        append("code", authorizationCode)
        append("code_verifier", codeVerifier)
        append("grant_type", "authorization_code")
        append("redirect_uri", redirectUri)
    }
)
```

### 4. Token Storage

The received ID token is stored in a file in the user's home directory:

```kotlin
private val tokenFile = File(System.getProperty("user.home"), ".demo_app_token")
```

## Configuration

### Google Cloud Console Setup

1. Create a new OAuth2 client ID with application type "Desktop application"
2. **Important**: Add `http://localhost` as an authorized redirect URI (without specifying a port)
3. Note the client ID for configuration

**Redirect URI Configuration**: Desktop applications should use `http://localhost` as the redirect URI in Google Cloud Console. The application will dynamically assign an available port at runtime.

### Environment Configuration

Set the Google client ID in your environment:

```bash
# Environment variable
export GOOGLE_CLIENT_ID="your-client-id.apps.googleusercontent.com"

# Or in local.properties
GOOGLE_CLIENT_ID=your-client-id.apps.googleusercontent.com
```

## Usage

The `DesktopTokenProvider` is automatically injected via Koin dependency injection:

```kotlin
val desktopModule = module {
    single<TokenProvider> { DesktopTokenProvider(get<AppConfig>()) }
}
```

### Token Operations

```kotlin
// Get current token
val token = tokenProvider.getToken()

// Refresh token (triggers OAuth2 flow)
val newToken = tokenProvider.refreshToken()

// Clear stored token
tokenProvider.clearToken()
```

## Error Handling

The implementation handles various error scenarios:

- **Browser not available**: Falls back to printing the URL to console
- **OAuth errors**: Displays error page in browser and logs the error
- **Network errors**: Logs errors and returns null
- **State mismatch**: Prevents CSRF attacks by validating state parameter

## Browser Integration

The implementation automatically opens the default browser:

```kotlin
if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
    Desktop.getDesktop().browse(URI(authUrl))
} else {
    println("Please open this URL in your browser: $authUrl")
}
```

## Callback Handling

The local HTTP server provides user-friendly feedback:

- **Success Page**: Confirms successful authentication
- **Error Page**: Displays error information
- **Automatic Cleanup**: Server is automatically stopped after callback

## Dependencies

The desktop OAuth2 implementation uses:

- **Ktor Client CIO**: For HTTP requests
- **Ktor Content Negotiation**: For JSON serialization
- **Kotlinx Serialization**: For parsing token responses
- **Java HTTP Server**: For local callback handling
- **Java Desktop API**: For browser integration

## Testing

To test the OAuth2 flow:

1. Set the `GOOGLE_CLIENT_ID` environment variable
2. Run the desktop application
3. Trigger authentication (e.g., by calling `refreshToken()`)
4. Browser should open to Google's OAuth page
5. After authentication, check that token is stored in `~/.demo_app_token`

## Troubleshooting

### Common Issues

1. **redirect_uri_mismatch**: Ensure `http://localhost` is configured as an authorized redirect URI in Google Cloud Console (without specifying a port).
2. **Browser doesn't open**: Check if Desktop API is supported on your platform.
3. **Client ID not configured**: Ensure `GOOGLE_CLIENT_ID` is set correctly.
4. **Token file permissions**: Ensure the application can write to the user's home directory.
5. **Port conflicts**: The application now uses dynamic port assignment to avoid conflicts.

### Logging

The implementation provides detailed logging through the `Logger.network` logger:

```kotlin
Logger.network.d("DesktopTokenProvider: Starting OAuth2 flow")
Logger.network.d("DesktopTokenProvider: OAuth2 flow successful")
```

Enable debug logging to troubleshoot issues.