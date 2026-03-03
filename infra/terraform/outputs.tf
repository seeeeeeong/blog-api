output "ec2_public_ip" {
  description = "EC2 Elastic IP (API 서버 주소)"
  value       = aws_eip.main.public_ip
}

output "image_bucket_name" {
  description = "이미지 S3 버킷 이름"
  value       = aws_s3_bucket.images.bucket
}

output "frontend_bucket_name" {
  description = "프론트엔드 S3 버킷 이름"
  value       = aws_s3_bucket.frontend.bucket
}

output "image_cdn_domain" {
  description = "이미지 CloudFront 도메인"
  value       = aws_cloudfront_distribution.images.domain_name
}

output "frontend_cdn_domain" {
  description = "프론트엔드 CloudFront 도메인"
  value       = aws_cloudfront_distribution.frontend.domain_name
}

output "frontend_distribution_id" {
  description = "프론트엔드 CloudFront Distribution ID"
  value       = aws_cloudfront_distribution.frontend.id
}

output "api_cdn_domain" {
  description = "API CloudFront 도메인 (HTTPS)"
  value       = aws_cloudfront_distribution.api.domain_name
}

output "api_base_url" {
  description = "blog-web VITE_API_BASE_URL 값"
  value       = "https://${aws_cloudfront_distribution.api.domain_name}/api"
}

output "frontend_base_url" {
  description = "프론트엔드 기본 URL"
  value       = "https://${aws_cloudfront_distribution.frontend.domain_name}"
}

output "api_env_cors_allowed_origins" {
  description = "blog-api CORS_ALLOWED_ORIGINS 권장값"
  value       = "https://${aws_cloudfront_distribution.frontend.domain_name}"
}

output "api_env_oauth_redirect_url" {
  description = "blog-api OAUTH_REDIRECT_URL 권장값"
  value       = "https://${aws_cloudfront_distribution.frontend.domain_name}/auth/callback"
}

output "api_env_oauth_callback_url" {
  description = "blog-api OAUTH_CALLBACK_URL 권장값"
  value       = "https://${aws_cloudfront_distribution.api.domain_name}/api/auth/github/callback"
}

output "api_env_aws_s3_bucket" {
  description = "blog-api AWS_S3_BUCKET 값"
  value       = aws_s3_bucket.images.bucket
}

output "api_env_aws_cloudfront_domain" {
  description = "blog-api AWS_CLOUDFRONT_DOMAIN 값"
  value       = aws_cloudfront_distribution.images.domain_name
}

output "github_actions_role_arn" {
  description = "GitHub Actions OIDC Role ARN (GitHub Secret: AWS_ROLE_ARN)"
  value       = aws_iam_role.github_actions.arn
}

output "ec2_instance_id" {
  description = "EC2 인스턴스 ID (GitHub Secret: EC2_INSTANCE_ID)"
  value       = aws_instance.main.id
}

output "ssm_session_command" {
  description = "SSM Session Manager 접속 명령어"
  value       = "aws ssm start-session --target ${aws_instance.main.id} --region ${var.aws_region}"
}

output "cloudwatch_alarm_names" {
  description = "생성된 CloudWatch 알람 이름"
  value = [
    aws_cloudwatch_metric_alarm.ec2_cpu_high.alarm_name,
    aws_cloudwatch_metric_alarm.ec2_status_check_failed.alarm_name
  ]
}

output "monitoring_sns_topic_arn" {
  description = "CloudWatch 알림 SNS 토픽 ARN (이메일 미사용 시 null)"
  value       = try(aws_sns_topic.monitoring_alerts[0].arn, null)
}

output "ecr_registry_url" {
  description = "ECR 리포지토리 URL (GitHub Actions ECR_REGISTRY 값)"
  value       = aws_ecr_repository.blog_api.repository_url
}
