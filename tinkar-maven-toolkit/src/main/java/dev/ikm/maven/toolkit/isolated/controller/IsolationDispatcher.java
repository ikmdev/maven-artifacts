package dev.ikm.maven.toolkit.isolated.controller;

import dev.ikm.maven.toolkit.isolated.boundary.IsolatedTinkarMojo;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class IsolationDispatcher {

	private final IsolatedTinkarMojo isolatedTinkarMojo;
	private final IsolationFieldSerializer isolationFieldSerializer;
	private String classPath;
	private String canonicalName;
	private Path isolatedDirectory;

	private IsolationDispatcher(Builder builder) {
		this.isolatedTinkarMojo = builder.isolatedTinkarMojo;
		this.classPath = builder.classPath;
		this.canonicalName = builder.canonicalName;
		this.isolatedDirectory = builder.isolatedDirectory;
		this.isolationFieldSerializer = new IsolationFieldSerializer(isolatedDirectory);
	}

	public void dispatch() {
		isolationFieldSerializer.discoverIsolatedFields(isolatedTinkarMojo);
		isolationFieldSerializer.serializeFields();

		ProcessBuilder pb = new ProcessBuilder();
		List<String> command = new ArrayList<>();
		command.add(System.getProperty("java.home") + "/bin/java");
		command.add("-Dfile.encoding=UTF-8");
		command.add("-Dsun.stdout.encoding=UTF-8");
		command.add("-Dsun.stderr.encoding=UTF-8");
		command.add("-cp");
		command.add(classPath);
		command.add(canonicalName);
		command.add(isolatedDirectory.toString());
		command.add(canonicalName);
		pb.command(command);

		try {
			Process process = pb.start();
			try (InputStream inputStream = process.getInputStream();
				 InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
				 BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
				 InputStream errorStream = process.getErrorStream();
				 InputStreamReader errorStreamReader = new InputStreamReader(errorStream);
				 BufferedReader errorBufferedReader = new BufferedReader(errorStreamReader)) {

				String stdOut;
				while ((stdOut = bufferedReader.readLine()) != null) {
					System.out.println("Stdout: " + stdOut);
				}

				String errorOut;
				while ((errorOut = errorBufferedReader.readLine()) != null) {
					System.out.println("Error: " + errorOut);
				}

				// Wait for the process to complete
				int exitCode = process.waitFor();
				System.out.println("Process exited with code: " + exitCode);

			} catch (IOException | InterruptedException e) {
				e.printStackTrace();
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}


	public static class Builder {

		private IsolatedTinkarMojo isolatedTinkarMojo;
		private Path isolatedDirectory;
		private String classPath;
		private List<File> dependencies;
		private Path buildDirectory;
		private String canonicalName;

		private final String suffix = ".if";


		public Builder clazz(IsolatedTinkarMojo isolatedTinkarMojo) {
			this.isolatedTinkarMojo = isolatedTinkarMojo;
			return this;
		}


		public Builder dependencies(List<File> dependencies) {
			this.dependencies = dependencies;
			return this;
		}

		public Builder buildDirectory(Path buildDirectory) {
			this.buildDirectory = buildDirectory;
			return this;
		}

		public Builder canonicalName(String canonicalName) {
			this.canonicalName = canonicalName;
			return this;
		}

		public IsolationDispatcher build() {
			Objects.requireNonNull(isolatedTinkarMojo);
			Objects.requireNonNull(buildDirectory);
			Objects.requireNonNull(dependencies);

			//Build Class Path string based on dependencies
			StringBuilder cpBuilder = new StringBuilder();
			String targetDirectory = buildDirectory.resolve("classes").toString();
			cpBuilder.append(targetDirectory).append(":");
			for (int i = 0; i < dependencies.size(); i++) {
				File dependency = dependencies.get(i);
				String artifactPath = dependency.getAbsolutePath();
				if (i < dependencies.size() - 1) {
					cpBuilder.append(artifactPath).append(":");
				} else {
					cpBuilder.append(artifactPath);
				}
			}
			classPath = cpBuilder.toString();

			Objects.requireNonNull(classPath);
			Objects.requireNonNull(canonicalName);

			//Create a directory for isolated fields
			LocalTime localTime = LocalTime.now();
			DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");
			isolatedDirectory = Path.of(buildDirectory.toString(), dateTimeFormatter.format(localTime) + "-" + canonicalName);
			try {
				Files.createDirectories(isolatedDirectory);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
			return new IsolationDispatcher(this);
		}
	}
}
