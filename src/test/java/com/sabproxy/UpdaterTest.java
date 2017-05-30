package com.sabproxy;

import org.junit.Test;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.Assert.assertTrue;

public class UpdaterTest {
    @Test
    public void getLatestVersion() throws Exception {
        Updater updater = new Updater();
        String latestVersion = updater.getLatestVersion();
        Matcher matcher = Pattern.compile("(?!\\.)(\\d+(\\.\\d+)+)(?:[-.][A-Z]+)?(?![\\d.])$").matcher(latestVersion);
        assertTrue(matcher.find()); // actually got a valid version on the response
    }

}