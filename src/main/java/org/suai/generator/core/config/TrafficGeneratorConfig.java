package org.suai.generator.core.config;

import lombok.Getter;
import org.suai.generator.core.entity.SendingParams;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

@Getter
public class TrafficGeneratorConfig {

    private String domainName;
    private String domainControllerHost;
    private int domainControllerPort;
    private String baseDN;
    private String bindDN;
    private String bindPassword;

    private String mailHost;
    private List<Integer> mailPorts;
    private int mailDelay;

    private List<String> externalMailList;

    private String fileSearchDirectory;
    private List<SendingParams> sendingParams = new ArrayList<>();

    public void load(String filename) throws IOException {

        Properties properties = new Properties();
        properties.load(new FileReader(filename));

        domainName = properties.getProperty("domainName");
        domainControllerHost = properties.getProperty("domainController.host");
        domainControllerPort = Integer.parseInt(properties.getProperty("domainController.port"));
        baseDN = properties.getProperty("domainController.baseDN");
        bindDN = properties.getProperty("domainController.bindDN");
        bindPassword = properties.getProperty("domainController.bindPassword");

        mailHost = properties.getProperty("mail.host");
        mailPorts = Arrays.stream(properties.getProperty("mail.ports").split(","))
                .map(s -> Integer.parseInt(s.trim()))
                .collect(Collectors.toList());
        mailDelay = Integer.parseInt(properties.getProperty("mail.delay"));

        externalMailList = Arrays.stream(properties.getProperty("mail.external").split(","))
                .map(String::trim)
                .collect(Collectors.toList());

        fileSearchDirectory = properties.getProperty("fileSearchDirectory");

        loadSendingParams(filename);
    }

    private void loadSendingParams(String filename) throws IOException {
        List<String> groupPairs = getStringArray("groups", filename);
        List<String> dirs = getStringArray("dir", filename);

        for (int i = 0; i < groupPairs.size(); i++) {
            String[] groupPair = groupPairs.get(i).trim().split("->");
            List<File> files = readFilesFromDirectory(dirs.get(i).trim());
            sendingParams.add(new SendingParams(groupPair[0].trim(), groupPair[1].trim(), files));
        }
    }

    private List<String> getStringArray(String key, String filename) throws IOException {
        return Files.readAllLines(Paths.get(filename))
                .stream()
                .filter(n -> n.startsWith(key))
                .map(n -> n.split("=")[1])
                .collect(Collectors.toList());
    }

    private List<File> readFilesFromDirectory(String directory) throws IOException {
        return Files.walk(Paths.get(directory))
                .filter(Files::isRegularFile)
                .map(Path::toFile)
                .collect(Collectors.toList());
    }
}
