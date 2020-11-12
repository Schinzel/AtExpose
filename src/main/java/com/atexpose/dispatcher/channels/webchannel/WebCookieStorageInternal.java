package com.atexpose.dispatcher.channels.webchannel;

import io.schinzel.basicutils.thrower.Thrower;
import lombok.val;

import java.util.*;

/**
 * The purpose of this class is to store cookies per thread
 */
class WebCookieStorageInternal {
    /**
     * Holds the incoming cookies that where a part of the request
     * Key - the name of a thread
     * Value - A map with cookie keys and values
     */
    final Map<String, Map<String, String>> cookiesFromClient = new HashMap<>();
    /**
     * Holds cookies to send to the browser
     * Key - the name of the thread
     * Value - A list of cookies to send to the client
     */
    final Map<String, List<WebSessionCookie>> cookiesToSendToClient = new HashMap<>();


    String getIncomingCookie(String cookieName, String threadName) {
        try {
            Thrower.throwIfVarEmpty(cookieName, "cookieName");
            Thrower.throwIfVarEmpty(threadName, "threadName");
            val currentThreadsCookies = cookiesFromClient.get(threadName);
            if (currentThreadsCookies == null) {
                throw new RuntimeException("No cookies for thread");
            }
            return currentThreadsCookies.get(cookieName);
        } catch (Exception e) {
            val errorMessage = "Error when requesting incoming cookie named '"
                    + cookieName + "' in thread '" + threadName + "'. ";
            throw new RuntimeException(errorMessage + e.getMessage());
        }
    }


    void addCookieToSendToClient(WebSessionCookie cookie, String threadName) {
        try {
            Thrower.throwIfVarNull(cookie, "cookie");
            Thrower.throwIfVarEmpty(threadName, "threadName");
            // If the current thread is not in the map
            if (!cookiesToSendToClient.containsKey(threadName)) {
                // Add an entry for current thread
                cookiesToSendToClient.put(threadName, new ArrayList<>());
            }
            // Add the argument cookie to the current thread list of cookie to send to client
            cookiesToSendToClient
                    .get(threadName)
                    .add(cookie);
        } catch (Exception e) {
            val errorMessage = "Error when adding a cookie to send to client. '"
                    + "' in thread '" + threadName + "'. ";
            throw new RuntimeException(errorMessage + e.getMessage());
        }
    }


    void setCookiesFromClient(Map<String, String> cookies, String threadName) {
        Thrower.throwIfVarEmpty(threadName, "threadName");
        if (cookies == null) {
            cookies = Collections.emptyMap();
        }
        final HashMap<String, String> map = new HashMap<>(cookies);
        cookiesFromClient.put(threadName, map);
    }


    List<WebSessionCookie> getCookiesToSendToClient(String threadName) {
        Thrower.throwIfVarEmpty(threadName, "threadName");
        return cookiesToSendToClient.get(threadName);
    }


    void closeSession(String threadName) {
        Thrower.throwIfVarEmpty(threadName, "threadName");
        // Get map for cookies-from-client
        Map<String, String> currentThreadsCookiesFromClient = cookiesFromClient.get(threadName);
        // If there was a map
        if (currentThreadsCookiesFromClient != null) {
            // Clear the map
            currentThreadsCookiesFromClient.clear();
        }
        // Get map for cookies-to-send-to-client
        List<WebSessionCookie> currentThreadsCookiesToSendToClient = cookiesToSendToClient.get(threadName);
        // If there was such a map
        if (currentThreadsCookiesToSendToClient != null) {
            // Clear the map
            currentThreadsCookiesToSendToClient.clear();
        }
    }
}
