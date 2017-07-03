package com.atexpose.util.sqs;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClientBuilder;
import com.amazonaws.services.sqs.model.Message;
import com.amazonaws.services.sqs.model.ReceiveMessageRequest;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * The purpose of this class is to read messages from an AWS SQS queue.
 * <p>
 * Created by schinzel on 2017-07-02.
 */
public class SqsReceiver {
    @Getter(AccessLevel.PRIVATE) private final String queueUrl;
    @Getter(AccessLevel.PRIVATE) private final AmazonSQS sqsClient;


    @Builder
    SqsReceiver(String awsAccessKey, String awsSecretKey, Regions region, String queueUrl) {
        AWSCredentials credentials = new BasicAWSCredentials(awsAccessKey, awsSecretKey);
        this.sqsClient = AmazonSQSClientBuilder
                .standard()
                .withCredentials(new AWSStaticCredentialsProvider(credentials))
                .withRegion(region)
                .build();
        this.queueUrl = queueUrl;
    }


    public String receive() {
        List<Message> messages;
        do {
            ReceiveMessageRequest receiveMessageRequest = new ReceiveMessageRequest()
                    .withQueueUrl(this.getQueueUrl())
                    .withMaxNumberOfMessages(1)
                    .withWaitTimeSeconds(20);
            messages = this.getSqsClient().receiveMessage(receiveMessageRequest).getMessages();
        } while (messages.isEmpty());
        Message message = messages.get(0);
        this.getSqsClient().deleteMessage(this.getQueueUrl(), message.getReceiptHandle());
        return message.getBody();
    }
}
