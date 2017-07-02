package io.schinzel;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClientBuilder;
import com.amazonaws.services.sqs.model.SendMessageRequest;
import io.schinzel.basicutils.RandomUtil;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;

/**
 *
 * Created by schinzel on 2017-07-02.
 */
public class SqsSender {
    @Getter(AccessLevel.PRIVATE) private final String queueUrl;
    @Getter(AccessLevel.PRIVATE) private final AmazonSQS sqsClient;
    @Getter(AccessLevel.PRIVATE) private final String groupId;


    @Builder
    private SqsSender(String awsAccessKey, String awsSecretKey, Regions region, String queueUrl, String groupId) {
        AWSCredentials credentials = new BasicAWSCredentials(awsAccessKey, awsSecretKey);
        this.sqsClient = AmazonSQSClientBuilder
                .standard()
                .withCredentials(new AWSStaticCredentialsProvider(credentials))
                .withRegion(region)
                .build();
        this.queueUrl = queueUrl;
        this.groupId = groupId;
    }


    public SqsSender send(String message) {
        SendMessageRequest sendMsgRequest = new SendMessageRequest()
                .withQueueUrl(this.getQueueUrl())
                .withMessageGroupId(this.getGroupId())
                .withMessageDeduplicationId(getDeduplicationId())
                .withMessageBody(message);
        this.getSqsClient().sendMessage(sendMsgRequest);
        return this;
    }


    private static String getDeduplicationId() {
        return String.valueOf(System.nanoTime()) + "_" + RandomUtil.getRandomString(10);
    }
}
