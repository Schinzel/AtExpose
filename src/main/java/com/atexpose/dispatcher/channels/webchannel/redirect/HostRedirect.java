package com.atexpose.dispatcher.channels.webchannel.redirect;

import io.schinzel.basicutils.Thrower;
import lombok.SneakyThrows;
import org.apache.http.client.utils.URIBuilder;

import java.net.URI;

/**
 * An instance of this class is a redirect from an host to another.
 * <p>
 * Created by schinzel on 2017-04-20.
 */
public class HostRedirect implements IRedirect {
    private final String from;
    private final String to;


    /**
     * Examples of from and to arguments:
     * "example.com"
     * "www.example.com"
     * "sub1.example.com"
     * "sub2.example.com"
     * "www.schinzel.io"
     *
     * @param from The domain to redirect from.
     * @param to   The domain to redirect ot.
     * @return A new instance.
     */
    public static HostRedirect create(String from, String to) {
        return new HostRedirect(from, to);
    }


    private HostRedirect(String from, String to) {
        Thrower.throwIfEmpty(from, "from");
        Thrower.throwIfEmpty(to, "to");
        this.from = from;
        this.to = to;
    }


    @Override
    public boolean shouldRedirect(URI uri) {
        return (uri.getHost().equalsIgnoreCase(this.from));
    }


    @Override
    @SneakyThrows
    public URI getNewLocation(URI uri) {
        return new URIBuilder(uri).setHost(this.to).build();
    }
}
