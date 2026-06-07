# JWT Authentication

## Overview

The banking service operates as an OAuth 2.0 resource server. API clients authenticate by
supplying an RSA-signed JSON Web Token in the HTTP `Authorization` header.

The local implementation uses RS256. The private RSA key signs development tokens, while the
corresponding public key verifies tokens received by the API. The configured key pair is intended
exclusively for local development and must not be reused in production.

## Key Configuration

`JwtConfiguration` binds the public and private PEM values under:

```yaml
banking-service:
  security:
    jwt:
      public-key: |
        -----BEGIN PUBLIC KEY-----
        ...
        -----END PUBLIC KEY-----
      private-key: |
        -----BEGIN PRIVATE KEY-----
        ...
        -----END PRIVATE KEY-----
```

The public key uses the X.509 SubjectPublicKeyInfo format. The private key uses PKCS#8.
`JwtSecurityConfiguration` converts the PEM values into RSA key objects and creates:

- A `NimbusJwtDecoder` for validating API bearer tokens.
- A local-profile `NimbusJwtEncoder` for development signing scenarios.

## Token Claims

A local token contains:

- `sub`: Customer UUID.
- `customer_id`: Customer UUID used to load the authenticated customer.
- `iat`: Token issue time.
- `exp`: Token expiration time.
- `iss`: `banking-service-local`.

Roles and privileges are not trusted from JWT claims. Authorization data is loaded from
PostgreSQL after signature validation.

## Authentication Flow

1. The client sends `Authorization: Bearer <token>`.
2. Spring Security extracts the bearer token.
3. `NimbusJwtDecoder` verifies the RS256 signature and validates standard timestamp claims.
4. `CustomerJwtConverter` reads the required `customer_id` claim.
5. `HttpUserDetailsService` loads the customer, roles, and nested privileges from PostgreSQL.
6. Privilege names are converted into `SimpleGrantedAuthority` instances.
7. Spring stores `CustomerUserDetails` as the authenticated principal.
8. Method security evaluates the endpoint's `@PreAuthorize` expression.

The principal can be injected with `@AuthenticationPrincipal` or accessed directly:

```java
CustomerUserDetails customer =
    (CustomerUserDetails)
        SecurityContextHolder.getContext().getAuthentication().getPrincipal();
```

## Authorization

The transaction endpoint requires:

```java
@PreAuthorize("hasAuthority('transactions:view')")
```

The authority is loaded from `customer_privilege` through the customer's role associations.
Possession of a valid JWT does not by itself grant access to the endpoint.

## Dummy Token Generation

Run `DummyJwtGeneratorTest` directly from the IDE. Supply the UUID of an existing database
customer as a JVM system property:

```text
-DcustomerId=<existing-customer-uuid>
```

The test uses Mockito to provide the local PEM key pair to `JwtConfiguration`, signs a one-hour
token, verifies it with the matching public key, and prints the bearer token to the test output.

If the system property is omitted, the generator uses
`00000000-0000-0000-0000-000000000001`. That UUID must exist in the database for an API request
to authenticate successfully.

## API Invocation

```bash
curl \
  -H "Authorization: Bearer <generated-token>" \
  "http://localhost:8080/api/v1/transactions?year=2026&month=6&page=0&size=20&majorDisplayCurrency=USD"
```

## Failure Behavior

- Missing, malformed, expired, or incorrectly signed token: HTTP 401.
- Missing or unknown `customer_id`: HTTP 401.
- Authenticated customer without `transactions:view`: HTTP 403.
- Authorized customer: request proceeds to the transaction controller.

## Production Considerations

Production environments should use an external identity provider and managed secret storage.
Private signing keys must not be committed to application configuration. Production deployments
should also validate issuer and audience claims, support key rotation, and use a JWK Set endpoint
where appropriate.
