package org.suai.generator;

import com.unboundid.ldap.sdk.LDAPException;
import lombok.extern.slf4j.Slf4j;
import org.suai.generator.core.config.TrafficGeneratorConfig;
import org.suai.generator.core.service.TrafficGeneratorService;

import java.io.IOException;

@Slf4j
public class Main {
    public static void main(String[] args) {

        if (args.length != 1) {
            System.out.println("Usage: java -jar <jar name> <config file>");
            System.exit(0);
        }

        TrafficGeneratorConfig config = new TrafficGeneratorConfig();

        try {
            config.load(args[0]);
        } catch (IOException e) {
            log.error("Could not find or load configuration file: {}", e.getLocalizedMessage());
            System.exit(0);
        }

        try {
            TrafficGeneratorService generatorService = new TrafficGeneratorService(config);
            generatorService.generate();
        } catch (LDAPException | InterruptedException e) {
            e.printStackTrace();
        }
    }
}
