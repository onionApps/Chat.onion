package onion.chat;

public class Native {

    static
    {
        System.loadLibrary("app");
    }

    native public static void killTor();

}
