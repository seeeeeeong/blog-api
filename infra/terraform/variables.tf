# ── General ──────────────────────────────────

variable "project_name" {
  description = "Resource name prefix"
  type        = string
  default     = "blog"
}

variable "aws_region" {
  description = "AWS region"
  type        = string
  default     = "ap-northeast-2"
}

# ── Compute ──────────────────────────────────

variable "ec2_instance_type" {
  description = "EC2 instance type"
  type        = string
  default     = "t4g.micro"
}

variable "ec2_key_name" {
  description = "SSH key pair (empty = SSM only)"
  type        = string
  default     = ""
}

# ── Database ─────────────────────────────────

variable "rds_instance_class" {
  description = "RDS instance class"
  type        = string
  default     = "db.t3.micro"
}

variable "db_password" {
  description = "RDS master password"
  type        = string
  sensitive   = true
}

# ── Domain & CDN ─────────────────────────────

variable "domain_name" {
  description = "Custom domain (empty = CloudFront defaults)"
  type        = string
  default     = ""
}

variable "cloudfront_acm_certificate_arn" {
  description = "ACM cert ARN in us-east-1 for CloudFront"
  type        = string
  default     = ""
}

variable "cloudfront_origin_secret" {
  description = "Header secret to block direct EC2 access"
  type        = string
  sensitive   = true
  default     = ""
}

# ── CI/CD ────────────────────────────────────

variable "github_repo" {
  description = "blog-api GitHub repo (owner/repo)"
  type        = string
  default     = "sinseonglee/blog-api"
}

variable "devlog_archive_github_repo" {
  description = "devlog-archive GitHub repo (owner/repo)"
  type        = string
  default     = "seeeeeeong/devlog-archive"
}

variable "ssm_parameter_prefix" {
  description = "SSM prefix for blog-api config"
  type        = string
  default     = "/blog/prod"
}

variable "devlog_archive_ssm_parameter_prefix" {
  description = "SSM prefix for devlog-archive config"
  type        = string
  default     = "/devlog-archive/prod"
}

# ── Monitoring ───────────────────────────────

variable "monitoring_alert_email" {
  description = "CloudWatch alarm email (empty = disabled)"
  type        = string
  default     = ""
}

variable "cpu_alarm_threshold" {
  description = "CPU alarm threshold (%)"
  type        = number
  default     = 80
}

variable "memory_alarm_threshold" {
  description = "Memory alarm threshold (%)"
  type        = number
  default     = 85
}

variable "disk_alarm_threshold" {
  description = "Disk alarm threshold (%)"
  type        = number
  default     = 85
}

variable "api_5xx_error_rate_threshold" {
  description = "API 5xx error rate threshold (%)"
  type        = number
  default     = 1
}

# ── devlog-archive ───────────────────────────

variable "archive_domain_name" {
  description = "devlog-archive domain"
  type        = string
  default     = "archive.seeeeeeong.com"
}
