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
  description = "EC2 SSH 키페어 이름 (AWS 콘솔에서 미리 생성)"
  type        = string
}

# --- Domain ---
variable "domain_name" {
  description = "도메인 이름 (비워두면 Route53 생성 안 됨)"
  type        = string
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
