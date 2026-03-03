# =============================================
# VPC (심플 구성 - public subnet만)
# =============================================
resource "aws_vpc" "main" {
  cidr_block           = "10.0.0.0/16"
  enable_dns_hostnames = true
  enable_dns_support   = true

  tags = { Name = "${var.project_name}-vpc" }
}

data "aws_availability_zones" "available" {
  state = "available"
}

resource "aws_subnet" "public" {
  vpc_id                  = aws_vpc.main.id
  cidr_block              = "10.0.1.0/24"
  availability_zone       = data.aws_availability_zones.available.names[0]
  map_public_ip_on_launch = true

  tags = { Name = "${var.project_name}-public" }
}

resource "aws_internet_gateway" "main" {
  vpc_id = aws_vpc.main.id

  tags = { Name = "${var.project_name}-igw" }
}

resource "aws_route_table" "public" {
  vpc_id = aws_vpc.main.id

  route {
    cidr_block = "0.0.0.0/0"
    gateway_id = aws_internet_gateway.main.id
  }

  tags = { Name = "${var.project_name}-public-rt" }
}

resource "aws_route_table_association" "public" {
  subnet_id      = aws_subnet.public.id
  route_table_id = aws_route_table.public.id
}

# =============================================
# Security Group (EC2용)
# =============================================
# CloudFront managed prefix list (origin-facing IP 범위)
data "aws_ec2_managed_prefix_list" "cloudfront" {
  name = "com.amazonaws.global.cloudfront.origin-facing"
}

resource "aws_security_group" "ec2" {
  name_prefix = "${var.project_name}-ec2-"
  vpc_id      = aws_vpc.main.id

  # CloudFront prefix list는 SG 쿼터(기본 60)를 초과함.
  # AWS Service Quotas에서 "Security group rules per security group"을
  # 300 이상으로 증가 신청 후 terraform apply 하면 아래 코드가 적용됨.
  # TODO: 쿼터 증가 후 아래 cidr_blocks 방식을 prefix_list_ids 방식으로 교체
  # CloudFront prefix list는 SG 쿼터(기본 60)를 초과함.
  # AWS Service Quotas에서 "Security group rules per security group"을
  # 300 이상으로 증가 신청 후 terraform apply 하면 아래 코드가 적용됨.
  # TODO: 쿼터 증가 후 아래 cidr_blocks 방식을 prefix_list_ids 방식으로 교체
  ingress {
    description = "HTTP from CloudFront"
    from_port   = 80
    to_port     = 80
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
    # prefix_list_ids = [data.aws_ec2_managed_prefix_list.cloudfront.id]
  }
  # 443은 origin이 http-only라 불필요 — origin secret header로 우회 방지

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = { Name = "${var.project_name}-ec2-sg" }

  lifecycle { create_before_destroy = true }
}
