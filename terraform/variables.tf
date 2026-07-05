variable "aws_region" {
  description = "AWS region to deploy resources"
  type        = string
  default     = "us-east-1"
}

variable "project_name" {
  description = "Name of the project"
  type        = string
  default     = "rate-limiter"
}

variable "db_username" {
  description = "Master username for PostgreSQL RDS"
  type        = string
  default     = "postgres"
}

variable "db_password" {
  description = "Master password for PostgreSQL RDS"
  type        = string
  sensitive   = true
  default     = "G3~W84mllP"
}

variable "db_name" {
  description = "Database name"
  type        = string
  default     = "ratelimiter"
}

variable "ec2_key_name" {
  description = "Name of the AWS SSH key pair to access the EC2 instance"
  type        = string
  default     = "rate-limiter-key"
}

variable "rate_limiter_strategy" {
  description = "Active rate limiting strategy: fixed-window, sliding-window, token-bucket, or leaky-bucket"
  type        = string
  default     = "fixed-window"
}

variable "rate_limiter_max_requests" {
  description = "Max requests allowed in the rate limit window"
  type        = number
  default     = 10
}

variable "rate_limiter_window_seconds" {
  description = "Duration of the rate limit window in seconds"
  type        = number
  default     = 60
}

variable "rate_limiter_leaky_capacity" {
  description = "Leaky bucket capacity size"
  type        = number
  default     = 5
}

variable "rate_limiter_leaky_rate" {
  description = "Leaky bucket process rate per second"
  type        = number
  default     = 1
}
