locals {
  cloudwatch_alarm_actions = var.monitoring_alert_email != "" ? [aws_sns_topic.monitoring_alerts[0].arn] : []
}

resource "aws_sns_topic" "monitoring_alerts" {
  count = var.monitoring_alert_email != "" ? 1 : 0
  name  = "${var.project_name}-monitoring-alerts"
}

resource "aws_sns_topic_subscription" "monitoring_email" {
  count     = var.monitoring_alert_email != "" ? 1 : 0
  topic_arn = aws_sns_topic.monitoring_alerts[0].arn
  protocol  = "email"
  endpoint  = var.monitoring_alert_email
}

resource "aws_cloudwatch_metric_alarm" "ec2_cpu_high" {
  alarm_name          = "${var.project_name}-ec2-cpu-high"
  comparison_operator = "GreaterThanThreshold"
  evaluation_periods  = 3
  metric_name         = "CPUUtilization"
  namespace           = "AWS/EC2"
  period              = 300
  statistic           = "Average"
  threshold           = var.cpu_alarm_threshold
  alarm_description   = "EC2 CPU 사용률이 높습니다."
  treat_missing_data  = "missing"

  dimensions = {
    InstanceId = aws_instance.main.id
  }

  alarm_actions = local.cloudwatch_alarm_actions
  ok_actions    = local.cloudwatch_alarm_actions
}

resource "aws_cloudwatch_metric_alarm" "ec2_status_check_failed" {
  alarm_name          = "${var.project_name}-ec2-status-check-failed"
  comparison_operator = "GreaterThanThreshold"
  evaluation_periods  = 2
  metric_name         = "StatusCheckFailed"
  namespace           = "AWS/EC2"
  period              = 60
  statistic           = "Maximum"
  threshold           = 0
  alarm_description   = "EC2 상태 점검 실패가 감지되었습니다."
  treat_missing_data  = "missing"

  dimensions = {
    InstanceId = aws_instance.main.id
  }

  alarm_actions = local.cloudwatch_alarm_actions
  ok_actions    = local.cloudwatch_alarm_actions
}
