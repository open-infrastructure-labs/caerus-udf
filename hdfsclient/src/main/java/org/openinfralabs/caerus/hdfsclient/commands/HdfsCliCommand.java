package org.openinfralabs.caerus.hdfsclient.commands;

import org.springframework.stereotype.Component;
import picocli.CommandLine;

import static picocli.CommandLine.Command;


@Component
@Command(name = "caerus_hdfscli")
public class HdfsCliCommand implements Runnable {
    public static void main(String[] args) {
        CommandLine.run(new HdfsCliCommand(), args);
    }

    @Override
    public void run() {
        System.out.println("The Caerus HDFS cli command");
    }
/*
    @Command(name = "get")
    public void getCommand() {
        System.out.println("get object from HDFS");
    }

    @Command(name = "put")
    public void putCommand() {
        System.out.println("put object to HDFS");
    }

    @Command(name = "list")
    public void listCommand() {
        System.out.println("list objects on HDFS");
    }

    @Command(name = "delete")
    public void deleteCommand() {
        System.out.println("delete object from HDFS");
    }

    @Command(name = "copy")
    public void copyCommand() {
        System.out.println("copy object to HDFS");
    }*/
}

