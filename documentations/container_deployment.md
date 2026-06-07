# Container Deployment

## Build the Image

The root `Dockerfile` uses Java 21 in a multi-stage build. Maven compiles the executable Spring
Boot JAR in the build stage, and only the resulting application artifact is copied into the runtime
image.

```bash
docker build -t banking-service:local .
```

The runtime container:

- Runs as non-root user `1001`.
- Grants group `0` equivalent access to `/application` for compatibility with OpenShift's
  arbitrary user ID model.
- Exposes port `8080`.
- Accepts Spring configuration through environment variables.

## Local Container Configuration

Create a local `.env` from `.env.example` and replace every placeholder. The `.env` file is ignored
by Git.

For Docker env-file compatibility, encode the JWT public key on one line:

```text
BANKING_SERVICE_SECURITY_JWT_PUBLIC_KEY=-----BEGIN PUBLIC KEY-----MIIB...-----END PUBLIC KEY-----
```

The application removes PEM headers and whitespace before decoding the key, so the compact form is
accepted.

Run the image:

```bash
docker run --rm \
  --name banking-service \
  -p 8080:8080 \
  --env-file .env \
  banking-service:local
```

The configured PostgreSQL and Kafka hostnames must resolve from the container network. When using
the existing Docker Compose infrastructure, attach the application container to the Compose
network or replace the hostnames with addresses reachable from the container.

## Environment Variables

| Variable | Purpose | Sensitive |
| --- | --- | --- |
| `SERVER_PORT` | HTTP listener port | No |
| `SPRING_DATASOURCE_URL` | PostgreSQL JDBC URL | No |
| `SPRING_DATASOURCE_USERNAME` | PostgreSQL username | Yes |
| `SPRING_DATASOURCE_PASSWORD` | PostgreSQL password | Yes |
| `SPRING_KAFKA_BOOTSTRAP_SERVERS` | Kafka bootstrap servers | No |
| `SPRING_KAFKA_CONSUMER_GROUP_ID` | Kafka consumer group | No |
| `BANKING_SERVICE_SECURITY_JWT_PUBLIC_KEY` | JWT verification public key | Yes |
| `BANKING_SERVICE_TRANSACTIONS_KAFKA_TOPIC` | Incoming transaction topic | No |
| `BANKING_SERVICE_TRANSACTIONS_KAFKA_DEAD_LETTER_TOPIC` | Rejected transaction topic | No |
| `BANKING_SERVICE_TRANSACTIONS_SYSTEM_DEFAULT_MAJOR_DISPLAY_CURRENCY` | Default display currency | No |
| `BANKING_SERVICE_KAFKA_ENABLED` | Enables Kafka listener infrastructure | No |
| `JAVA_TOOL_OPTIONS` | JVM container memory settings | No |

Only the JWT public key is required outside the `local` profile. The private key and JWT encoder
are local-profile components used for development token generation.

## Kubernetes

The Kubernetes resources are under `deploy/kubernetes`:

- `configmap.yml` contains non-sensitive application configuration.
- `secret.example.yml` is a template for credentials and JWT key material.
- `deployment.yml` defines the application workload.
- `service.yml` exposes the pods through a cluster-internal service.

Before applying the resources:

1. Replace `banking-service:latest` in `deployment.yml` with an image available to the cluster.
2. Copy `secret.example.yml` to a secure, untracked location.
3. Replace all example secret values.
4. Update database and Kafka addresses for the target namespace.

Apply the resources:

```bash
kubectl apply -f deploy/kubernetes/configmap.yml
kubectl apply -f /secure/path/banking-service-secret.yml
kubectl apply -f deploy/kubernetes/service.yml
kubectl apply -f deploy/kubernetes/deployment.yml
```

The Deployment uses:

- Two replicas and a rolling-update strategy.
- Non-root execution with privilege escalation disabled.
- CPU and memory requests and limits.
- A read-only root filesystem with a writable ephemeral `/tmp`.
- Startup, liveness, and readiness probes.
- A 30-second pod termination grace period aligned with Spring graceful shutdown.

Health endpoints:

```text
/actuator/health/liveness
/actuator/health/readiness
```

These endpoints are deliberately accessible without JWT authentication so the kubelet can perform
health checks.

## OpenShift

After applying the Kubernetes resources, expose the service with:

```bash
oc apply -f deploy/openshift/route.yml
```

The Route terminates TLS at the OpenShift router and redirects insecure HTTP traffic to HTTPS.
OpenShift may replace the image's user ID with an arbitrary non-root ID. The image permissions are
configured so that a process in group `0` can access the application directory.
