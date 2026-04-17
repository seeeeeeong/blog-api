# ── GitHub Actions OIDC Provider ──────────────

resource "aws_iam_openid_connect_provider" "github_actions" {
  url             = "https://token.actions.githubusercontent.com"
  client_id_list  = ["sts.amazonaws.com"]
  thumbprint_list = ["6938fd4d98bab03faadb97b34396831e3780aea1"]
}

# ── Shared IAM policy for ECR + SSM deploy ───

locals {
  account_id = data.aws_caller_identity.current.account_id

  # Shared SSM + EC2 deploy statements (used by both roles)
  deploy_policy_statements = [
    {
      Effect   = "Allow"
      Action   = ["ecr:GetAuthorizationToken"]
      Resource = "*"
    },
    {
      Effect = "Allow"
      Action = [
        "ssm:SendCommand",
        "ssm:GetCommandInvocation",
        "ssm:ListCommandInvocations",
      ]
      Resource = [
        "arn:aws:ssm:${var.aws_region}::document/AWS-RunShellScript",
        "arn:aws:ec2:${var.aws_region}:${local.account_id}:instance/*",
      ]
    },
    {
      Effect   = "Allow"
      Action   = ["ssm:GetCommandInvocation"]
      Resource = "arn:aws:ssm:${var.aws_region}:${local.account_id}:*"
    },
    {
      Effect   = "Allow"
      Action   = ["ec2:DescribeInstances"]
      Resource = "*"
    },
  ]
}

# ── blog-api GitHub Actions Role ─────────────

resource "aws_iam_role" "github_actions" {
  name = "${var.project_name}-github-actions"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Effect    = "Allow"
      Principal = { Federated = aws_iam_openid_connect_provider.github_actions.arn }
      Action    = "sts:AssumeRoleWithWebIdentity"
      Condition = {
        StringEquals = {
          "token.actions.githubusercontent.com:aud" = "sts.amazonaws.com"
          "token.actions.githubusercontent.com:sub" = "repo:${var.github_repo}:ref:refs/heads/main"
        }
        StringLike = {
          "token.actions.githubusercontent.com:job_workflow_ref" = "${var.github_repo}/.github/workflows/*@refs/heads/main"
        }
      }
    }]
  })
}

resource "aws_iam_role_policy" "github_actions" {
  name = "${var.project_name}-github-actions-policy"
  role = aws_iam_role.github_actions.id

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = concat(local.deploy_policy_statements, [
      {
        Effect = "Allow"
        Action = [
          "ecr:BatchCheckLayerAvailability",
          "ecr:GetDownloadUrlForLayer",
          "ecr:BatchGetImage",
          "ecr:PutImage",
          "ecr:InitiateLayerUpload",
          "ecr:UploadLayerPart",
          "ecr:CompleteLayerUpload",
        ]
        Resource = aws_ecr_repository.blog_api.arn
      },
    ])
  })
}

# ── devlog-archive GitHub Actions Role ───────

resource "aws_iam_role" "devlog_archive_github_actions" {
  name = "devlog-archive-github-actions"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Effect    = "Allow"
      Principal = { Federated = aws_iam_openid_connect_provider.github_actions.arn }
      Action    = "sts:AssumeRoleWithWebIdentity"
      Condition = {
        StringEquals = {
          "token.actions.githubusercontent.com:aud"              = "sts.amazonaws.com"
          "token.actions.githubusercontent.com:sub"              = "repo:${var.devlog_archive_github_repo}:ref:refs/heads/main"
          "token.actions.githubusercontent.com:job_workflow_ref" = "${var.devlog_archive_github_repo}/.github/workflows/deploy.yml@refs/heads/main"
        }
      }
    }]
  })
}

resource "aws_iam_role_policy" "devlog_archive_github_actions" {
  name = "devlog-archive-github-actions-policy"
  role = aws_iam_role.devlog_archive_github_actions.id

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = concat(local.deploy_policy_statements, [
      {
        Effect = "Allow"
        Action = [
          "ecr:BatchCheckLayerAvailability",
          "ecr:GetDownloadUrlForLayer",
          "ecr:BatchGetImage",
          "ecr:PutImage",
          "ecr:InitiateLayerUpload",
          "ecr:UploadLayerPart",
          "ecr:CompleteLayerUpload",
        ]
        Resource = aws_ecr_repository.devlog_archive.arn
      },
    ])
  })
}
