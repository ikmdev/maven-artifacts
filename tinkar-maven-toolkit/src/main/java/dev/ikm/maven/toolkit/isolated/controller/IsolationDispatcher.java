package dev.ikm.maven.toolkit.isolated.controller;

import dev.ikm.maven.toolkit.TinkarMojo;
import dev.ikm.maven.toolkit.isolated.entity.LogInstant;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Semaphore;

/**
 * Isolation Dispatcher starts a new JVM and serializes fields necessary to be run in new JVM instance
 */
public class IsolationDispatcher {

	Logger LOG = LoggerFactory.getLogger(IsolationDispatcher.class.getSimpleName());

	private final TinkarMojo tinkarMojo;
	private final IsolationFieldSerializer isolationFieldSerializer;
	private String classPath;
	private String canonicalName;
	private Path isolatedDirectory;
	private final Semaphore semaphore = new Semaphore(2);
	public static final int VERIFY_EXIT_CODE = 1;

	private IsolationDispatcher(Builder builder) {
		this.tinkarMojo = builder.tinkarMojo;
		this.classPath = builder.classPath;
		this.canonicalName = builder.canonicalName;
		this.isolatedDirectory = builder.isolatedDirectory;
		this.isolationFieldSerializer = new IsolationFieldSerializer(isolatedDirectory);
	}

	/**
	 * Dispatch new instance of JVM and run Mojo
	 */
	public void dispatch() throws MojoExecutionException, MojoFailureException  {
		isolationFieldSerializer.discoverIsolatedFields(tinkarMojo);
		isolationFieldSerializer.serializeFields();

		long maxMemory = Runtime.getRuntime().maxMemory();
		long maxInMB = maxMemory / 1024 / 1024;
		ProcessBuilder pb = new ProcessBuilder();
		List<String> command = new ArrayList<>();
		command.add(System.getProperty("java.home") + "/bin/java");
		command.add("-Dfile.encoding=UTF-8");
		command.add("-Dsun.stdout.encoding=UTF-8");
		command.add("-Dsun.stderr.encoding=UTF-8");
		command.add("-Xmx"+maxInMB+"m");
		command.add("-cp");
		command.add(classPath);
		command.add(canonicalName);
		command.add(isolatedDirectory.toString());
		command.add(canonicalName);
		pb.command(command);

		LOG.info("isolated dispatcher: " + command);

		try {
			Process process = pb.start();
			List<LogInstant> logInstants = logProcess(process);
			int exitCode = process.waitFor();
			semaphore.acquireUninterruptibly();
			logInstants.sort(Comparator.comparing(LogInstant::instant));
			logInstants.forEach(logInstant -> LOG.info(logInstant.message()));
			LOG.info("Process exited with code: " + exitCode);
			if (exitCode == VERIFY_EXIT_CODE) {
				throw new MojoExecutionException("Mojo Failed to Execute");
			}
		} catch (IOException | InterruptedException e) {
			e.printStackTrace();
		}

	}

	/**
	 * Async capture output streams from dispatched JVM process
	 * @param process - JVM process
	 * @return
	 * @throws IOException
	 */
	private List<LogInstant> logProcess(Process process) throws IOException {
		List<LogInstant> list = new ArrayList<>();
		final List<LogInstant> logInstants = Collections.synchronizedList(list);
		Thread.startVirtualThread(() -> {
			try (InputStream inputStream = process.getInputStream();
				 InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
				 BufferedReader bufferedReader = new BufferedReader(inputStreamReader)) {
				semaphore.acquire();
				String stdOut;
				while ((stdOut = bufferedReader.readLine()) != null) {
					logInstants.add(new LogInstant(stdOut, Instant.now()));
				}
			} catch (IOException | InterruptedException e) {
				throw new RuntimeException(e);
			} finally {
				semaphore.release();
			}
		});

		Thread.startVirtualThread(() -> {
			try (InputStream errorStream = process.getErrorStream();
				 InputStreamReader errorStreamReader = new InputStreamReader(errorStream);
				 BufferedReader errorBufferedReader = new BufferedReader(errorStreamReader)) {
				semaphore.acquire();
				String errorOut;
				while ((errorOut = errorBufferedReader.readLine()) != null) {
					logInstants.add(new LogInstant(errorOut, Instant.now()));
				}
			} catch (IOException | InterruptedException e) {
				throw new RuntimeException(e);
			} finally {
				semaphore.release();
			}
		});
		return logInstants;
	}

	public static class Builder {

		private TinkarMojo tinkarMojo;
		private Path isolatedDirectory;
		private String classPath;
		private List<File> dependencies;
		private Path buildDirectory;
		private String canonicalName;

		private final String suffix = ".if";


		public Builder clazz(TinkarMojo tinkarMojo) {
			this.tinkarMojo = tinkarMojo;
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

		/**
		 * Builder for creating an Isolation Dispatcher
		 * @return
		 */
		public IsolationDispatcher build() {
			Objects.requireNonNull(tinkarMojo);
			Objects.requireNonNull(buildDirectory);
			Objects.requireNonNull(dependencies);

			//Build Class Path string based on dependencies
			StringBuilder cpBuilder = new StringBuilder();
			String targetDirectory = buildDirectory.resolve("classes").toString();
			cpBuilder.append(targetDirectory).append(File.pathSeparator);
			for (int i = 0; i < dependencies.size(); i++) {
				File dependency = dependencies.get(i);
				String artifactPath = dependency.getAbsolutePath();
				if (i < dependencies.size() - 1) {
					cpBuilder.append(artifactPath).append(File.pathSeparator);
				} else {
					cpBuilder.append(artifactPath);
				}
			}
			classPath = cpBuilder.toString();

			Objects.requireNonNull(classPath);
			Objects.requireNonNull(canonicalName);

			//Create a directory for isolated fields
			LocalTime localTime = LocalTime.now();
			DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("HHmmss");
			isolatedDirectory = Path.of(buildDirectory.toString(), tinkarMojo.getClass().getSimpleName() + "-" + dateTimeFormatter.format(localTime));
			try {
				Files.createDirectories(isolatedDirectory);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
			return new IsolationDispatcher(this);
		}
	}
}
