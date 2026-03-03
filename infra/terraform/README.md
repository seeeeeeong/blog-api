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
