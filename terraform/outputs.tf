output "vpc_id" {
  description = "ID of the VPC created"
  value       = aws_vpc.main.id
}

output "ec2_public_ip" {
  description = "Public IP address of the EC2 instance hosting the rate limiter application"
  value       = aws_instance.app_server.public_ip
}

output "rds_endpoint" {
  description = "Connection endpoint of the PostgreSQL RDS instance (Hostname:Port)"
  value       = aws_db_instance.postgres.endpoint
}

output "rds_address" {
  description = "Hostname address of the PostgreSQL RDS instance"
  value       = aws_db_instance.postgres.address
}
