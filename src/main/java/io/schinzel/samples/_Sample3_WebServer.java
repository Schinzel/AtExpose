package io.schinzel.samples;

import com.atexpose.AtExpose;
import com.atexpose.api.datatypes.DataType;
import com.atexpose.dispatcher.IDispatcher;
import com.atexpose.dispatcher.logging.Logger;
import com.atexpose.dispatcher.logging.LoggerType;
import com.atexpose.dispatcher.logging.format.LogFormatterFactory;
import com.atexpose.dispatcher.logging.writer.LogWriterFactory;
import com.atexpose.dispatcherfactories.CliFactory;
import com.atexpose.dispatcherfactories.WebServerBuilder;
import io.schinzel.samples.auxiliary.MyClass;
import io.schinzel.samples.auxiliary.MyObjectWithCustomArgument;

/**
 * This sample exposes a class and an object.
 * Also a more advanced web server configuration is used.
 * A command line interface and a web server is started
 * The web server serves files from the directory "web/sample3"
 * <p>
 * Test the below commands in a browser:
 * http://127.0.0.1:5555/mypage.html
 *
 * @author schinzel
 */
public class _Sample3_WebServer {


    public static void main(String[] args) {
        AtExpose atExpose = AtExpose.create();
        //Add a custom argument
        atExpose.getAPI()
                .addArgument("Price", DataType.INT, "A price");
        atExpose
                //Expose static methods in a class
                .expose(MyClass.class)
                //Expose an instance
                .expose(new MyObjectWithCustomArgument())
                //Start web server
                .start(getWebServer())
                //Start a command line interface
                .start(CliFactory.create());
    }


    private static IDispatcher getWebServer() {
        return WebServerBuilder.create()
                //Set the dir from which requested file will be read
                .webServerDir("web/sample3")
                //Disable RAM cache so that changes to files made kicks through without restarting the web server
                .cacheFilesInRAM(false)
                //Set custom 404 page
                .fileName404Page("404page.html")
                //Build web server
                .build()
                //Add a logger
                .addLogger(getEventLogger());
    }


    private static Logger getEventLogger() {
        return Logger.builder()
                .loggerType(LoggerType.EVENT)
                .logFormatter(LogFormatterFactory.JSON.create())
                .logWriter(LogWriterFactory.SYSTEM_OUT.create())
                .build();
    }
}
