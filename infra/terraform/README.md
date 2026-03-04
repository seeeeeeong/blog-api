# Terraform Runbook

## 1) Backend bootstrap (one-time per AWS account)

```bash
bash infra/bootstrap/bootstrap.sh
```

The script creates:
- S3 state bucket: `blog-terraform-state-<account-id>`
- DynamoDB lock table: `terraform-state-lock`
- local backend file: `infra/terraform/backend.hcl` (gitignored)

## 2) Terraform init (migrate local state -> S3)

```bash
terraform -chdir=infra/terraform init -reconfigure -backend-config=backend.hcl -migrate-state
```

If this is a fresh workspace with no local state, you can omit `-migrate-state`.

## 3) Plan / Apply

```bash
terraform -chdir=infra/terraform plan
terraform -chdir=infra/terraform apply
```

## 4) Formatting

```bash
terraform -chdir=infra/terraform fmt
terraform -chdir=infra/terraform fmt -check
```

## 5) Optional: use the example backend file

```bash
cp infra/terraform/backend.hcl.example infra/terraform/backend.hcl
```

Then edit `bucket`, `region`, and `dynamodb_table` before running `init`.

## 6) SSM Parameters for Deploy Runtime

`deploy.yml` now injects app config at runtime from SSM Parameter Store.

Prefix example: `/blog/prod`

Required keys:
- `/blog/prod/DB_PASSWORD`
- `/blog/prod/JWT_SECRET`
- `/blog/prod/GITHUB_CLIENT_ID`
- `/blog/prod/GITHUB_CLIENT_SECRET`
- `/blog/prod/CORS_ALLOWED_ORIGINS`
- `/blog/prod/OAUTH_REDIRECT_URL`
- `/blog/prod/OAUTH_CALLBACK_URL`
- `/blog/prod/AWS_S3_BUCKET`
- `/blog/prod/AWS_CLOUDFRONT_DOMAIN`
- `/blog/prod/CLOUDFRONT_SECRET`

Optional keys:
- `/blog/prod/SPRING_JPA_HIBERNATE_DDL_AUTO`
- `/blog/prod/JWT_ACCESS_EXPIRATION`
- `/blog/prod/JWT_REFRESH_EXPIRATION`
- `/blog/prod/AWS_REGION`
- `/blog/prod/BACKUP_S3_PREFIX`
