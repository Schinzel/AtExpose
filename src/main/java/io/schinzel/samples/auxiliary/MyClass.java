package io.schinzel.samples.auxiliary;

import com.atexpose.Expose;

/**
 * The purpose of this class it to hold sample static methods that will be exposed.
 */
public class MyClass {

    @Expose
    public static String sayIt() {
        return "Helloooo world!";
    }


    @Expose(
            arguments = {"Int"}
    )
    public static int doubleIt(int i) {
        return i * 2;
    }


    @Expose(
            arguments = {"String"}
    )
    public static String doEcho(String str) {
        return "Echo: " + str;
    }
}
