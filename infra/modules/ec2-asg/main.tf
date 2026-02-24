# =============================================================================
# ECS-optimized AMI (Amazon Linux 2023)
# =============================================================================

data "aws_ssm_parameter" "ecs_ami" {
  name = "/aws/service/ecs/optimized-ami/amazon-linux-2023/recommended/image_id"
}

# =============================================================================
# IAM Role & Instance Profile
# =============================================================================

resource "aws_iam_role" "ec2_instance" {
  name = "${var.project}-${var.environment}-ecs-ec2-role"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Action = "sts:AssumeRole"
        Effect = "Allow"
        Principal = {
          Service = "ec2.amazonaws.com"
        }
      }
    ]
  })
}

resource "aws_iam_role_policy_attachment" "ecs_ec2" {
  role       = aws_iam_role.ec2_instance.name
  policy_arn = "arn:aws:iam::aws:policy/service-role/AmazonEC2ContainerServiceforEC2Role"
}

resource "aws_iam_role_policy_attachment" "ssm_ec2" {
  role       = aws_iam_role.ec2_instance.name
  policy_arn = "arn:aws:iam::aws:policy/AmazonSSMManagedInstanceCore"
}

resource "aws_iam_instance_profile" "ec2" {
  name = "${var.project}-${var.environment}-ecs-ec2-profile"
  role = aws_iam_role.ec2_instance.name
}

# =============================================================================
# Launch Template
# =============================================================================

resource "aws_launch_template" "ecs" {
  name_prefix   = "${var.project}-${var.environment}-ecs-"
  image_id      = data.aws_ssm_parameter.ecs_ami.value
  instance_type = var.instance_type

  iam_instance_profile {
    arn = aws_iam_instance_profile.ec2.arn
  }

  vpc_security_group_ids = [var.security_group_id]

  user_data = base64encode(<<-EOF
    #!/bin/bash
    echo "ECS_CLUSTER=${var.cluster_name}" >> /etc/ecs/ecs.config
  EOF
  )

  block_device_mappings {
    device_name = "/dev/xvda"

    ebs {
      volume_size = 30
      volume_type = "gp3"
      encrypted   = true
    }
  }

  metadata_options {
    http_endpoint               = "enabled"
    http_tokens                 = "required"
    http_put_response_hop_limit = 1
  }

  tag_specifications {
    resource_type = "instance"

    tags = {
      Name = "${var.project}-${var.environment}-ecs-instance"
    }
  }

  lifecycle {
    create_before_destroy = true
  }
}

# =============================================================================
# Auto Scaling Group
# =============================================================================

resource "aws_autoscaling_group" "ecs" {
  name_prefix = "${var.project}-${var.environment}-ecs-"

  launch_template {
    id      = aws_launch_template.ecs.id
    version = "$Latest"
  }

  min_size         = var.asg_min_size
  max_size         = var.asg_max_size
  desired_capacity = var.asg_desired_capacity

  vpc_zone_identifier = var.private_subnet_ids

  health_check_type         = "EC2"
  health_check_grace_period = 300

  protect_from_scale_in = true

  tag {
    key                 = "Name"
    value               = "${var.project}-${var.environment}-ecs-instance"
    propagate_at_launch = true
  }

  tag {
    key                 = "AmazonECSManaged"
    value               = "true"
    propagate_at_launch = true
  }

  lifecycle {
    create_before_destroy = true
  }
}
