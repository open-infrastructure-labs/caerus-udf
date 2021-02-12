package org.openinfralabs.caerus.s3client;

import org.openinfralabs.caerus.s3client.commands.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.Banner;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import picocli.CommandLine;

@SpringBootApplication
public class S3clientApplication implements CommandLineRunner {

	public static void main(String[] args) {
		SpringApplication application = new SpringApplication(S3clientApplication.class);
		application.setBannerMode(Banner.Mode.OFF);
		application.run(args);
	}

	private S3CliCommand s3cliCommand;
	private GetCommand getCommand;
	private DeleteCommand deleteCommand;
	private ListCommand listCommand;
	private PutCommand putCommand;
	private CopyCommand copyCommand;

	@Autowired
	public S3clientApplication(S3CliCommand s3cliCommand, GetCommand getCommand, DeleteCommand deleteCommand,
							   ListCommand listCommand, PutCommand putCommand, CopyCommand copyCommand) {
		this.s3cliCommand = s3cliCommand;
		this.getCommand = getCommand;
		this.listCommand = listCommand;
		this.deleteCommand = deleteCommand;
		this.putCommand = putCommand;
		this.copyCommand = copyCommand;
	}
	@Override
	public void run(String... args) {
		CommandLine commandLine = new CommandLine(s3cliCommand);

		commandLine.addSubcommand("list", listCommand);
		commandLine.addSubcommand("put", putCommand);
		commandLine.addSubcommand("delete", deleteCommand);
		commandLine.addSubcommand("copy", copyCommand);
		commandLine.addSubcommand("get", getCommand);

		commandLine.parseWithHandler(new CommandLine.RunLast(), args);
	}

}
