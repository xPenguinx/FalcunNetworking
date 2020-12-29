package me.minidigger.falcunnetworking.common;

import java.nio.charset.Charset;
import java.security.PrivateKey;

public class Constants {

    public static final Charset CHARSET = Charset.forName("UTF-8"); // no StandardCharsets in java 6...

    public static final boolean DEBUG_OFFLINE_MODE = true;
    public static boolean TEST_MODE = false;

    private static PrivateKey privateKey;

    public static PrivateKey getPrivateKey() {
        return privateKey;
    }

    public static void setPrivateKey(PrivateKey privateKey) {
        Constants.privateKey = privateKey;
    }
}
