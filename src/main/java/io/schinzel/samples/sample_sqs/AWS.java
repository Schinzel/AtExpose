package io.schinzel.samples.sample_sqs;

import io.schinzel.basicutils.configvar.ConfigVar;

/**
 * Holds AWS credentials and other data.
 * <p>
 * Created by schinzel on 2017-07-07.
 */
class AWS {
    static final String ACCESS_KEY = ConfigVar.create(".env").getValue("AWS_SQS_ACCESS_KEY");
    static final String SECRET_KEY = ConfigVar.create(".env").getValue("AWS_SQS_SECRET_KEY");
    static final String QUEUE_URL = "https://sqs.eu-west-1.amazonaws.com/146535832843/my_first_queue.fifo";

}
