# =============================================
# ECR Repository
# =============================================
resource "aws_ecr_repository" "blog_api" {
  name                 = "blog-api"
  image_tag_mutability = "IMMUTABLE"

  image_scanning_configuration {
    scan_on_push = true
  }

  tags = { Name = "${var.project_name}-ecr" }
}

resource "aws_ecr_lifecycle_policy" "blog_api" {
  repository = aws_ecr_repository.blog_api.name

  policy = jsonencode({
    rules = [
      {
        rulePriority = 1
        description  = "최근 5개 이미지만 유지"
        selection = {
          tagStatus   = "any"
          countType   = "imageCountMoreThan"
          countNumber = 5
        }
        action = {
          type = "expire"
        }
      }
    ]
  })
}
