# ── Infrastructure ───────────────────────────

output "ec2_public_ip" {
  description = "EC2 Elastic IP"
  value       = aws_eip.main.public_ip
}

output "ec2_instance_id" {
  description = "EC2 instance ID"
  value       = aws_instance.main.id
}

output "rds_endpoint" {
  description = "RDS endpoint"
  value       = aws_db_instance.main.address
}

output "rds_port" {
  description = "RDS port"
  value       = aws_db_instance.main.port
}

# ── Storage ──────────────────────────────────

output "image_bucket_name" {
  description = "Image S3 bucket"
  value       = aws_s3_bucket.images.bucket
}

output "frontend_bucket_name" {
  description = "Frontend S3 bucket"
  value       = aws_s3_bucket.frontend.bucket
}

output "ecr_registry_url" {
  description = "blog-api ECR URL"
  value       = aws_ecr_repository.blog_api.repository_url
}

# ── CDN ──────────────────────────────────────

output "image_cdn_domain" {
  description = "Image CloudFront domain"
  value       = aws_cloudfront_distribution.images.domain_name
}

output "frontend_cdn_domain" {
  description = "Frontend CloudFront domain"
  value       = aws_cloudfront_distribution.frontend.domain_name
}

output "frontend_distribution_id" {
  description = "Frontend CloudFront distribution ID"
  value       = aws_cloudfront_distribution.frontend.id
}

output "api_cdn_domain" {
  description = "API CloudFront domain"
  value       = aws_cloudfront_distribution.api.domain_name
}

# ── Application Config Values ────────────────

output "api_base_url" {
  description = "VITE_API_BASE_URL for blog-web"
  value       = local.use_custom_cloudfront_domain ? "https://api.${var.domain_name}/api" : "https://${aws_cloudfront_distribution.api.domain_name}/api"
}

output "frontend_base_url" {
  description = "Frontend base URL"
  value       = local.use_custom_cloudfront_domain ? "https://${var.domain_name}" : "https://${aws_cloudfront_distribution.frontend.domain_name}"
}

output "cors_allowed_origins" {
  description = "CORS_ALLOWED_ORIGINS for blog-api"
  value       = local.use_custom_cloudfront_domain ? "https://${var.domain_name},https://www.${var.domain_name}" : "https://${aws_cloudfront_distribution.frontend.domain_name}"
}

# ── CI/CD ────────────────────────────────────

output "github_actions_role_arn" {
  description = "blog-api GitHub Actions OIDC role ARN"
  value       = aws_iam_role.github_actions.arn
}

output "ssm_session_command" {
  description = "SSM Session Manager connect command"
  value       = "aws ssm start-session --target ${aws_instance.main.id} --region ${var.aws_region}"
}

# ── blog-ai ──────────────────────────────────

output "blog_ai_instance_id" {
  description = "blog-ai EC2 instance ID"
  value       = aws_instance.blog_ai.id
}

output "blog_ai_private_ip" {
  description = "blog-ai private IP (blog-api reaches it here)"
  value       = aws_instance.blog_ai.private_ip
}

output "blog_ai_ecr_registry_url" {
  description = "blog-ai ECR URL"
  value       = aws_ecr_repository.blog_ai.repository_url
}

output "blog_ai_github_actions_role_arn" {
  description = "blog-ai GitHub Actions OIDC role ARN"
  value       = aws_iam_role.blog_ai_github_actions.arn
}

output "blog_ai_ssm_session_command" {
  description = "SSM Session Manager connect command for blog-ai"
  value       = "aws ssm start-session --target ${aws_instance.blog_ai.id} --region ${var.aws_region}"
}

# ── Monitoring ───────────────────────────────

output "monitoring_sns_topic_arn" {
  description = "SNS topic ARN (null if email not configured)"
  value       = try(aws_sns_topic.monitoring_alerts[0].arn, null)
}
