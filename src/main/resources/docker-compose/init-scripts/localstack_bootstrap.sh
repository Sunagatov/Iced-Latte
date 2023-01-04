#!/bin/bash
echo "########### Setting env variables ###########"
export AWS_DEFAULT_PROFILE=test-profile
export AWS_REGION=us-east-1
export SNS_TOPIC=purchase-transactions-sns-topic
export SNS_ENDPOINT_URL=http://localhost:4566
export SQS_QUEUE=purchase-transactions-sqs-queue

echo "AWS_DEFAULT_PROFILE = ${AWS_DEFAULT_PROFILE}"
echo "AWS_REGION = ${AWS_REGION}"
echo "SNS_TOPIC = ${SNS_TOPIC}"
echo "SNS_ENDPOINT_URL = ${SNS_ENDPOINT_URL}"
echo "SQS_QUEUE = ${SQS_QUEUE}"

echo "########### Setting up localstack profile ###########"
aws configure set aws_access_key_id     access_key     --profile="$AWS_DEFAULT_PROFILE"
aws configure set aws_secret_access_key secret_key     --profile="$AWS_DEFAULT_PROFILE"
aws configure set region                "$AWS_REGION"  --profile="$AWS_DEFAULT_PROFILE"

echo "########### Creating AWS SQS queue which will be a subscriber of AWS SNS topic  ###########"
aws --endpoint-url="$SNS_ENDPOINT_URL" \
 sqs create-queue \
  --queue-name "$SQS_QUEUE"

echo "########### List of AWS SQS queues ###########"
aws --endpoint-url="$SNS_ENDPOINT_URL" \
 sqs list-queues \
  --region="$AWS_REGION"

echo "########### Creating AWS SNS topic and getting arn  ###########"
SNS_TOPIC_ARN=$(aws --endpoint-url="$SNS_ENDPOINT_URL" \
  sns create-topic --name=$SNS_TOPIC \
   |  sed 's/"TopicArn"/\n"TopicArn"/g' | grep '"TopicArn"' | awk -F '"TopicArn":' '{print $2}' | tr -d '"' | xargs)

echo "########### List of AWS SNS topics ###########"
aws --endpoint-url="$SNS_ENDPOINT_URL" \
 sns  list-topics \
  --starting-token=0  \
  --max-items=3

echo "########### Creating AWS SNS topic subscription (AWS SQS queue)  ###########"
aws --endpoint-url="$SNS_ENDPOINT_URL" \
 sns subscribe \
--topic-arn="$SNS_TOPIC_ARN" \
--protocol=sqs \
--notification-endpoint=http://localhost:4566/000000000000/"$SQS_QUEUE" \
--return-subscription-arn

echo "########### List of AWS SNS topic subscriptions  ###########"
aws --endpoint-url="$SNS_ENDPOINT_URL" \
 sns list-subscriptions


