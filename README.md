# blog-api

Spring Boot API for the public blog and admin workflows.

## Repository Structure

This repository follows the same skeleton as `blog-ai`.

```text
com.blog.api
├── core
│   ├── api
│   │   ├── config
│   │   └── controller/v1
│   │       ├── request
│   │       └── response
│   ├── domain
│   └── support
│       ├── auth
│       ├── converter
│       ├── error
│       ├── properties
│       ├── response
│       ├── security
│       └── web
└── storage
```

Use [docs/conventions/clean-code.md](docs/conventions/clean-code.md) as the repository-wide refactoring baseline.

## Quality Gates

- `./gradlew test`
- `./gradlew ktlintCheck`
- `./gradlew detekt`

