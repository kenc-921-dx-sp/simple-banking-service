# Docker & Deployment Related
> **Work in progress:** The deployment resources are an initial technical baseline and are not
> production-ready. They may require changes before they run in a specific Kubernetes or OpenShift
> environment because cluster networking, image registries, credentials, security policies,
> storage, Kafka, PostgreSQL, and platform conventions differ between environments.

## 1. Purpose and Background

This document explains how the banking service is packaged as a Docker image, how developers can
run and verify the service locally, and how the resources under `deploy/` are intended to support
an initial Kubernetes or OpenShift deployment.

The deployment configuration is a **work in progress (WIP)**. It demonstrates the expected
container boundary, environment-variable configuration, health probes, security controls, and
service exposure. It is not a production-ready platform definition. Environment-specific
networking, secret management, observability, scaling, availability, and operational policies must
be designed and validated before production use.

The application has the following runtime dependencies:

- Java 21 for compiling and running the Spring Boot application.
- PostgreSQL for customer, account, authorization, and transaction persistence.
- Kafka for incoming bank transaction events and dead-letter publishing.
- Liquibase for applying versioned database schema changes during application startup.
- RSA public key material for validating bearer JWTs.

Spring Boot relaxed binding maps environment variables to application properties. For example,
`SPRING_DATASOURCE_URL` overrides `spring.datasource.url`, while
`BANKING_SERVICE_KAFKA_ENABLED` overrides `banking-service.kafka.enabled`. This allows the same
container image to be promoted between environments without rebuilding it.

## 2. Docker Image Design

The root `Dockerfile` uses a multi-stage build. A build stage compiles the application, and a
separate runtime stage contains only the Java runtime and executable application JAR. Separating
these responsibilities reduces the final image size and prevents Maven, source code, and compiler
tooling from being included in the runtime image.

### 2.1 Build Stage

```dockerfile
FROM eclipse-temurin:21-jdk-jammy AS build
WORKDIR /workspace
```

The build stage uses the Eclipse Temurin Java 21 JDK. The JDK is required because Maven invokes the
Java compiler. `/workspace` is used as the build directory inside the image.

```dockerfile
COPY .mvn .mvn
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw -B -DskipTests dependency:go-offline
```

The Maven Wrapper and `pom.xml` are copied before the source tree. Docker can therefore reuse the
dependency layer when application source files change but Maven configuration does not.
`dependency:go-offline` resolves project dependencies and Maven plugins in advance. `-B` enables
non-interactive batch mode. Tests are not executed while dependencies are being prepared.

```dockerfile
COPY src src
RUN ./mvnw -B -DskipTests package \
    && find target -maxdepth 1 -name "*.jar" \
       ! -name "*.original" -exec cp {} application.jar \;
```

The source is copied after dependencies have been cached. Maven then compiles the application and
creates an executable Spring Boot JAR. Tests are skipped during image construction because they are
expected to run as a separate CI quality gate before the image is published. The packaged artifact
is normalized to `/workspace/application.jar`, keeping the runtime stage independent of the Maven
artifact version.

### 2.2 Runtime Stage

```dockerfile
FROM eclipse-temurin:21-jre-jammy
WORKDIR /application
```

The runtime stage uses the Java 21 JRE rather than the JDK because compilation has already
completed.

```dockerfile
COPY --from=build --chown=1001:0 /workspace/application.jar application.jar
RUN chgrp -R 0 /application && chmod -R g=u /application
```

Only the executable JAR is copied from the build stage. User `1001` owns the artifact, and group
`0` receives the same permissions as the owning user. The group permission is important for
OpenShift, which commonly starts containers with an arbitrary non-root user ID that belongs to
group `0`.

```dockerfile
USER 1001
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "application.jar"]
```

The process runs as a non-root user. Port `8080` documents the expected HTTP port, although actual
port publication is controlled by Docker or the target orchestration platform. The entry point
starts the executable Spring Boot JAR.

### 2.3 Docker Build Context

`.dockerignore` excludes build output, IDE metadata, Git history, documentation, and local testing
tools from the Docker build context. This reduces transferred data, avoids accidental inclusion of
local artifacts, and improves build-cache stability.

Build the image from the repository root:

```bash
docker build -t banking-service:local .
```

The resulting image is tagged `banking-service:local`.

## 3. Local Execution and API Verification

### 3.1 Start PostgreSQL and Kafka

The existing `docker-compose.yml` provides PostgreSQL, pgAdmin, Kafka, ZooKeeper, Kafka UI, and a
Kafka topic initialization container. It does not run the banking service application.

Set a predictable Compose project name and start the supporting services:

```bash
COMPOSE_PROJECT_NAME=banking-service docker compose up -d
COMPOSE_PROJECT_NAME=banking-service docker compose ps
```

Wait until PostgreSQL and Kafka report healthy. The supporting interfaces are:

| Component | Local address |
| --- | --- |
| PostgreSQL | `localhost:5432` |
| pgAdmin | `http://localhost:5050` |
| Kafka | `localhost:9092` |
| Kafka UI | `http://localhost:8081` |

### 3.2 Run the Application Image

For a local container test, connect the application to the Compose network and activate the
`local` Spring profile:

```bash
docker run --rm \
  --name banking-service \
  --network banking-service_default \
  -p 8080:8080 \
  -e SPRING_PROFILES_ACTIVE=local \
  -e SPRING_DATASOURCE_URL=jdbc:postgresql://postgres:5432/banking_db \
  -e SPRING_DATASOURCE_USERNAME=postgres \
  -e SPRING_DATASOURCE_PASSWORD=postgres \
  -e SPRING_KAFKA_BOOTSTRAP_SERVERS=kafka:29092 \
  -e SPRING_KAFKA_CONSUMER_GROUP_ID=banking-service \
  -e BANKING_SERVICE_KAFKA_ENABLED=true \
  banking-service:local
```

The internal Kafka address is `kafka:29092`. Port `9092` is the broker address advertised to
processes running on the host, while containers on the Compose network should use the internal
listener.

Activating `local` has three relevant effects:

- Liquibase runs both the `main` and `test` contexts.
- Local test data is inserted, including customer
  `10000000-0000-0000-0000-000000000001`.
- The development RSA key pair is available for local JWT signing and validation.

These behaviors are development conveniences and must not be enabled in production.

If the Compose network has a different name, identify it with:

```bash
docker network ls
```

Use the matching Compose network in the `docker run --network` argument.

### 3.3 Confirm Application Health

The health endpoints do not require JWT authentication:

```bash
curl http://localhost:8080/actuator/health/liveness
curl http://localhost:8080/actuator/health/readiness
```

Both endpoints should return HTTP `200` with an `UP` status after startup has completed.

### 3.4 Generate a Mock JWT

`DummyJwtGeneratorTest` creates a one-hour RS256 token using the local development private key. The
token includes a `customer_id` claim that Spring Security uses to load the customer and
authorization records from PostgreSQL.

Run the generator for the seeded customer:

```bash
./mvnw \
  -Dtest=DummyJwtGeneratorTest \
  -DcustomerId=10000000-0000-0000-0000-000000000001 \
  test
```

Copy the value printed after `Bearer token:`. The local key pair is test-only key material and must
never be used to issue production credentials.

If the test cannot initialize Mockito because the local JDK prevents dynamic Java agent
attachment, run `DummyJwtGeneratorTest` from the IDE with the same `customerId` system property.

### 3.5 Test the Transaction API with Postman

Create a `GET` request in Postman:

```text
http://localhost:8080/api/v1/transactions
```

Add these query parameters:

| Parameter | Example | Description |
| --- | --- | --- |
| `year` | `2026` | Transaction year |
| `month` | `1` | Transaction month, from 1 to 12; local seed transactions use January 2026 |
| `page` | `0` | Zero-based result page |
| `size` | `20` | Number of records requested |
| `majorDisplayCurrency` | `USD` | Three-letter ISO 4217 display currency |

Under **Authorization**, select **Bearer Token** and enter the generated token. Alternatively, add
the header directly:

```text
Authorization: Bearer <generated-token>
```

Send the request. A successful response returns HTTP `200` and a pageable transaction view for the
authenticated customer. Authentication and authorization failures have distinct meanings:

- HTTP `401`: the token is missing, malformed, expired, signed by an unknown key, or references an
  unknown customer.
- HTTP `403`: the customer is authenticated but does not have the `transactions:view` privilege.
- HTTP `400`: one or more query parameters fail validation.

The same request can be executed without Postman:

```bash
curl \
  -H "Authorization: Bearer <generated-token>" \
  "http://localhost:8080/api/v1/transactions?year=2026&month=1&page=0&size=20&majorDisplayCurrency=USD"
```

To stop and remove the local supporting services and their persisted volumes:

```bash
COMPOSE_PROJECT_NAME=banking-service docker compose down --volumes --remove-orphans
```

## 4. Kubernetes and OpenShift Deployment

### 4.1 Current Deployment Structure

The deployment resources are intentionally separated by responsibility:

| File | Responsibility |
| --- | --- |
| `deploy/kubernetes/configmap.yml` | Non-sensitive Spring, database, Kafka, topic, port, and JVM settings |
| `deploy/kubernetes/secret.example.yml` | Template for database credentials and JWT public key |
| `deploy/kubernetes/deployment.yml` | Pod template, rollout strategy, probes, resources, and security settings |
| `deploy/kubernetes/service.yml` | Cluster-internal HTTP access to healthy application pods |
| `deploy/openshift/route.yml` | OpenShift edge-TLS route to the Kubernetes Service |

The `ConfigMap` and `Secret` are injected with `envFrom`. Spring Boot consumes their keys through
environment-variable property binding. Sensitive values are separated from non-sensitive values,
but the example Secret is only a template and must not be applied unchanged.

### 4.2 Deployment Runtime Behavior

The Deployment requests two replicas and uses a rolling-update strategy with
`maxUnavailable: 0` and `maxSurge: 1`. Kubernetes can create one replacement pod above the desired
replica count and should keep existing pods available while the replacement becomes ready.

Three HTTP probes control pod lifecycle:

- The startup probe allows the application time to initialize Spring, connect to dependencies, and
  execute Liquibase before liveness enforcement begins.
- The liveness probe determines whether the process is operational and should be restarted.
- The readiness probe determines whether the pod may receive traffic through the Service.

The Deployment also defines CPU and memory requests and limits, disables privilege escalation,
drops Linux capabilities, requires non-root execution, uses the runtime-default seccomp profile,
and makes the root filesystem read-only. An `emptyDir` volume provides writable temporary storage
at `/tmp`.

Spring Boot graceful shutdown is enabled. Kubernetes allows 30 seconds for pod termination, while
Spring is configured with a 25-second shutdown phase. This gives the application time to stop
accepting work and complete in-flight requests before the container is terminated.

### 4.3 Preparing and Applying the Resources

Before deployment:

1. Publish the image to a registry accessible from the cluster.
2. Replace `banking-service:latest` in `deployment.yml` with an immutable image reference,
   preferably a version tag or image digest.
3. Update PostgreSQL and Kafka addresses in `configmap.yml` for the target namespace.
4. Create the `banking-service` Secret through an approved secret-management process.
5. Confirm that the database is reachable and that the deployment identity may run Liquibase.
6. Confirm that the Kafka topics exist or that an approved provisioning process creates them.

Example Kubernetes application sequence:

```bash
kubectl apply -f deploy/kubernetes/configmap.yml
kubectl apply -f /secure/path/banking-service-secret.yml
kubectl apply -f deploy/kubernetes/service.yml
kubectl apply -f deploy/kubernetes/deployment.yml
kubectl rollout status deployment/banking-service
```

For OpenShift, apply the Route after the Kubernetes Service exists:

```bash
oc apply -f deploy/openshift/route.yml
oc get route banking-service
```

The Route performs edge TLS termination and redirects insecure HTTP requests to HTTPS.

## 5. Work in Progress and Production Readiness

The current manifests are an implementation baseline and require environment-specific
fine-tuning. A production readiness review should address at least the following areas:

- **Image lifecycle:** immutable tags, vulnerability scanning, software bill of materials,
  signing, registry retention, and controlled promotion between environments.
- **Secrets:** external secret management, encryption at rest, access controls, rotation, and
  removal of private signing keys from the application deployment.
- **Identity validation:** production issuer and audience validation, JWK Set integration, key
  rotation, and trusted identity-provider configuration.
- **Database migrations:** ownership of Liquibase execution, concurrent-start behavior, rollback
  procedures, backup validation, and separation of schema permissions from runtime permissions.
- **Kafka:** authentication, TLS, authorization, topic provisioning, retention policies, retry and
  dead-letter monitoring, and consumer lag alerts.
- **Availability:** pod anti-affinity or topology spread constraints, PodDisruptionBudget,
  multi-zone scheduling, dependency failure behavior, and tested rolling upgrades.
- **Scaling:** load testing, validated CPU and memory values, replica sizing, and Horizontal Pod
  Autoscaler policy where appropriate.
- **Observability:** centralized logs, metrics, traces, dashboards, service-level objectives, and
  alerts for application, database, Kafka, and dead-letter failures.
- **Networking:** namespaces, DNS, ingress or Route standards, NetworkPolicy, egress controls,
  certificates, and production domain names.
- **Security:** least-privilege service accounts, admission policies, supported base-image update
  processes, penetration testing, and organization-specific compliance controls.
- **Operations:** environment overlays, deployment automation, rollback and recovery runbooks,
  ownership, support escalation, and disaster-recovery testing.

The resource requests, limits, probe thresholds, replica count, and termination timings currently
represent sensible starting values only. They must be measured under realistic workloads and
adjusted using evidence from performance, resilience, and operational testing before production
approval.
