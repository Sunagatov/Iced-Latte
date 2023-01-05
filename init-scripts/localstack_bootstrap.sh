#!/bin/bash
echo "-----------------------------------------------------------------------------------"
echo "########### Setting env variables ###########"
export AWS_ENDPOINT_URL=http://localhost:4566
export AWS_DEFAULT_PROFILE=test-profile
export AWS_REGION=us-east-1
export AWS_OUTPUT_FORMAT=table
export SNS_TOPIC=purchase-transactions-sns-topic
export SQS_QUEUE=purchase-transactions-sqs-queue

echo "AWS_ENDPOINT_URL         = ${AWS_ENDPOINT_URL}"
echo "AWS_DEFAULT_PROFILE      = ${AWS_DEFAULT_PROFILE}"
echo "AWS_REGION               = ${AWS_REGION}"
echo "SNS_TOPIC                = ${SNS_TOPIC}"
echo "SQS_QUEUE                = ${SQS_QUEUE}"
echo "CURRENT MACHINE HOSTNAME = ${HOSTNAME}"

echo "-----------------------------------------------------------------------------------"
echo "########### Setting up localstack profile ###########"
aws configure set aws_access_key_id     access_key          --profile="$AWS_DEFAULT_PROFILE"
aws configure set aws_secret_access_key secret_key          --profile="$AWS_DEFAULT_PROFILE"
aws configure set region                $AWS_REGION         --profile="$AWS_DEFAULT_PROFILE"
aws configure set output                $AWS_OUTPUT_FORMAT  --profile="$AWS_DEFAULT_PROFILE"
aws configure list --profile $AWS_DEFAULT_PROFILE

echo "-----------------------------------------------------------------------------------"
echo "########### Creating AWS SQS queue  ###########"
aws --endpoint-url="$AWS_ENDPOINT_URL" \
sqs create-queue \
--region $AWS_REGION \
--queue-name $SQS_QUEUE \

echo "-----------------------------------------------------------------------------------"
echo "########### Printing list of AWS SQS queues ###########"
aws --endpoint-url="$AWS_ENDPOINT_URL" \
sqs list-queues \
--region $AWS_REGION

echo "-----------------------------------------------------------------------------------"
echo "########### Creating AWS SNS topic  ###########"
aws --endpoint-url="$AWS_ENDPOINT_URL" \
 sns create-topic \
--region $AWS_REGION \
--name $SNS_TOPIC

echo "-----------------------------------------------------------------------------------"
echo "########### Printing list of AWS SNS topics ###########"
aws --endpoint-url="$AWS_ENDPOINT_URL" \
sns  list-topics \
--region $AWS_REGION \
--starting-token=0  \
--max-items=3

echo "-----------------------------------------------------------------------------------"
echo "########### Subscribe AWS SQS queue to AWS SNS topic  ###########"
aws --endpoint-url="$AWS_ENDPOINT_URL" \
sns subscribe \
--region $AWS_REGION \
--topic-arn arn:aws:sns:us-east-1:000000000000:purchase-transactions-sns-topic \
--protocol sqs \
--notification-endpoint http://localhost:4566/000000000000/purchase-transactions-sqs-queue \
--return-subscription-arn

echo "-----------------------------------------------------------------------------------"
echo "########### Printing list of AWS SNS topic subscriptions  ###########"
aws --endpoint-url $AWS_ENDPOINT_URL \
sns list-subscriptions \
--region $AWS_REGION


