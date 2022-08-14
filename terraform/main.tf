terraform {
  backend "s3" {
    bucket = "mccartney-terraform-state"
    key    = "scala-spam-filter"
    region = "eu-west-1"
  }

  required_providers {
    aws = {
      source = "hashicorp/aws"
    }
  }
}

provider "aws" {
  region  = "eu-west-1"
  profile = "scala-spam-filter"
}

locals {
  build_zip = "../app/build/distributions/lambda-scala-spam-filter.zip"
}