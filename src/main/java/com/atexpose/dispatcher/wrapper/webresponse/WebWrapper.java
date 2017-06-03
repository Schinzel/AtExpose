package com.atexpose.dispatcher.wrapper.webresponse;

import com.atexpose.MyProperties;
import com.atexpose.dispatcher.PropertiesDispatcher;
import com.atexpose.dispatcher.wrapper.IWrapper;
import com.atexpose.util.EncodingUtil;
import com.atexpose.util.FileRW;
import com.atexpose.util.http.HttpResponse404;
import com.atexpose.util.http.HttpResponseFile;
import com.atexpose.util.http.HttpResponseJson;
import com.atexpose.util.http.HttpResponseString;
import com.google.common.base.Joiner;
import io.schinzel.basicutils.Checker;
import io.schinzel.basicutils.EmptyObjects;
import io.schinzel.basicutils.Thrower;
import io.schinzel.basicutils.collections.Cache;
import io.schinzel.basicutils.state.State;
import io.schinzel.basicutils.str.Str;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.experimental.Accessors;
import org.apache.commons.io.FilenameUtils;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This wrapper handles two types of responses:
 * 1) JSON responses
 * 2) file responses (image, html, js and so on)
 * <p>
 * The file responses are divided into two groups:
 * 1) Text files. Examples: html and text.
 * 2) Static files. Examples: jpg and pdf.
 * <p>
 * Text files support two types of server side includes
 * 1) Files <!--#include file="header.html" -->
 * 2) Variables <!--#echo var="my_var" -->
 * The format is according to SSI: https://en.wikipedia.org/wiki/Server_Side_Includes
 * First are files in included and after that the variables are inserted in the resulting file.
 *
 * @author Schinzel
 */
@Accessors(prefix = "m")
public class WebWrapper implements IWrapper {
    //Pattern for server side variables. Example: <!--#echo var="my_var" -->
    static final Pattern VARIABLE_PLACEHOLDER_PATTERN = Pattern.compile("<!--#echo var=\"([a-zA-Z1-9_]{3,25})\" -->");
    //Pattern for server side include files. Example: <!--#include file="header.html" -->
    private static final Pattern INCLUDE_FILE_PATTERN = Pattern.compile("<!--#include file=\"([\\w,/]+\\.[A-Za-z]{2,4})\" -->");
    private static final String RESPONSE_HEADER_LINE_BREAK = "\r\n";
    /** The default to return if no page was specified. */
    private static final String DEFAULT_PAGE = "index.html";
    /** Where the files to server resides on the hard drive **/
    private final String mWebServerDir;
    /** Browser cache age instruction. **/
    private final int mBrowserCacheMaxAge;
    private final Map<String, String> mServerSideVariables;
    //If true, files read - e.g. HTML files - will be cached in RAM.
    private boolean mFilesCacheOn = true;
    @Getter(AccessLevel.PACKAGE)
    private Map<String, String> mResponseHeaders = new HashMap<>();
    @Getter(AccessLevel.PACKAGE)
    private Cache<String, byte[]> mFilesCache;

    private enum ReturnType {
        FILE, JSON, STRING
    }

    private enum HTTPStatusCode {
        OK("200 OK"),
        FileNotFound("404  Not Found"),
        InternalServerError("500 Internal Server Error");
        final String mCode;


        HTTPStatusCode(String code) {
            mCode = code;
        }
    }


    @Builder
    WebWrapper(String webServerDir, int browserCacheMaxAge, boolean cacheFilesInRam,
               Map<String, String> serverSideVariables, Map<String, String> responseHeaders) {
        //If the last char is not a file separator, then add it
        mWebServerDir = (!webServerDir.endsWith(MyProperties.FILE_SEPARATOR)) ?
                webServerDir + MyProperties.FILE_SEPARATOR : webServerDir;
        mBrowserCacheMaxAge = browserCacheMaxAge;
        mFilesCacheOn = cacheFilesInRam;
        mServerSideVariables = (serverSideVariables != null)
                ? serverSideVariables
                : EmptyObjects.EMPTY_MAP;
        Thrower.throwIfVarOutsideRange(browserCacheMaxAge, "browserCacheMaxAge", 0, 604800);
        mResponseHeaders = (responseHeaders != null)
                ? responseHeaders
                : EmptyObjects.EMPTY_MAP;
        mFilesCache = new Cache<>();
    }


    @Override
    public String wrapResponse(String methodReturn) {
        return HttpResponseString.builder()
                .body(methodReturn)
                .customResponseHeaders(this.getResponseHeaders())
                .build()
                .getResponse();
    }


    @Override
    public String wrapError(String error) {
        int contentLength = EncodingUtil.convertToByteArray(error).length;
        String responseHeader = getResponseHeader("", contentLength, HTTPStatusCode.InternalServerError, ReturnType.STRING);
        return responseHeader + error;
    }


    @Override
    public byte[] wrapFile(String requestedFile) {
        String filename = this.getActualFilename(requestedFile);
        return isTextFile(filename)
                ? this.getTextFileHeaderAndContent(filename)
                : this.getStaticFileHeaderAndContent(filename);
    }


    /**
     * @param filename
     * @return The content of argument file including HTTP headers.
     */
    byte[] getTextFileHeaderAndContent(String filename) {
        //Get the text file
        byte[] abFileContent = this.getTextFileContent(filename);
        //If there was no such file
        if (abFileContent == null) {
            return HttpResponse404.builder()
                    .customResponseHeaders(this.getResponseHeaders())
                    .filenameMissingFile(filename)
                    .build()
                    .getResponse();
        } else {
            //Add server side variables
            abFileContent = WebWrapper.setServerSideVariables(abFileContent, mServerSideVariables);
            return HttpResponseFile.builder()
                    .body(abFileContent)
                    .customResponseHeaders(this.getResponseHeaders())
                    .fileName(filename)
                    .build()
                    .getResponse();
        }
    }


    /**
     * @param filename
     * @return The content of the argument file. Null if there was no such file.
     */
    byte[] getTextFileContent(String filename) {
        //If is to use cache AND there is the argument file is cached
        if (mFilesCacheOn && mFilesCache.has(filename)) {
            //Return cached file
            return mFilesCache.get(filename);
        }
        //If file does not exist
        if (!FileRW.fileExists(filename)) {
            return null;
        }
        //Read file
        byte[] abFileContent = FileRW.readFileAsByteArray(filename);
        //Add server side include files
        abFileContent = WebWrapper.setServerIncludeFiles(abFileContent, mWebServerDir);
        //If is to use file cache
        if (mFilesCacheOn) {
            //Add file content to cache
            mFilesCache.put(filename, abFileContent);
        }
        return abFileContent;
    }


    /**
     * @param filename
     * @return The content of argument file including HTTP headers.
     */
    byte[] getStaticFileHeaderAndContent(String filename) {
        byte[] abFileContent;
        //If is to use cache AND the argument file is cached
        if (mFilesCacheOn && mFilesCache.has(filename)) {
            //Return cached file
            return mFilesCache.get(filename);
        }
        //If file doesn't exists
        if (!FileRW.fileExists(filename)) {
            return HttpResponse404.builder()
                    .customResponseHeaders(this.getResponseHeaders())
                    .filenameMissingFile(filename)
                    .build()
                    .getResponse();
        } else {
            abFileContent = FileRW.readFileAsByteArray(filename);
            byte[] abFileHeaderAndContent = HttpResponseFile.builder()
                    .body(abFileContent)
                    .customResponseHeaders(this.getResponseHeaders())
                    .fileName(filename)
                    .build()
                    .getResponse();
            //If is to use file cache
            if (mFilesCacheOn) {
                //Add file header and content to cache
                this.mFilesCache.put(filename, abFileHeaderAndContent);
            }
            return abFileHeaderAndContent;
        }

    }


    /**
     * Derives the actual file name for the requested file.
     * <p>
     * If the request is a directory, the default file including the
     * argument directory is returned.
     * <p>
     * If forced default page is enabled, this page in the in the web root is
     * always returned.
     * <p>
     * The directory on the hard drive is added as a prefix to argument file
     * name.
     *
     * @param requestedFile
     * @return
     */
    String getActualFilename(String requestedFile) {
        // if filename is empty
        if (Checker.isEmpty(requestedFile)) {
            requestedFile = DEFAULT_PAGE;
        } // if the request if a folder path, we return the default file in this folder
        else if (isFolderPath(requestedFile)) {
            // suffix with / if not there
            if (!requestedFile.endsWith("/")) {
                requestedFile += "/";
            }
            // suffix with default page
            requestedFile += DEFAULT_PAGE;
        }
        //Prefix with path to directory where files resides
        requestedFile = mWebServerDir + requestedFile;
        return requestedFile;
    }


    /**
     * @param filename
     * @return True if argument file has a text file extension, else false.
     */
    static boolean isTextFile(String filename) {
        String fileExtension = FilenameUtils.getExtension(filename);
        return FileTypes.getInstance().getProps(fileExtension).isTextFile();
    }


    @Override
    public String wrapJSON(JSONObject response) {
        return HttpResponseJson.builder()
                .body(response)
                .customResponseHeaders(this.getResponseHeaders())
                .build()
                .getResponse();
    }
    // ------------------------------------
    // - SERVER SIDE INCLUDES
    // ------------------------------------


    /**
     * Replaces all the server side variable placeholders <!--#echo var="MY_VAR" --> with
     * the server side variable value.
     *
     * @param fileContent
     * @return The argument with the placeholders replaced with server side
     * variables.
     */
    static byte[] setServerSideVariables(byte[] fileContent, Map<String, String> serverSideVariables) {
        //Create a string from the file content
        String mainFileContent = EncodingUtil.convertToString(fileContent);
        //Create holder of return string
        StringBuffer fileContentReturn = new StringBuffer();
        //Create a matcher for the placeholders for variables on the file content
        Matcher placeHolderMatcher = VARIABLE_PLACEHOLDER_PATTERN.matcher(mainFileContent);
        //Go through all server side variable tags in the file
        while (placeHolderMatcher.find()) {
            //Get the name of the variable
            String currentPlaceholder = placeHolderMatcher.group(1);
            String variableValue = serverSideVariables.get(currentPlaceholder);
            //If there was a value for the found place holder
            if (variableValue != null) {
                variableValue = Matcher.quoteReplacement(variableValue);
                //Replace the placeholder with the value
                placeHolderMatcher.appendReplacement(fileContentReturn, variableValue);
            }
        }
        //Add the end of the file to return string
        placeHolderMatcher.appendTail(fileContentReturn);
        //Create byte array and return
        return EncodingUtil.convertToByteArray(fileContentReturn.toString());
    }


    /**
     * Replaces all include file placeholders <!--#include file="header.html" --> with the
     * content of the include file.
     *
     * @param fileContent
     * @return The argument with the placeholders replaced with filecontent.
     */
    static byte[] setServerIncludeFiles(byte[] fileContent, String directory) {
        //Create a string from the file content
        String mainFileContent = EncodingUtil.convertToString(fileContent);
        //Create holder of return string
        StringBuffer fileContentReturn = new StringBuffer();
        ///Create a matcher for the include file tags on the file content
        Matcher includeFileMatcher = INCLUDE_FILE_PATTERN.matcher(mainFileContent);
        //Go through all include file tags in the file
        while (includeFileMatcher.find()) {
            //Get the name of the include file
            String includeFilename = includeFileMatcher.group(1);
            //Add directory to file name
            includeFilename = directory + includeFilename;
            String includeFileContent;
            if (FileRW.fileExists(includeFilename)) {
                //Get include file content
                includeFileContent = FileRW.readFileAsString(includeFilename);
            } else {
                includeFileContent = "Include file '" + includeFilename + "' not found";
            }
            //Replace all $ with \$. This as dollar sign has a sepcial meaning in Matcher.appendRelacement
            includeFileContent = includeFileContent.replaceAll("\\$", "\\\\\\$");
            //Add content up until include file and replace include file reference with include file content
            includeFileMatcher.appendReplacement(fileContentReturn, includeFileContent);
        }
        //Add the end of the file to return string
        includeFileMatcher.appendTail(fileContentReturn);
        //Create byte array and return
        return EncodingUtil.convertToByteArray(fileContentReturn.toString());
    }
    // ------------------------------------
    // - PRIVATE STATIC UTIL
    // ------------------------------------


    /**
     * @param filename
     * @param contentLength
     * @return A response header for the argument filename and content length
     */
    private String getResponseHeader(String filename, int contentLength, HTTPStatusCode HTTPStatusCode, ReturnType contentType) {
        Str str = Str.create()
                .a("HTTP/1.1 ").acrlf(HTTPStatusCode.mCode)
                .a("Server: ").acrlf(PropertiesDispatcher.RESP_HEADER_SERVER_NAME)
                .a("Content-Length: ").acrlf(String.valueOf(contentLength))
                .a("Content-Type: ").acrlf(getResponseHeaderContentType(filename, contentType));
        //If there are any response headers to attach
        if (!mResponseHeaders.isEmpty()) {
            //Add the response headers
            str.acrlf(Joiner.on("\r\n").withKeyValueSeparator(": ").join(mResponseHeaders));
        }
        //If there was no filename, i.e. is a method call Set the cache to zero seconds
        int cacheMaxAgeInSeconds = Checker.isEmpty(filename) ? 0 : mBrowserCacheMaxAge;
        return str.a("Cache-Control: ").a("max-age=")
                .acrlf(String.valueOf(cacheMaxAgeInSeconds))
                .acrlf().toString();
    }


    /**
     * @param filename
     * @return The content-type to use in a response header for the argument
     * file name
     */
    private static String getResponseHeaderContentType(String filename, ReturnType contentType) {
        String headerContentType;
        switch (contentType) {
            case JSON:
                headerContentType = "application/json; charset=UTF-8";
                break;
            case STRING:
                headerContentType = "text/html; charset=UTF-8";
                break;
            case FILE:
                String filenameExtension = FilenameUtils.getExtension(filename);
                headerContentType = FileTypes.getInstance().getProps(filenameExtension).getHeaderContentType();
                break;
            default:
                throw new RuntimeException("Unhandled content type '" + contentType.name() + "'.");
        }
        return headerContentType;
    }


    /**
     * Method to test if request is for a file or folder
     *
     * @param requestName
     * @return
     */
    static boolean isFolderPath(String requestName) {
        int lastSlash = requestName.lastIndexOf('/');
        int lastDot = requestName.lastIndexOf('.');
        // if we have not dot, it is a folder
        // if we have a slash after the last dot, it is a folder
        // else it is a file
        return lastDot == -1 || lastSlash > lastDot;
    }


    @Override
    public State getState() {
        return State.getBuilder()
                .add("Directory", mWebServerDir)
                .add("BrowserCacheMaxAge", mBrowserCacheMaxAge)
                .add("FilesInRamCache", mFilesCacheOn)
                .build();
    }

}
