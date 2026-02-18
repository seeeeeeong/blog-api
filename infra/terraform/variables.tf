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
