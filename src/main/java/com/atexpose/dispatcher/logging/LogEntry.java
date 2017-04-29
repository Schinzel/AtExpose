package com.atexpose.dispatcher.logging;

import com.atexpose.dispatcher.channels.IChannel;
import com.atexpose.dispatcher.logging.crypto.ICrypto;
import com.atexpose.dispatcher.parser.Request;
import com.atexpose.util.DateTimeStrings;
import io.schinzel.basicutils.Checker;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The purpose of this class to hold the data wholly or partially end up in a
 * log. For example a log file.
 *
 * @author schinzel
 */
public class LogEntry {
    /**
     * Holds the values of the logged data.
     */
    private final Map<LogKey, String> mLogValues = new LinkedHashMap<>(LogKey.values().length);
    /**
     * The time of the incoming call.
     */
    private Instant mTimeOfIncomingCall;
    /**
     * Flag if current log entry is an error.
     */
    private boolean mIsError = false;
    /**
     *
     */
    private final String mThreadNumber;
    private final IChannel mChannel;
    private String mDecodedIncomingRequest;
    private String mResponse;
    private Request mRequest;


    public LogEntry(int threadNumber, IChannel channel) {
        mThreadNumber = String.valueOf(threadNumber);
        mChannel = channel;
    }


    public void setIsError() {
        mIsError = true;
    }


    /**
     * @return True if this log entry is an error that is being logged, else
     * false.
     */
    public boolean isError() {
        return mIsError;
    }


    /**
     * Sets the time of the incoming call. Is the time after all bytes of
     * when the incoming call have been read.
     */
    public void setTimeOfIncomingCall() {
        mTimeOfIncomingCall = Instant.now();
    }


    /**
     * Set all log values to be empty string.
     */
    public final void cleanUpLogData() {
        mLogValues.clear();
        mIsError = false;
    }


    /**
     * @param crypto Encrypts selected pars of the log data.
     * @return A map with log data.
     */
    Map<LogKey, String> getLogData(ICrypto crypto) {
        String request = crypto.encrypt(mDecodedIncomingRequest);
        String[] argNames = mRequest.getArgumentNames();
        String[] argValues = mRequest.getArgumentValues();
        //Encrypt argument values
        if (!Checker.isEmpty(argValues)) {
            for (int i = 0; i < argValues.length; i++) {
                argValues[i] = crypto.encrypt(argValues[i]);
            }
        }
        String arguments = argumentsToString(argNames, argValues);
        if (mTimeOfIncomingCall == null) {
            mTimeOfIncomingCall = Instant.now();
        }
        mLogValues.put(LogKey.CALL_TIME_UTC, DateTimeStrings.getDateTimeUTC(mTimeOfIncomingCall));
        mLogValues.put(LogKey.METHOD_NAME, mRequest.getMethodName());
        mLogValues.put(LogKey.ARGUMENTS, arguments);
        mLogValues.put(LogKey.FILENAME, mRequest.getFileName());
        mLogValues.put(LogKey.RESPONSE, mResponse);
        mLogValues.put(LogKey.THREAD, mThreadNumber);
        mLogValues.put(LogKey.READ_TIME_IN_MS, String.valueOf(mChannel.requestReadTime()));
        mLogValues.put(LogKey.EXEC_TIME_IN_MS, String.valueOf(mTimeOfIncomingCall.until(Instant.now(), ChronoUnit.MILLIS)));
        mLogValues.put(LogKey.WRITE_TIME_IN_MS, String.valueOf(mChannel.responseWriteTime()));
        mLogValues.put(LogKey.SENDER, mChannel.senderInfo());
        mLogValues.put(LogKey.REQUEST, request);
        return mLogValues;
    }


    /**
     * Sets the log entry data for ea
     *
     * @param request  The incoming request.
     * @param response The response sent.
     */
    public void setLogData(String decodedIncomingRequest, String response, Request request) {
        mDecodedIncomingRequest = decodedIncomingRequest;
        mResponse = response;
        mRequest = request;
    }


    /**
     * @param argumentNames  The names of the arguments
     * @param argumentValues The values of the arguments
     * @return The argument names and values formatted to a string
     */
    private static String argumentsToString(String[] argumentNames, String[] argumentValues) {
        //If there are no arguments
        if (Checker.isEmpty(argumentNames) && Checker.isEmpty(argumentValues)) {
            return "-";
        }
        StringBuilder sb = new StringBuilder();
        //Go through all arguments values
        for (int i = 0; i < argumentValues.length; i++) {
            //If not is first argument
            if (i != 0) {
                sb.append(", ");
            }
            //If there are argument names
            if (!Checker.isEmpty(argumentNames)) {
                sb.append(argumentNames[i]).append("=");
            }
            String argumentValue = argumentValues[i];
            sb.append("\'").append(argumentValue).append("\'");
        }
        return sb.toString();
    }

}
