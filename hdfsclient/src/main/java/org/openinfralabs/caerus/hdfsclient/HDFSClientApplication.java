package org.openinfralabs.caerus.hdfsclient;

import org.openinfralabs.caerus.hdfsclient.commands.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.Banner;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import picocli.CommandLine;

@SpringBootApplication
public class HDFSClientApplication implements CommandLineRunner {

    public static void main(String[] args) {
        SpringApplication application = new SpringApplication(HDFSClientApplication.class);
        application.setBannerMode(Banner.Mode.OFF);
        application.run(args);
    }

    private HdfsCliCommand hdfsCliCommand;
    private GetCommand getCommand;
    private DeleteCommand deleteCommand;
    private ListCommand listCommand;
    private PutCommand putCommand;
    private CopyCommand copyCommand;

    @Autowired
    public HDFSClientApplication(HdfsCliCommand hdfsCliCommand, GetCommand getCommand, DeleteCommand deleteCommand,
                                 ListCommand listCommand, PutCommand putCommand, CopyCommand copyCommand) {
        this.hdfsCliCommand = hdfsCliCommand;
        this.getCommand = getCommand;
        this.listCommand = listCommand;
        this.deleteCommand = deleteCommand;
        this.putCommand = putCommand;
        this.copyCommand = copyCommand;
    }

    @Override
    public void run(String... args) {
        CommandLine commandLine = new CommandLine(hdfsCliCommand);

        commandLine.addSubcommand("list", listCommand);
        commandLine.addSubcommand("put", putCommand);
        commandLine.addSubcommand("delete", deleteCommand);
        commandLine.addSubcommand("copy", copyCommand);
        commandLine.addSubcommand("get", getCommand);

        commandLine.parseWithHandler(new CommandLine.RunLast(), args);
    }

}
