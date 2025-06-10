# Ktor Server with JWT Authentication

This server implements JWT authentication using Auth0 JWKProvider for Google issuer verification.

## Features

- **JWT Authentication**: Validates Google ID tokens using Auth0's JWKProvider
- **Google Issuer Support**: Configured to work with Google's OAuth2 tokens
- **Protected Routes**: Example endpoints that require valid JWT authentication
- **Caching & Rate Limiting**: JWK provider includes caching and rate limiting for performance

## Architecture

### Authentication Configuration (`JwtConfig.kt`)

The JWT authentication is configured with:
- **JWK Provider**: Uses Auth0's JWKProvider to fetch Google's public keys from `https://www.googleapis.com/oauth2/v3/certs`
- **Caching**: JWKs are cached for 24 hours (max 10 entries) to reduce external calls
- **Rate Limiting**: Limited to 10 requests per minute to Google's JWKS endpoint
- **Token Validation**: Verifies issuer (`https://accounts.google.com`), signature, and required claims

### Routes (`AuthRoutes.kt`)

#### Public Routes
- `POST /auth/google`: Accepts Google ID tokens for authentication

#### Protected Routes (require valid JWT)
- `GET /protected`: Simple protected endpoint returning user info
- `GET /user/profile`: Returns detailed user profile from JWT claims

## Dependencies

The following dependencies are added to support JWT authentication:

```kotlin
implementation(ktor.serverAuth)
implementation(ktor.serverAuthJwt)
implementation(libs.auth0.java.jwt)
implementation(libs.auth0.jwks.rsa)
```

## Usage

### Starting the Server

```bash
./gradlew :server:run
```

The server will start on port 8080.

### Testing Authentication

1. **Send a Google ID Token**:
```bash
curl -X POST http://localhost:8080/auth/google \
  -H "Content-Type: application/json" \
  -d '{"idToken": "your-google-id-token"}'
```

2. **Access Protected Endpoint**:
```bash
curl -X GET http://localhost:8080/protected \
  -H "Authorization: Bearer your-google-id-token"
```

3. **Get User Profile**:
```bash
curl -X GET http://localhost:8080/user/profile \
  -H "Authorization: Bearer your-google-id-token"
```

## JWT Token Validation

The server validates JWT tokens by:

1. **Signature Verification**: Uses Google's public keys from JWKS endpoint
2. **Issuer Check**: Ensures token is issued by `https://accounts.google.com`
3. **Claims Validation**: Verifies required claims (subject, email, email_verified)
4. **Expiration**: Automatically handled by Auth0 JWT library

## Security Features

- **Key Rotation**: Automatically handles Google's key rotation via JWKS
- **Clock Skew**: Accepts 3 seconds leeway for clock differences
- **Rate Limiting**: Prevents abuse of JWKS endpoint
- **Caching**: Reduces external dependencies and improves performance

## Testing

Run the test suite:

```bash
./gradlew :server:test
```

Tests include:
- Authentication endpoint functionality
- Protected route access control
- Invalid token handling

## Configuration

The JWT authentication can be customized by modifying `JwtConfig.kt`:

- **Cache Duration**: Adjust JWK cache time
- **Rate Limits**: Modify request limits to JWKS endpoint
- **Issuer**: Change to support different OAuth providers
- **Claims**: Add additional claim validation as needed

## Integration with Client

This server is designed to work with the existing `GoogleAuthManager` in the `composeApp` module, which sends Google ID tokens to the `/auth/google` endpoint.