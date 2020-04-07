package org.suai.generator.core.service;

import com.unboundid.ldap.sdk.*;
import lombok.extern.slf4j.Slf4j;
import org.simplejavamail.email.Email;
import org.simplejavamail.email.EmailBuilder;
import org.simplejavamail.mailer.Mailer;
import org.simplejavamail.mailer.MailerBuilder;
import org.simplejavamail.mailer.config.TransportStrategy;
import org.suai.generator.core.config.TrafficGeneratorConfig;
import org.suai.generator.core.entity.SendingParams;
import org.suai.generator.core.entity.User;

import javax.activation.FileDataSource;
import java.io.File;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
public class TrafficGeneratorService {
    private TrafficGeneratorConfig config;
    private LDAPConnection connection;

    public TrafficGeneratorService(TrafficGeneratorConfig config) throws LDAPException {
        this.config = config;
        this.connection =  new LDAPConnection(
                config.getDomainControllerHost(),
                config.getDomainControllerPort(),
                config.getBindDN(),
                config.getBindPassword()
        );
    }

    public void generate() throws LDAPException, InterruptedException {
        for (int port : config.getMailPorts()) {
            sendMail(port);
        }
        connection.close();
    }

    private void sendMail(int port) throws LDAPException, InterruptedException {
        Mailer mailer = MailerBuilder
                .withSMTPServer(config.getMailHost(), port, config.getBindDN(), config.getBindPassword())
                .withTransportStrategy(TransportStrategy.SMTP)
                .withThreadPoolSize(5)
                .trustingAllHosts(true)
                .buildMailer();

        System.out.println("Sending mail to port " + port);
        for (SendingParams sendingParam : config.getSendingParams()) {
            for (File file : sendingParam.getFiles()) {
                User sender = getUserForSend(sendingParam.getSenderGroup());
                User recipient = getUserForSend(sendingParam.getRecipientGroup());
                System.out.println(String.format("%s --> %s, File: %s", sender, recipient, file.getName()));
                Email email = createEmailWithAttachment(sender, recipient, file);
                mailer.sendMail(email, true);
                System.out.println(String.format("Mail sent. Waiting %d seconds", config.getMailDelay()));
                TimeUnit.SECONDS.sleep(config.getMailDelay());
            }
        }
    }

    private User getUserForSend(String group) throws LDAPException {
        if (group.endsWith("@" + config.getDomainName())) {
            return new User("User", group, "Internal");
        }

        if (group.equals("External")) {
            return new User("External user", getRandomEmailFromList(), "External");
        }

        return getRandomUserFromGroup(group);
    }

    private String getRandomEmailFromList() {
        Random r = new Random();
        return config.getExternalMailList().get(r.nextInt(config.getExternalMailList().size()));
    }

    private User getRandomUserFromGroup(String group) throws LDAPException {
        Random random = new Random();
        SearchRequest searchRequest = new SearchRequest(
                config.getBaseDN(),
                SearchScope.SUB,
                Filter.createANDFilter(
                        Filter.createEqualityFilter("objectClass", "user"),
                        Filter.createEqualityFilter("memberOf", String.format("cn=%s,%s", group, config.getBaseDN()))
                )
        );
        SearchResult searchResult = connection.search(searchRequest);
        List<SearchResultEntry> searchEntries = searchResult.getSearchEntries();
        SearchResultEntry entry = searchEntries.get(random.nextInt(searchEntries.size()));
        return new User(
                entry.getAttributeValue("cn"),
                entry.getAttributeValue("mail"),
                group
        );
    }

    private Email createEmailWithAttachment(User sender, User recipient, File attachment) {
        return EmailBuilder.startingBlank()
                .from(sender.getName(), sender.getEmail())
                .to(recipient.getName(), recipient.getEmail())
                .withSubject(String.format("From: %s, To: %s, File: %s",
                        sender.getGroup(), recipient.getGroup(), attachment.getName()))
                .withPlainText(attachment.getName())
                .withAttachment(attachment.getName(), new FileDataSource(attachment.getAbsolutePath()))
                .buildEmail();
    }
}
