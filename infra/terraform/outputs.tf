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

output "ssh_command" {
  description = "SSH 접속 명령어"
  value       = "ssh -i ~/.ssh/${var.ec2_key_name}.pem ec2-user@${aws_eip.main.public_ip}"
}
