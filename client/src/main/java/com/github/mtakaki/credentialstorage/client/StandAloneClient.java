package com.github.mtakaki.credentialstorage.client;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.ServerErrorException;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionGroup;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.github.mtakaki.credentialstorage.client.model.Credential;

import io.dropwizard.client.JerseyClientConfiguration;
import io.dropwizard.util.Duration;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

public class StandAloneClient {
    private static final ObjectMapper MAPPER = new ObjectMapper()
            .setPropertyNamingStrategy(
                    PropertyNamingStrategy.CAMEL_CASE_TO_LOWER_CASE_WITH_UNDERSCORES)
            .setSerializationInclusion(Include.NON_NULL);

    private final Options options;
    private final OptionGroup commandGroup;

    public StandAloneClient() {
        this.options = new Options();
        this.options.addOption(Option.builder("priv")
                .longOpt("private")
                .hasArg()
                .required()
                .desc("Private key file").build());
        this.options.addOption(Option.builder("pub")
                .longOpt("public")
                .hasArg()
                .required()
                .desc("Public key file").build());
        this.options.addOption(Option.builder("u")
                .longOpt("url")
                .hasArg()
                .required()
                .desc("Credential service URL").build());

        this.options.addOption(Option.builder("p")
                .longOpt("primary")
                .hasArg()
                .desc("Primary credential").build());
        this.options.addOption(Option.builder("s")
                .longOpt("secondary")
                .hasArg()
                .desc("Secondary credential").build());

        this.commandGroup = new OptionGroup();
        this.commandGroup.setRequired(true);
        this.commandGroup.addOption(Option.builder()
                .argName("get")
                .longOpt("get")
                .desc("Retrieves credentials from the server")
                .build());
        this.commandGroup.addOption(Option.builder()
                .argName("upload")
                .longOpt("upload")
                .desc("Uploads new credentials to the server")
                .build());
        this.commandGroup.addOption(Option.builder()
                .argName("update")
                .longOpt("update")
                .desc("Updates existing credentials at the server")
                .build());
        this.commandGroup.addOption(Option.builder()
                .argName("delete")
                .longOpt("delete")
                .desc("Deletes credentials from the server")
                .build());
        this.options.addOptionGroup(this.commandGroup);
    }

    public void run(final String[] args) throws NoSuchAlgorithmException, InvalidKeySpecException,
            FileNotFoundException, IOException, InvalidKeyException, NoSuchPaddingException,
            IllegalBlockSizeException, BadPaddingException {
        final CommandLineParser parser = new DefaultParser();
        CommandLine commandLine = null;
        try {
            commandLine = parser.parse(this.options, args);
        } catch (final ParseException e) {
            final String header = "Credential service client\n\n";
            final String footer = "\nPlease report issues at https://github.com/mtakaki/CredentialStorageService/issues";

            final HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp(" ", header, this.options, footer, true);
            System.exit(-1);
        }

        final File privateKeyFile = new File(commandLine.getOptionValue("priv"));
        final File publicKeyFile = new File(commandLine.getOptionValue("pub"));
        final String url = commandLine.getOptionValue("u");

        final JerseyClientConfiguration clientConfiguration = new JerseyClientConfiguration();
        // TODO Change these to parameters
        clientConfiguration.setTimeout(Duration.minutes(1L));
        clientConfiguration.setConnectionRequestTimeout(Duration.minutes(1L));
        clientConfiguration.setConnectionTimeout(Duration.minutes(1L));

        final CredentialStorageServiceClient client = new CredentialStorageServiceClient(
                privateKeyFile, publicKeyFile, url, clientConfiguration);
        try {
            if (commandLine.hasOption("get")) {
                final Credential credential = client.getCredential();
                credential.setSymmetricKey(null);
                System.out.println(MAPPER.writeValueAsString(credential));
            } else if (commandLine.hasOption("upload")) {
                validateResult(client.uploadNewCredential(buildCredential(commandLine)),
                        "Credential successfully uploaded!",
                        "Failed to upload credentials.");
            } else if (commandLine.hasOption("update")) {
                validateResult(client.updateCredential(buildCredential(commandLine)),
                        "Credential successfully updated!",
                        "Failed to update credentials.");
            } else {
                validateResult(client.deleteCredential(),
                        "Credential successfully deleted!",
                        "Failed to delete credentials.");
            }
            System.exit(0);
        } catch (final NotFoundException e) {
            System.out.println("Credentials not found.");
        } catch (final ServerErrorException e) {
            System.out.println("Server failed to process request. Please report an issue.");
        }
        System.exit(-1);
    }

    public static void main(final String[] args) throws NoSuchAlgorithmException,
            InvalidKeySpecException, FileNotFoundException, IOException, InvalidKeyException,
            NoSuchPaddingException, IllegalBlockSizeException, BadPaddingException {
        final Logger root = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
        root.setLevel(Level.WARN);

        final StandAloneClient client = new StandAloneClient();
        client.run(args);
    }

    private static Credential buildCredential(final CommandLine commandLine) {
        return Credential.builder()
                .primary(commandLine.getOptionValue("primary"))
                .secondary(commandLine.getOptionValue("secondary")).build();
    }

    private static void validateResult(final boolean success, final String successMessage,
            final String failureMessage) {
        if (success) {
            System.out.println(successMessage);
        } else {
            System.out.println(failureMessage);
            System.exit(-1);
        }
    }
}