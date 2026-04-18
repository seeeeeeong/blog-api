# ── blog-ai EC2 IAM Role ─────────────────────

resource "aws_iam_role" "blog_ai_ec2" {
  name = "blog-ai-ec2-role"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Action    = "sts:AssumeRole"
      Effect    = "Allow"
      Principal = { Service = "ec2.amazonaws.com" }
    }]
  })
}

resource "aws_iam_role_policy" "blog_ai_ec2_ecr" {
  name = "blog-ai-ec2-ecr"
  role = aws_iam_role.blog_ai_ec2.id

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect   = "Allow"
        Action   = ["ecr:GetAuthorizationToken"]
        Resource = "*"
      },
      {
        Effect   = "Allow"
        Action   = ["ecr:BatchGetImage", "ecr:GetDownloadUrlForLayer"]
        Resource = [aws_ecr_repository.blog_ai.arn]
      },
    ]
  })
}

resource "aws_iam_role_policy" "blog_ai_ec2_ssm_parameter_read" {
  name = "blog-ai-ec2-ssm-parameter-read"
  role = aws_iam_role.blog_ai_ec2.id

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect = "Allow"
        Action = ["ssm:GetParameter", "ssm:GetParameters", "ssm:GetParametersByPath"]
        Resource = [
          "arn:aws:ssm:${var.aws_region}:${data.aws_caller_identity.current.account_id}:parameter/${trimprefix(var.blog_ai_ssm_parameter_prefix, "/")}/*",
        ]
      },
      {
        Effect    = "Allow"
        Action    = ["kms:Decrypt"]
        Resource  = "*"
        Condition = { StringEquals = { "kms:ViaService" = "ssm.${var.aws_region}.amazonaws.com" } }
      },
    ]
  })
}

resource "aws_iam_role_policy" "blog_ai_ec2_cloudwatch" {
  name = "blog-ai-ec2-cloudwatch"
  role = aws_iam_role.blog_ai_ec2.id

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Effect   = "Allow"
      Action   = ["cloudwatch:PutMetricData"]
      Resource = "*"
    }]
  })
}

resource "aws_iam_role_policy_attachment" "blog_ai_ec2_ssm" {
  role       = aws_iam_role.blog_ai_ec2.name
  policy_arn = "arn:aws:iam::aws:policy/AmazonSSMManagedInstanceCore"
}

resource "aws_iam_instance_profile" "blog_ai_ec2" {
  name = "blog-ai-ec2-profile"
  role = aws_iam_role.blog_ai_ec2.name
}

# ── Security Group ───────────────────────────

resource "aws_security_group" "blog_ai_ec2" {
  name_prefix = "blog-ai-ec2-"
  vpc_id      = aws_vpc.main.id

  # Only blog-api EC2 can reach blog-ai's HTTP port
  ingress {
    description     = "HTTP from blog-api EC2"
    from_port       = 8081
    to_port         = 8081
    protocol        = "tcp"
    security_groups = [aws_security_group.ec2.id]
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = { Name = "blog-ai-ec2-sg" }

  lifecycle { create_before_destroy = true }
}

# ── EC2 Instance ─────────────────────────────

resource "aws_instance" "blog_ai" {
  ami                    = data.aws_ami.amazon_linux.id
  instance_type          = var.blog_ai_instance_type
  subnet_id              = aws_subnet.public.id
  vpc_security_group_ids = [aws_security_group.blog_ai_ec2.id]
  iam_instance_profile   = aws_iam_instance_profile.blog_ai_ec2.name

  root_block_device {
    volume_size = 40
    volume_type = "gp3"
    encrypted   = true
  }

  user_data = <<-EOF
    #!/bin/bash
    set -e

    # 2GB swap
    fallocate -l 2G /swapfile
    chmod 600 /swapfile
    mkswap /swapfile
    swapon /swapfile
    echo '/swapfile swap swap defaults 0 0' >> /etc/fstab

    # Install packages
    dnf update -y
    dnf install -y docker git amazon-cloudwatch-agent cronie postgresql15
    systemctl start docker && systemctl enable docker
    systemctl start crond && systemctl enable crond
    usermod -aG docker ec2-user

    # CloudWatch Agent config (memory + disk metrics)
    cat > /opt/aws/amazon-cloudwatch-agent/etc/amazon-cloudwatch-agent.json <<'CWCONFIG'
    {
      "agent": { "metrics_collection_interval": 60, "run_as_user": "root" },
      "metrics": {
        "namespace": "CWAgent",
        "append_dimensions": { "InstanceId": "$${aws:InstanceId}" },
        "aggregation_dimensions": [["InstanceId"]],
        "metrics_collected": {
          "mem": { "measurement": ["mem_used_percent"] },
          "disk": {
            "measurement": ["used_percent"],
            "resources": ["/"],
            "ignore_file_system_types": ["sysfs","devtmpfs","tmpfs","squashfs","overlay","proc","devfs"]
          }
        }
      }
    }
    CWCONFIG
    /opt/aws/amazon-cloudwatch-agent/bin/amazon-cloudwatch-agent-ctl \
      -a fetch-config -m ec2 \
      -c file:/opt/aws/amazon-cloudwatch-agent/etc/amazon-cloudwatch-agent.json -s || true

    # Docker Compose
    DOCKER_COMPOSE_VERSION="v2.24.5"
    curl -L "https://github.com/docker/compose/releases/download/$${DOCKER_COMPOSE_VERSION}/docker-compose-linux-aarch64" \
      -o /usr/local/bin/docker-compose
    chmod +x /usr/local/bin/docker-compose

    # Runtime directories
    mkdir -p /opt/services/{bin,env}
    mkdir -p /opt/services/data/{postgres,alloy}
    mkdir -p /home/ec2-user/app
    chown ec2-user:ec2-user /home/ec2-user/app
  EOF

  metadata_options {
    http_tokens   = "required"
    http_endpoint = "enabled"
  }

  tags = { Name = "blog-ai-server" }

  lifecycle { ignore_changes = [ami] }
}

# ── SSM parameter so blog-api can reach blog-ai ───

resource "aws_ssm_parameter" "blog_ai_base_url" {
  name  = "${var.ssm_parameter_prefix}/BLOG_AI_BASE_URL"
  type  = "String"
  value = "http://${aws_instance.blog_ai.private_ip}:8081"
}
