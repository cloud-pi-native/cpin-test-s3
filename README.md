# s3cli

Simple Spring Boot CLI application that uploads a file to S3.

Prerequisites
- Java 17+
- Maven
- AWS credentials available via environment variables (AWS_ACCESS_KEY_ID, AWS_SECRET_ACCESS_KEY) or AWS profile (~/.aws/credentials)

Build

Open PowerShell in the project root and run:

```powershell
mvn -DskipTests package
```

Run

You can set the bucket and other properties in `src/main/resources/application.properties` or via environment variables. Spring Boot's relaxed binding maps environment variables like `APP_S3_BUCKET` -> `app.s3.bucket` and `APP_S3_ENDPOINT` -> `app.s3.endpoint`.

Example using PowerShell with environment variables for AWS credentials and app properties:

```powershell
# Set AWS credentials (session will inherit these)
$env:AWS_ACCESS_KEY_ID = 'AKIA...'
$env:AWS_SECRET_ACCESS_KEY = 'SECRET'

# Set the target bucket and optional endpoint (for MinIO/local S3-compatible)
$env:APP_S3_BUCKET = 'my-bucket'
$env:APP_S3_ENDPOINT = 'http://localhost:9000'  # optional

# Build
mvn -DskipTests package

# Run the fat jar and upload a file (first arg = local path, second arg optional = s3 key)
java -jar target/s3cli-0.0.1-SNAPSHOT.jar C:\path\to\file.txt optional/s3/key.txt
```

Notes
- The app uses the AWS SDK v2 and Spring Boot starter. For local S3-compatible storage like MinIO, set `app.s3.endpoint` in `application.properties`.
