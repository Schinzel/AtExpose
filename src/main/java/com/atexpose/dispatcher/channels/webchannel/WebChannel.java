package com.atexpose.dispatcher.channels.webchannel;

import com.atexpose.dispatcher.channels.IChannel;
import com.atexpose.dispatcher.channels.webchannel.http.HttpRedirectResponse;
import com.atexpose.dispatcher.channels.webchannel.http.HttpTextResponse;
import com.atexpose.dispatcher.channels.webchannel.redirect.Redirects;
import com.atexpose.dispatcher.parser.urlparser.httprequest.HttpRequest;
import com.atexpose.util.ByteStorage;
import com.atexpose.util.EncodingUtil;
import io.schinzel.basicutils.Checker;
import io.schinzel.basicutils.EmptyObjects;
import io.schinzel.basicutils.Thrower;
import io.schinzel.basicutils.state.State;
import lombok.Builder;
import lombok.experimental.Accessors;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.URI;

/**
 * The purpose of this class is to listen for and read incoming request on
 * a certain port and write responses to the requests.
 *
 * @author Schinzel
 */
@Accessors(prefix = "m")
public class WebChannel implements IChannel {
    private static final int MAX_PENDING_REQUESTS = 50;
    /** The server socket. Shared by all threads listening to the same port. */
    final private ServerSocket mServerSocket;
    /** The socket timeout. */
    final private int mSocketTimeout;
    /** Holds the redirects. File, host and https redirects. */
    final private Redirects mRedirects;
    /** For logging and stats, hold the time it took to read the message from first to last byte. */
    private long mLogRequestReadTime;
    /** The client socket connection. */
    private Socket mClientSocket;
    /** The response write time. For logging and stats */
    private long mResponseWriteTime = 0L;


    //------------------------------------------------------------------------
    // CONSTRUCTORS AND SHUTDOWN
    //------------------------------------------------------------------------
    @Builder
    WebChannel(int port, int timeout, Redirects redirects) {
        this(getServerSocket(port), redirects, timeout);
        Thrower.throwIfOutsideRange(port, "port", 1, 65535);
        Thrower.throwIfOutsideRange(timeout, "timeout", 50, 30000);
    }


    static ServerSocket getServerSocket(int port) {
        try {
            return new ServerSocket(port, MAX_PENDING_REQUESTS);
        } catch (IOException ioe) {
            throw new RuntimeException("Error starting thread on port " + port + ". Most likely the port is busy. " + ioe.getMessage());
        }
    }


    @Builder(builderMethodName = "cloneBuilder", buildMethodName = "buildClone")
    private WebChannel(ServerSocket serverSocket, Redirects redirects, int timeout) {
        mServerSocket = serverSocket;
        mSocketTimeout = timeout;
        mRedirects = redirects;
    }


    @Override
    public IChannel getClone() {
        return WebChannel.cloneBuilder()
                .redirects(mRedirects)
                .timeout(mSocketTimeout)
                .serverSocket(mServerSocket)
                .buildClone();
    }


    @Override
    public void shutdown(Thread thread) {
        try {
            mServerSocket.close();
        } catch (IOException ex) {
            System.out.println("Error while closing socket");
        }
    }


    //------------------------------------------------------------------------
    // MESSAGING
    //------------------------------------------------------------------------
    @Override
    public boolean getRequest(ByteStorage request) {
        boolean keepReadingFromSocket;
        do {
            keepReadingFromSocket = false;
            try {
                mClientSocket = mServerSocket.accept();
            } catch (SocketException se) {
                //If socket is closed, most likely the shutdown method was used.
                if (mServerSocket.isClosed()) {
                    return false;
                }
            } catch (IOException ioe) {
                //If an error was thrown while waiting for accept the shutdown method in this object was probably(hopefully) invoked.
                return false;
            }
            try {
                mLogRequestReadTime = System.currentTimeMillis();
                mClientSocket.setSoTimeout(mSocketTimeout);
                HttpRequest httpRequest = SocketRW.read(request, mClientSocket);
                //Get direct response (empty string if there is no direct response)
                String directResponse = this.getDirectResponse(httpRequest);
                //If there was no direct response
                if (!Checker.isEmpty(directResponse)) {
                    byte[] redirectAsByteArr = EncodingUtil.convertToByteArray(directResponse);
                    //Send the redirect instruction to client
                    this.writeResponse(redirectAsByteArr);
                    //Clear the incoming request.
                    request.clear();
                    keepReadingFromSocket = true;
                }
            }//Catch read timeout errors
            catch (InterruptedIOException iioe) {
                keepReadingFromSocket = true;
                mLogRequestReadTime = System.currentTimeMillis() - mLogRequestReadTime;
                String err = "Server got read timeout error when reading from client socket No of bytes: " + request.getNoOfBytesStored() + " ";
                try {
                    mClientSocket.close();
                } catch (IOException e) {
                    err += " and failed to close the connection";
                }
                err += " : " + iioe.getMessage();
                throw new RuntimeException(err);
            } catch (Exception e) {
                mLogRequestReadTime = System.currentTimeMillis() - mLogRequestReadTime;
                throw new RuntimeException("Error while reading from socket. " + e.getMessage());
            }
        } while (keepReadingFromSocket);
        mLogRequestReadTime = System.currentTimeMillis() - mLogRequestReadTime;
        return true;
    }


    /**
     * Get any direct response. Direct responses is when the WebChannel send the response directly
     * without involving the rest of @expose. For example, if there is a http to https redirect.
     *
     * @param httpRequest
     * @return Empty string if no direct response is to be sent. Else the direct response to send.
     */
    private String getDirectResponse(HttpRequest httpRequest) {
        if (httpRequest.isGhostCall()) {
            return HttpTextResponse.wrap("Hi Ghost!");
        }
        URI uri = httpRequest.getURI();
        if (mRedirects.shouldRedirect(uri)) {
            uri = mRedirects.getNewLocation(uri);
            return HttpRedirectResponse.getHeader(uri);

        }
        //There was no direct response, and thus return empty string.
        return EmptyObjects.EMPTY_STRING;
    }


    @Override
    public void writeResponse(byte[] response) {
        try {
            mResponseWriteTime = System.currentTimeMillis();
            //Send the Response to the client.
            SocketRW.write(mClientSocket, response);
        } catch (IOException ioe) {
            //If not "Error while writing to socket Connection reset by peer: socket write error"
            //Error indicating timeout on client.
            if (ioe.getMessage().compareToIgnoreCase(
                    "Error while writing to socket Connection reset by peer: socket write error") == -1) {
                throw new RuntimeException("Error while writing to socket " + ioe.getMessage());
            }
        } finally {
            try {
                //Close the client connection.
                mClientSocket.close();
                mResponseWriteTime = (System.currentTimeMillis() - mResponseWriteTime);
            } catch (IOException e) {
            }
        }
    }


    //------------------------------------------------------------------------
    // LOGGING & STATS
    //------------------------------------------------------------------------
    @Override
    public long responseWriteTime() {
        return mResponseWriteTime;
    }


    @Override
    public long requestReadTime() {
        return mLogRequestReadTime;
    }


    @Override
    public String senderInfo() {
        return mClientSocket.getInetAddress().getHostAddress() + ":" + mClientSocket.getPort();
    }
    //------------------------------------------------------------------------
    // STATUS
    //------------------------------------------------------------------------


    @Override
    public State getState() {
        return State.getBuilder()
                .add("Port", mServerSocket.getLocalPort())
                .add("Timeout", mSocketTimeout)
                .add("Queue", MAX_PENDING_REQUESTS)
                .build();
    }

}

