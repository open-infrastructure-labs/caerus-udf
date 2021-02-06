package org.openinfralabs.caerus.s3client.commands;

import org.springframework.stereotype.Component;
import picocli.CommandLine;

import static picocli.CommandLine.Command;


@Component
@Command(name = "caerus_s3cli")
public class S3CliCommand implements Runnable {
    public static void main(String[] args) {
        CommandLine.run(new S3CliCommand(), args);
    }

    @Override
    public void run() {
        System.out.println("The Caerus s3 cli command");
    }
/*
    @Command(name = "get")
    public void getCommand() {
        System.out.println("get s3 object");
    }

    @Command(name = "put")
    public void putCommand() {
        System.out.println("put s3 object");
    }

    @Command(name = "list")
    public void listCommand() {
        System.out.println("list s3 objects");
    }

    @Command(name = "delete")
    public void deleteCommand() {
        System.out.println("delete s3 object");
    }

    @Command(name = "copy")
    public void copyCommand() {
        System.out.println("copy s3 object");
    }*/
}

