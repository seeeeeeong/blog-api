variable "project_name" {
  description = "프로젝트 이름"
  type        = string
  default     = "blog"
}

variable "aws_region" {
  description = "AWS 리전"
  type        = string
  default     = "ap-northeast-2"
}

# --- EC2 ---
variable "ec2_instance_type" {
  description = "EC2 인스턴스 타입"
  type        = string
  default     = "t4g.micro"
}

variable "ec2_key_name" {
  description = "EC2 SSH 키페어 이름 (SSM 도입 후 불필요 — 빈 문자열 유지)"
  type        = string
  default     = ""
}

variable "github_repo" {
  description = "GitHub 저장소 (owner/repo 형식). OIDC 신뢰 정책에 사용"
  type        = string
  default     = "sinseonglee/blog-api"
}

# --- Domain ---
variable "domain_name" {
  description = "도메인 이름 (비워두면 Route53 생성 안 됨)"
  type        = string
  default     = ""
}

variable "cloudfront_origin_secret" {
  description = "CloudFront → EC2 Origin Custom Header 값. Caddy에서 이 값 없는 요청을 403으로 차단해 EC2 직접 접근을 방지함."
  type        = string
  sensitive   = true
  default     = ""
}

variable "cloudfront_acm_certificate_arn" {
  description = "CloudFront용 ACM 인증서 ARN(us-east-1). 비우면 기본 인증서 사용"
  type        = string
  default     = ""
}

# --- Monitoring ---
variable "monitoring_alert_email" {
  description = "CloudWatch 알람 수신 이메일 (비워두면 이메일 알림 미사용)"
  type        = string
  default     = ""
}

variable "cpu_alarm_threshold" {
  description = "EC2 CPUUtilization 경고 임계치(%)"
  type        = number
  default     = 80
}

variable "memory_alarm_threshold" {
  description = "EC2 메모리 사용률 경고 임계치(%) (CloudWatch Agent)"
  type        = number
  default     = 85
}

variable "disk_alarm_threshold" {
  description = "EC2 디스크 사용률 경고 임계치(%) (CloudWatch Agent)"
  type        = number
  default     = 85
}

variable "api_5xx_error_rate_threshold" {
  description = "CloudFront API 5xx 비율 경고 임계치(%)"
  type        = number
  default     = 1
}

variable "ssm_parameter_prefix" {
  description = "배포 시 앱 설정을 읽을 SSM Parameter 경로 prefix (예: /blog/prod)"
  type        = string
  default     = "/blog/prod"
}

# --- RDS ---
variable "rds_instance_class" {
  description = "RDS 인스턴스 타입"
  type        = string
  default     = "db.t3.micro"
}

variable "db_password" {
  description = "RDS PostgreSQL 마스터 비밀번호 (terraform.tfvars에 설정)"
  type        = string
  sensitive   = true
}

# --- devlog-archive integration ---
variable "devlog_archive_github_repo" {
  description = "devlog-archive GitHub 저장소 (owner/repo 형식)"
  type        = string
  default     = "seeeeeeong/devlog-archive"
}

variable "archive_domain_name" {
  description = "devlog-archive 서비스 도메인 (e.g. archive.seeeeeeong.com)"
  type        = string
  default     = "archive.seeeeeeong.com"
}

variable "devlog_archive_ssm_parameter_prefix" {
  description = "devlog-archive SSM Parameter 경로 prefix"
  type        = string
  default     = "/devlog-archive/prod"
}
