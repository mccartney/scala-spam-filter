locals {
  name_prefix = "scala-spam-filter"
}

resource "aws_iam_role" "role_for_lambda" {
  name = "${local.name_prefix}-role-for-lambda"

  assume_role_policy = <<EOF
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Action": "sts:AssumeRole",
      "Principal": {
        "Service": "lambda.amazonaws.com"
      },
      "Effect": "Allow",
      "Sid": ""
    }
  ]
}
EOF
}

resource "aws_iam_role_policy" "read_secrets" {
  name = "read_secrets"
  role = aws_iam_role.role_for_lambda.id

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Action = [
          "secretsmanager:GetSecretValue",
        ]
        Effect   = "Allow"
        Resource = "arn:aws:secretsmanager:eu-west-1:214582020536:secret:poczta/onet/grzegon-zEPDW4"
      },
    ]
  })
}

resource "aws_iam_role_policy" "write_logs" {
  name = "write_logs"
  role = aws_iam_role.role_for_lambda.id

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Action = [
          "logs:CreateLogGroup",
          "logs:CreateLogStream",
          "logs:PutLogEvents"
        ]
        Effect   = "Allow"
        Resource = "*"
      },
    ]
  })
}

resource "aws_lambda_function" "main" {
  filename         = local.build_zip
  function_name    = local.name_prefix
  role             = aws_iam_role.role_for_lambda.arn
  handler          = "pl.waw.oledzki.spam_filter.FilterMail"
  source_code_hash = filebase64sha256(local.build_zip)
  runtime          = "java11"
  timeout          = 65
  memory_size      = 512
}
