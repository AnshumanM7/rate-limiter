terraform {
  required_version = ">= 1.10.0"
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
  }

  backend "s3" {
    bucket       = "devops-assignment-terraform-state-779323586929-us-east-1-an"
    key          = "rate-limiter/terraform.tfstate"
    region       = "us-east-1"
    encrypt      = true
    use_lockfile = true
  }
}

provider "aws" {
  region = var.aws_region
}

# 1. VPC & Networking Infrastructure

resource "aws_vpc" "main" {
  cidr_block           = "10.0.0.0/16"
  enable_dns_hostnames = true
  enable_dns_support   = true

  tags = {
    Name = "${var.project_name}-vpc"
  }
}

resource "aws_internet_gateway" "gw" {
  vpc_id = aws_vpc.main.id

  tags = {
    Name = "${var.project_name}-igw"
  }
}

# Subnet 1 in Availability Zone A
resource "aws_subnet" "public_a" {
  vpc_id            = aws_vpc.main.id
  cidr_block        = "10.0.1.0/24"
  availability_zone = "${var.aws_region}a"
  map_public_ip_on_launch = true

  tags = {
    Name = "${var.project_name}-public-subnet-a"
  }
}

# Subnet 2 in Availability Zone B (needed for RDS Subnet Group)
resource "aws_subnet" "public_b" {
  vpc_id            = aws_vpc.main.id
  cidr_block        = "10.0.2.0/24"
  availability_zone = "${var.aws_region}b"
  map_public_ip_on_launch = true

  tags = {
    Name = "${var.project_name}-public-subnet-b"
  }
}

resource "aws_route_table" "public_rt" {
  vpc_id = aws_vpc.main.id

  route {
    cidr_block = "0.0.0.0/0"
    gateway_id = aws_internet_gateway.gw.id
  }

  tags = {
    Name = "${var.project_name}-public-rt"
  }
}

resource "aws_route_table_association" "a" {
  subnet_id      = aws_subnet.public_a.id
  route_table_id = aws_route_table.public_rt.id
}

resource "aws_route_table_association" "b" {
  subnet_id      = aws_subnet.public_b.id
  route_table_id = aws_route_table.public_rt.id
}

# Private Subnets
resource "aws_subnet" "private_a" {
  vpc_id            = aws_vpc.main.id
  cidr_block        = "10.0.3.0/24"
  availability_zone = "${var.aws_region}a"
  map_public_ip_on_launch = false

  tags = {
    Name = "${var.project_name}-private-subnet-a"
  }
}

resource "aws_subnet" "private_b" {
  vpc_id            = aws_vpc.main.id
  cidr_block        = "10.0.4.0/24"
  availability_zone = "${var.aws_region}b"
  map_public_ip_on_launch = false

  tags = {
    Name = "${var.project_name}-private-subnet-b"
  }
}

# Private Route Table
resource "aws_route_table" "private_rt" {
  vpc_id = aws_vpc.main.id

  tags = {
    Name = "${var.project_name}-private-rt"
  }
}

# Private Route Table Associations
resource "aws_route_table_association" "private_a" {
  subnet_id      = aws_subnet.private_a.id
  route_table_id = aws_route_table.private_rt.id
}

resource "aws_route_table_association" "private_b" {
  subnet_id      = aws_subnet.private_b.id
  route_table_id = aws_route_table.private_rt.id
}


# 2. Security Groups


# Security Group for EC2 Server hosting the rate-limiter
resource "aws_security_group" "ec2_sg" {
  name        = "${var.project_name}-ec2-sg"
  description = "Allow SSH and Spring Boot traffic"
  vpc_id      = aws_vpc.main.id

  ingress {
    description = "SSH from anywhere"
    from_port   = 22
    to_port     = 22
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  ingress {
    description = "Spring Boot application port"
    from_port   = 8080
    to_port     = 8080
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = {
    Name = "${var.project_name}-ec2-sg"
  }
}

# Security Group for RDS PostgreSQL Database
resource "aws_security_group" "db_sg" {
  name        = "${var.project_name}-db-sg"
  description = "Allow database traffic from EC2 only"
  vpc_id      = aws_vpc.main.id

  ingress {
    description     = "PostgreSQL port"
    from_port       = 5432
    to_port         = 5432
    protocol        = "tcp"
    security_groups = [aws_security_group.ec2_sg.id] # Limit access ONLY to EC2 instance
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = {
    Name = "${var.project_name}-db-sg"
  }
}

# 3. Database (RDS PostgreSQL)

resource "aws_db_subnet_group" "db_subnets" {
  name       = "${var.project_name}-db-private-subnet-group"
  subnet_ids = [aws_subnet.private_a.id, aws_subnet.private_b.id]

  tags = {
    Name = "${var.project_name}-db-subnet-group"
  }

  lifecycle {
    create_before_destroy = true
  }
}

resource "aws_db_instance" "postgres" {
  identifier             = "${var.project_name}-private-db"
  allocated_storage      = 20
  max_allocated_storage  = 100
  db_name                = var.db_name
  engine                 = "postgres"
  engine_version         = "16.3"
  instance_class         = "db.t4g.micro" # Free tier eligible in newer regions, t3.micro can be used otherwise
  username               = var.db_username
  password               = var.db_password
  db_subnet_group_name   = aws_db_subnet_group.db_subnets.name
  vpc_security_group_ids = [aws_security_group.db_sg.id]
  skip_final_snapshot    = true
  publicly_accessible    = false

  tags = {
    Name = "${var.project_name}-rds-db"
  }
}

# 4. Compute (EC2 Instance)

data "aws_ami" "ubuntu" {
  most_recent = true
  owners      = ["099720109477"] # Canonical owner ID

  filter {
    name   = "name"
    values = ["ubuntu/images/hvm-ssd/ubuntu-jammy-22.04-amd64-server-*"]
  }

  filter {
    name   = "virtualization-type"
    values = ["hvm"]
  }
}

resource "aws_instance" "app_server" {
  ami                    = data.aws_ami.ubuntu.id
  instance_type          = "t3.micro"
  subnet_id              = aws_subnet.public_a.id
  vpc_security_group_ids = [aws_security_group.ec2_sg.id]
  key_name               = var.ec2_key_name
  iam_instance_profile   = aws_iam_instance_profile.ec2_profile.name
  user_data_replace_on_change = true

  # Dynamically inject database credentials and Spring Boot overrides into EC2 environment variables
  user_data = <<-EOF
              #!/bin/bash
              # Write custom app env vars to /etc/profile for interactive shells
              echo "export RDS_HOSTNAME=\"${aws_db_instance.postgres.address}\"" >> /etc/profile
              echo "export RDS_DB_NAME=\"${var.db_name}\"" >> /etc/profile
              echo "export RDS_USERNAME=\"${var.db_username}\"" >> /etc/profile
              echo "export RDS_PASSWORD=\"${var.db_password}\"" >> /etc/profile

              # Standard Spring Boot environment variables to override H2 defaults
              echo "export SPRING_DATASOURCE_URL=\"jdbc:postgresql://${aws_db_instance.postgres.address}:5432/${var.db_name}\"" >> /etc/profile
              echo "export SPRING_DATASOURCE_USERNAME=\"${var.db_username}\"" >> /etc/profile
              echo "export SPRING_DATASOURCE_PASSWORD=\"${var.db_password}\"" >> /etc/profile
              echo "export SPRING_DATASOURCE_DRIVER_CLASS_NAME=\"org.postgresql.Driver\"" >> /etc/profile
              echo "export SPRING_JPA_DATABASE_PLATFORM=\"org.hibernate.dialect.PostgreSQLDialect\"" >> /etc/profile
              echo "export SPRING_JPA_HIBERNATE_DDL_AUTO=\"update\"" >> /etc/profile
              echo "export RATE_LIMITER_STRATEGY=\"${var.rate_limiter_strategy}\"" >> /etc/profile
              echo "export RATE_LIMITER_MAX_REQUESTS=\"${var.rate_limiter_max_requests}\"" >> /etc/profile
              echo "export RATE_LIMITER_WINDOW_SECONDS=\"${var.rate_limiter_window_seconds}\"" >> /etc/profile
              echo "export RATE_LIMITER_LEAKY_CAPACITY=\"${var.rate_limiter_leaky_capacity}\"" >> /etc/profile
              echo "export RATE_LIMITER_LEAKY_RATE=\"${var.rate_limiter_leaky_rate}\"" >> /etc/profile

              # Write to /etc/environment for system services/daemons
              echo "RDS_HOSTNAME=${aws_db_instance.postgres.address}" >> /etc/environment
              echo "RDS_DB_NAME=${var.db_name}" >> /etc/environment
              echo "RDS_USERNAME=${var.db_username}" >> /etc/environment
              echo "RDS_PASSWORD=${var.db_password}" >> /etc/environment
              echo "SPRING_DATASOURCE_URL=jdbc:postgresql://${aws_db_instance.postgres.address}:5432/${var.db_name}" >> /etc/environment
              echo "SPRING_DATASOURCE_USERNAME=${var.db_username}" >> /etc/environment
              echo "SPRING_DATASOURCE_PASSWORD=${var.db_password}" >> /etc/environment
              echo "SPRING_DATASOURCE_DRIVER_CLASS_NAME=org.postgresql.Driver" >> /etc/environment
              echo "SPRING_JPA_DATABASE_PLATFORM=org.hibernate.dialect.PostgreSQLDialect" >> /etc/environment
              echo "SPRING_JPA_HIBERNATE_DDL_AUTO=update" >> /etc/environment
              echo "RATE_LIMITER_STRATEGY=${var.rate_limiter_strategy}" >> /etc/environment
              echo "RATE_LIMITER_MAX_REQUESTS=${var.rate_limiter_max_requests}" >> /etc/environment
              echo "RATE_LIMITER_WINDOW_SECONDS=${var.rate_limiter_window_seconds}" >> /etc/environment
              echo "RATE_LIMITER_LEAKY_CAPACITY=${var.rate_limiter_leaky_capacity}" >> /etc/environment
              echo "RATE_LIMITER_LEAKY_RATE=${var.rate_limiter_leaky_rate}" >> /etc/environment

              # Install Docker Community Edition
              apt-get update
              apt-get install -y apt-transport-https ca-certificates curl software-properties-common
              curl -fsSL https://download.docker.com/linux/ubuntu/gpg | apt-key add -
              add-apt-repository -y "deb [arch=amd64] https://download.docker.com/linux/ubuntu $(lsb_release -cs) stable"
              apt-get update
              apt-get install -y docker-ce

              # Add the default ubuntu user to the docker group to allow execution without sudo
              usermod -aG docker ubuntu
              systemctl enable docker
              systemctl start docker
              EOF

  tags = {
    Name = "${var.project_name}-app-server"
  }
}

# 5. IAM Resources for CloudWatch Logging
resource "aws_iam_role" "ec2_role" {
  name = "${var.project_name}-ec2-role"

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

resource "aws_iam_role_policy_attachment" "cw_agent_policy" {
  role       = aws_iam_role.ec2_role.name
  policy_arn = "arn:aws:iam::aws:policy/CloudWatchAgentServerPolicy"
}

resource "aws_iam_instance_profile" "ec2_profile" {
  name = "${var.project_name}-ec2-profile"
  role = aws_iam_role.ec2_role.name
}

# 6. CloudWatch Log Group for Docker Container
resource "aws_cloudwatch_log_group" "app_logs" {
  name              = "/aws/ec2/${var.project_name}-app"
  retention_in_days = 7
}

# 7. CloudWatch Metric Filters (Application Metrics)
resource "aws_cloudwatch_log_metric_filter" "app_errors" {
  name           = "${var.project_name}-app-errors"
  pattern        = "ERROR"
  log_group_name = aws_cloudwatch_log_group.app_logs.name

  metric_transformation {
    name      = "AppErrors"
    namespace = "RateLimiterApp"
    value     = "1"
  }
}

resource "aws_cloudwatch_log_metric_filter" "total_requests" {
  name           = "${var.project_name}-total-requests"
  pattern        = ""
  log_group_name = aws_cloudwatch_log_group.app_logs.name

  metric_transformation {
    name      = "RequestCount"
    namespace = "RateLimiterApp"
    value     = "1"
  }
}

# 8. CloudWatch Dashboards

# Dashboard 1: Infrastructure Performance
resource "aws_cloudwatch_dashboard" "infrastructure" {
  dashboard_name = "${var.project_name}-infrastructure"

  dashboard_body = jsonencode({
    widgets = [
      {
        type   = "metric"
        x      = 0
        y      = 0
        width  = 12
        height = 6
        properties = {
          metrics = [
            [ "AWS/EC2", "CPUUtilization", "InstanceId", aws_instance.app_server.id ]
          ]
          period = 60
          stat   = "Average"
          region = var.aws_region
          title  = "EC2 CPU Utilization (%)"
          view   = "timeSeries"
          stacked = false
        }
      },
      {
        type   = "metric"
        x      = 12
        y      = 0
        width  = 12
        height = 6
        properties = {
          metrics = [
            [ "AWS/EC2", "NetworkIn", "InstanceId", aws_instance.app_server.id ],
            [ "AWS/EC2", "NetworkOut", "InstanceId", aws_instance.app_server.id ]
          ]
          period = 60
          stat   = "Average"
          region = var.aws_region
          title  = "EC2 Network Traffic (Bytes)"
          view   = "timeSeries"
          stacked = false
        }
      },
      {
        type   = "metric"
        x      = 0
        y      = 6
        width  = 12
        height = 6
        properties = {
          metrics = [
            [ "AWS/EC2", "DiskReadBytes", "InstanceId", aws_instance.app_server.id ],
            [ "AWS/EC2", "DiskWriteBytes", "InstanceId", aws_instance.app_server.id ]
          ]
          period = 60
          stat   = "Average"
          region = var.aws_region
          title  = "EC2 Disk Operations (Bytes)"
          view   = "timeSeries"
          stacked = false
        }
      },
      {
        type   = "metric"
        x      = 12
        y      = 6
        width  = 12
        height = 6
        properties = {
          metrics = [
            [ "AWS/RDS", "CPUUtilization", "DBInstanceIdentifier", aws_db_instance.postgres.identifier ]
          ]
          period = 60
          stat   = "Average"
          region = var.aws_region
          title  = "RDS Database CPU Utilization (%)"
          view   = "timeSeries"
          stacked = false
        }
      },
      {
        type   = "metric"
        x      = 0
        y      = 12
        width  = 24
        height = 6
        properties = {
          metrics = [
            [ "AWS/RDS", "DatabaseConnections", "DBInstanceIdentifier", aws_db_instance.postgres.identifier ]
          ]
          period = 60
          stat   = "Average"
          region = var.aws_region
          title  = "RDS Database Active Connections"
          view   = "timeSeries"
          stacked = false
        }
      }
    ]
  })
}

# Dashboard 2: Application Logs & Metrics
resource "aws_cloudwatch_dashboard" "application" {
  dashboard_name = "${var.project_name}-application"

  dashboard_body = jsonencode({
    widgets = [
      {
        type   = "metric"
        x      = 0
        y      = 0
        width  = 12
        height = 6
        properties = {
          metrics = [
            [ "RateLimiterApp", "RequestCount" ]
          ]
          period = 60
          stat   = "Sum"
          region = var.aws_region
          title  = "Application Request Volume (Logs Count)"
          view   = "timeSeries"
          stacked = false
        }
      },
      {
        type   = "metric"
        x      = 12
        y      = 0
        width  = 12
        height = 6
        properties = {
          metrics = [
            [ "RateLimiterApp", "AppErrors" ]
          ]
          period = 60
          stat   = "Sum"
          region = var.aws_region
          title  = "Application Error Count (Log pattern: ERROR)"
          view   = "timeSeries"
          stacked = false
        }
      },
      {
        type   = "log"
        x      = 0
        y      = 6
        width  = 24
        height = 12
        properties = {
          queryString = "fields @timestamp, @message | sort @timestamp desc | limit 100"
          region = var.aws_region
          logGroupNames = [
            aws_cloudwatch_log_group.app_logs.name
          ]
          title  = "Live Container Log Stream (Stdout/Stderr)"
        }
      }
    ]
  })
}
