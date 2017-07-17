package com.sabproxy;

import com.sabproxy.util.Utils;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;

public class SABPUser {
    public static String CREDENTIALS_FILE = "credentials.txt";
    private final Logger log = LoggerFactory.getLogger(this.getClass());
    private String username = "admin";
    private String password = DigestUtils.md5Hex(username);

    public SABPUser() {

    }

    public String getUserName() {
        String credentials = readCredentialsFile();
        String[] creds = credentials.split(":");
        return creds[0];
    }

    public String getPasswordHash() {
        String credentials = readCredentialsFile();
        String[] creds = credentials.split(":");
        return creds[1];
    }

    public void initializeUser() {
        File credentialsFile = new File(getCredentialsFile());
        if (credentialsFile.exists()) {
            log.info("Found credentials file");
        } else {
            String userPassword = username + ":" + password;
            try {
                FileUtils.writeByteArrayToFile(credentialsFile, userPassword.getBytes(), false);
            } catch (IOException e) {
                log.error("Failed to create credentials file: " + e.getMessage());
            }
            log.info("Created new credentials file (user: " + username + ", password: " + username + ")");
            log.warn("Highly recommended that you change default credentials!");
        }
    }

    private String getCredentialsFile() {
        return Utils.getAppSettingFolder() + "/" + CREDENTIALS_FILE;
    }

    private String readCredentialsFile() {
        try {
            return FileUtils.readFileToString(new File(getCredentialsFile()), "UTF-8");
        } catch (IOException e) {
            log.error("Failed to read credentials file: " + e.getMessage());
        }
        return null;
    }

}
