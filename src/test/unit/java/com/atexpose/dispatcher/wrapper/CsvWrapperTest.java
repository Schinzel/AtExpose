package com.atexpose.dispatcher.wrapper;

import io.schinzel.basicutils.FunnyChars;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.util.Collections;
import java.util.Map;

import static org.junit.Assert.assertEquals;

/**
 * @author schinzel
 */
public class CsvWrapperTest {
    @Rule
    public ExpectedException exception = ExpectedException.none();


    @Test
    public void testWrapResponseString() {
        CsvWrapper csvWrapper = new CsvWrapper();
        String result;
        for (FunnyChars funnyString : FunnyChars.values()) {
            result = csvWrapper.wrapResponse(funnyString.getString());
            assertEquals(funnyString.getString(), result);
        }
    }


    @Test
    public void testWrapError() {
        CsvWrapper csvWrapper = new CsvWrapper();
        String result;
        for (FunnyChars funnyString : FunnyChars.values()) {
            Map<String, String> map = Collections.singletonMap("message", funnyString.getString());
            result = csvWrapper.wrapError(map);
            assertEquals("Error:\nmessage: " + funnyString.getString(), result);
        }
    }


    @Test
    public void testWrapJSON() {
        CsvWrapper csvWrapper = new CsvWrapper();
        JSONObject jo;
        String result;
        //
        jo = new JSONObject();
        jo.put("a", "b");
        result = csvWrapper.wrapJSON(jo);
        assertEquals("{\"a\": \"b\"}", result);
        //
        jo = new JSONObject();
        jo.put("a", "1");
        jo.put("b", "2");
        jo.put("c", "3");
        result = csvWrapper.wrapJSON(jo);
        assertEquals("{\n"
                + "   \"a\": \"1\",\n"
                + "   \"b\": \"2\",\n"
                + "   \"c\": \"3\"\n"
                + "}", result);
    }


    @Test
    public void testGetStatusAsJson() {
        CsvWrapper csvWrapper = new CsvWrapper();
        JSONObject joStatus = csvWrapper.getState().getJson();
        String statusAsString = joStatus.toString();
        assertEquals("{\"ColumnDelimiter\":\", \",\"Class\":\"CsvWrapper\"}", statusAsString);

    }


    @Test
    public void testWrapFile() throws JSONException {
        CsvWrapper csvWrapper = new CsvWrapper();
        exception.expect(UnsupportedOperationException.class);
        exception.expectMessage("Not supported yet.");
        csvWrapper.wrapFile("anyfile.txt");

    }
}
