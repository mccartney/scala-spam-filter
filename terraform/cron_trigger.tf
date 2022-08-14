resource "aws_cloudwatch_event_rule" "this" {
  name                = "Every-hour"
  schedule_expression = "cron(3 6-23 * * ? *)"
}

resource "aws_cloudwatch_event_target" "this" {
  rule  = aws_cloudwatch_event_rule.this.name
  arn   = aws_lambda_function.main.arn
}

resource "aws_lambda_permission" "this" {
  function_name = aws_lambda_function.main.function_name
  principal     = "events.amazonaws.com"
  action        = "lambda:InvokeFunction"
  source_arn    = aws_cloudwatch_event_rule.this.arn
}