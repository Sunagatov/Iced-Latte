#!/bin/bash
echo "########### Setting env variables ###########"
export AWS_DEFAULT_PROFILE=test-profile
export SNS_TOPIC=purchase-transactions-sns-topic
export SQS_QUEUE=purchase-transactions-sqs-queue
export SNS_ENDPOINT_URL=http://localhost:4566

echo "########### Setting up localstack profile ###########"
aws configure set aws_access_key_id     access_key --profile="$AWS_DEFAULT_PROFILE"
aws configure set aws_secret_access_key secret_key --profile="$AWS_DEFAULT_PROFILE"
aws configure set region                us-east-1  --profile="$AWS_DEFAULT_PROFILE"

echo "########### Creating Subscriber SQS  ###########"
aws --endpoint-url="$SNS_ENDPOINT_URL" \
 sqs create-queue \
  --queue-name "$SQS_QUEUE"

echo "########### Creating SNS topic and getting arn  ###########"
SNS_TOPIC_ARN=$(aws --endpoint-url="$SNS_ENDPOINT_URL" \
  sns create-topic --name=$SNS_TOPIC \
   |  sed 's/"TopicArn"/\n"TopicArn"/g' | grep '"TopicArn"' | awk -F '"TopicArn":' '{print $2}' | tr -d '"' | xargs)

echo "########### List SNS topics ###########"
aws --endpoint-url="$SNS_ENDPOINT_URL" \
 sns  list-topics \
  --starting-token=0  \
  --max-items=3

echo "########### Creating subscription for Spring Boot app  ###########"
aws --endpoint-url="$SNS_ENDPOINT_URL" \
 sns subscribe \
--topic-arn="$SNS_TOPIC_ARN" \
--protocol=sqs \
--notification-endpoint=http://localhost:4566/000000000000/"$SQS_QUEUE" \
--return-subscription-arn

echo "########### List subscriptions  ###########"
aws --endpoint-url="$SNS_ENDPOINT_URL" \
 sns list-subscriptions


