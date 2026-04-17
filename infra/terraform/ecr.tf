# ── ECR Repositories ─────────────────────────

locals {
  ecr_repos = {
    blog_api        = "blog-api"
    devlog_archive  = "devlog-archive"
  }

  ecr_lifecycle_policy = jsonencode({
    rules = [{
      rulePriority = 1
      description  = "Keep last 5 images"
      selection = {
        tagStatus   = "any"
        countType   = "imageCountMoreThan"
        countNumber = 5
      }
      action = { type = "expire" }
    }]
  })
}

resource "aws_ecr_repository" "blog_api" {
  name                 = local.ecr_repos.blog_api
  image_tag_mutability = "IMMUTABLE"

  image_scanning_configuration { scan_on_push = true }

  tags = { Name = "${var.project_name}-ecr" }
}

resource "aws_ecr_lifecycle_policy" "blog_api" {
  repository = aws_ecr_repository.blog_api.name
  policy     = local.ecr_lifecycle_policy
}

resource "aws_ecr_repository" "devlog_archive" {
  name                 = local.ecr_repos.devlog_archive
  image_tag_mutability = "IMMUTABLE"

  image_scanning_configuration { scan_on_push = true }

  tags = { Name = "devlog-archive-ecr" }
}

resource "aws_ecr_lifecycle_policy" "devlog_archive" {
  repository = aws_ecr_repository.devlog_archive.name
  policy     = local.ecr_lifecycle_policy
}
