###############################################################################
# VPC
###############################################################################

resource "aws_vpc" "main" {
  cidr_block           = var.vpc_cidr
  enable_dns_support   = true
  enable_dns_hostnames = false

  tags = {
    Name = "${local.name_prefix}-vpc"
  }
}

###############################################################################
# Subnets (2 AZ × 3 tiers = 6)
###############################################################################

resource "aws_subnet" "public_a" {
  vpc_id                  = aws_vpc.main.id
  cidr_block              = "10.0.1.0/24"
  availability_zone       = "ap-northeast-2a"
  map_public_ip_on_launch = false

  tags = {
    Name = "${local.name_prefix}-public-a"
  }
}

resource "aws_subnet" "public_c" {
  vpc_id                  = aws_vpc.main.id
  cidr_block              = "10.0.2.0/24"
  availability_zone       = "ap-northeast-2c"
  map_public_ip_on_launch = false

  tags = {
    Name = "${local.name_prefix}-public-c"
  }
}

resource "aws_subnet" "app_a" {
  vpc_id            = aws_vpc.main.id
  cidr_block        = "10.0.11.0/24"
  availability_zone = "ap-northeast-2a"

  tags = {
    Name = "${local.name_prefix}-app-a"
  }
}

resource "aws_subnet" "app_c" {
  vpc_id            = aws_vpc.main.id
  cidr_block        = "10.0.12.0/24"
  availability_zone = "ap-northeast-2c"

  tags = {
    Name = "${local.name_prefix}-app-c"
  }
}

resource "aws_subnet" "data_a" {
  vpc_id            = aws_vpc.main.id
  cidr_block        = "10.0.21.0/24"
  availability_zone = "ap-northeast-2a"

  tags = {
    Name = "${local.name_prefix}-data-a"
  }
}

resource "aws_subnet" "data_c" {
  vpc_id            = aws_vpc.main.id
  cidr_block        = "10.0.22.0/24"
  availability_zone = "ap-northeast-2c"

  tags = {
    Name = "${local.name_prefix}-data-c"
  }
}

###############################################################################
# Internet Gateway
###############################################################################

resource "aws_internet_gateway" "main" {
  vpc_id = aws_vpc.main.id

  tags = {
    Name = "${local.name_prefix}-igw"
  }
}

###############################################################################
# NAT Gateway (single, in public-a)
###############################################################################

resource "aws_eip" "nat" {
  domain = "vpc"

  tags = {
    Name = "${local.name_prefix}-nat-eip"
  }

  depends_on = [aws_internet_gateway.main]
}

resource "aws_nat_gateway" "main" {
  allocation_id = aws_eip.nat.id
  subnet_id     = aws_subnet.public_a.id

  tags = {
    Name = "${local.name_prefix}-nat"
  }

  depends_on = [aws_internet_gateway.main]
}

###############################################################################
# Route Tables
###############################################################################

resource "aws_route_table" "public" {
  vpc_id = aws_vpc.main.id

  route {
    cidr_block = "0.0.0.0/0"
    gateway_id = aws_internet_gateway.main.id
  }

  tags = {
    Name = "${local.name_prefix}-public-rt"
  }
}

resource "aws_route_table" "app" {
  vpc_id = aws_vpc.main.id

  route {
    cidr_block     = "0.0.0.0/0"
    nat_gateway_id = aws_nat_gateway.main.id
  }

  tags = {
    Name = "${local.name_prefix}-app-rt"
  }
}

resource "aws_route_table" "data" {
  vpc_id = aws_vpc.main.id

  tags = {
    Name = "${local.name_prefix}-data-rt"
  }
}

resource "aws_route_table_association" "public_a" {
  subnet_id      = aws_subnet.public_a.id
  route_table_id = aws_route_table.public.id
}

resource "aws_route_table_association" "public_c" {
  subnet_id      = aws_subnet.public_c.id
  route_table_id = aws_route_table.public.id
}

resource "aws_route_table_association" "app_a" {
  subnet_id      = aws_subnet.app_a.id
  route_table_id = aws_route_table.app.id
}

resource "aws_route_table_association" "app_c" {
  subnet_id      = aws_subnet.app_c.id
  route_table_id = aws_route_table.app.id
}

resource "aws_route_table_association" "data_a" {
  subnet_id      = aws_subnet.data_a.id
  route_table_id = aws_route_table.data.id
}

resource "aws_route_table_association" "data_c" {
  subnet_id      = aws_subnet.data_c.id
  route_table_id = aws_route_table.data.id
}

###############################################################################
# Network ACLs
###############################################################################

resource "aws_network_acl" "app" {
  vpc_id     = aws_vpc.main.id
  subnet_ids = [aws_subnet.app_a.id, aws_subnet.app_c.id]

  # Ingress: ALB (public subnets) → EB on port 80
  ingress {
    rule_no    = 100
    action     = "allow"
    protocol   = "6"
    cidr_block = "10.0.1.0/24"
    from_port  = 80
    to_port    = 80
  }
  ingress {
    rule_no    = 110
    action     = "allow"
    protocol   = "6"
    cidr_block = "10.0.2.0/24"
    from_port  = 80
    to_port    = 80
  }
  # Ingress: return traffic (ephemeral)
  ingress {
    rule_no    = 200
    action     = "allow"
    protocol   = "6"
    cidr_block = "0.0.0.0/0"
    from_port  = 1024
    to_port    = 65535
  }

  # Egress: app → data subnets for RDS 5432
  egress {
    rule_no    = 100
    action     = "allow"
    protocol   = "6"
    cidr_block = "10.0.21.0/24"
    from_port  = 5432
    to_port    = 5432
  }
  egress {
    rule_no    = 110
    action     = "allow"
    protocol   = "6"
    cidr_block = "10.0.22.0/24"
    from_port  = 5432
    to_port    = 5432
  }
  # Egress: app → data subnets for Redis 6379
  egress {
    rule_no    = 120
    action     = "allow"
    protocol   = "6"
    cidr_block = "10.0.21.0/24"
    from_port  = 6379
    to_port    = 6379
  }
  egress {
    rule_no    = 130
    action     = "allow"
    protocol   = "6"
    cidr_block = "10.0.22.0/24"
    from_port  = 6379
    to_port    = 6379
  }
  # Egress: HTTPS out (external APIs)
  egress {
    rule_no    = 200
    action     = "allow"
    protocol   = "6"
    cidr_block = "0.0.0.0/0"
    from_port  = 443
    to_port    = 443
  }
  # Egress: ephemeral (responses to ALB etc.)
  egress {
    rule_no    = 210
    action     = "allow"
    protocol   = "6"
    cidr_block = "0.0.0.0/0"
    from_port  = 1024
    to_port    = 65535
  }

  tags = {
    Name = "${local.name_prefix}-app-nacl"
  }
}

resource "aws_network_acl" "data" {
  vpc_id     = aws_vpc.main.id
  subnet_ids = [aws_subnet.data_a.id, aws_subnet.data_c.id]

  # Ingress: app subnets → RDS 5432
  ingress {
    rule_no    = 100
    action     = "allow"
    protocol   = "6"
    cidr_block = "10.0.11.0/24"
    from_port  = 5432
    to_port    = 5432
  }
  ingress {
    rule_no    = 110
    action     = "allow"
    protocol   = "6"
    cidr_block = "10.0.12.0/24"
    from_port  = 5432
    to_port    = 5432
  }
  # Ingress: app subnets → Redis 6379
  ingress {
    rule_no    = 120
    action     = "allow"
    protocol   = "6"
    cidr_block = "10.0.11.0/24"
    from_port  = 6379
    to_port    = 6379
  }
  ingress {
    rule_no    = 130
    action     = "allow"
    protocol   = "6"
    cidr_block = "10.0.12.0/24"
    from_port  = 6379
    to_port    = 6379
  }

  # Egress: ephemeral responses back to app subnets only
  egress {
    rule_no    = 100
    action     = "allow"
    protocol   = "6"
    cidr_block = "10.0.11.0/24"
    from_port  = 1024
    to_port    = 65535
  }
  egress {
    rule_no    = 110
    action     = "allow"
    protocol   = "6"
    cidr_block = "10.0.12.0/24"
    from_port  = 1024
    to_port    = 65535
  }

  tags = {
    Name = "${local.name_prefix}-data-nacl"
  }
}
