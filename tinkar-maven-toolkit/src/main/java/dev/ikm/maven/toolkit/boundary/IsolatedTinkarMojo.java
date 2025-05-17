package dev.ikm.maven.toolkit.boundary;

import dev.ikm.maven.toolkit.entity.IsolatedField;
import dev.ikm.tinkar.common.service.ServiceProperties;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

public abstract class IsolatedTinkarMojo extends TinkarMojo {

	@Parameter(defaultValue = "${project}", readonly = true, required = true)
	private MavenProject project;

	@Parameter(readonly = true, defaultValue = "${plugin.artifacts}")
	protected List<Artifact> pluginDependencies;

	@Parameter(name = "targetDir", defaultValue = "${project.build.directory}")
	public File targetDir;

	private static String suffix = ".if";

	@Override
	public void execute() {
		getLog().info("maven: " + ServiceProperties.jvmUuid());
		// Create isolated directory for all serialized objects to be used in separate jvm
		String paramDirectoryPostfix = getClass().getSimpleName();
		Path isolatedFieldsDirectory = buildParametersDirectory(paramDirectoryPostfix);

		// Get all dependencies need for classpath invoked via an isolated jvm process
		String classPath = buildClassPathString();
		String canonicalName = getClass().getCanonicalName();

		//Allow concrete isolated mojo instance to set non-serializable fields to be passed to the isolated jvm
		initIsolatedFields();

		//Discover and serialize @Isolate fields
		List<IsolatedField> isolatedFields = discoverIsolatedFields();
		serializeFields(isolatedFieldsDirectory, isolatedFields);

		execMain(classPath, canonicalName, isolatedFieldsDirectory);
	}

	public abstract void initIsolatedFields();

	private Path buildParametersDirectory(String paramDirectoryPostfix) {
		LocalTime localTime = LocalTime.now();
		DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");
		String prefix = localTime.format(dateTimeFormatter);
		Path paramsPath = Path.of(targetDir.getPath(), prefix + "-" + paramDirectoryPostfix);
		try {
			Files.createDirectories(paramsPath);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		return paramsPath;
	}

	private String buildClassPathString() {
		StringBuilder cpBuilder = new StringBuilder();
		String targetDirectory = project.getBuild().getDirectory() + "/classes";
		cpBuilder.append(targetDirectory).append(":");

		for (int i = 0; i < pluginDependencies.size(); i++) {
			Artifact artifact = pluginDependencies.get(i);
			String artifactPath = artifact.getFile().getAbsolutePath();
			if (i < pluginDependencies.size() - 1) {
				cpBuilder.append(artifactPath).append(":");
			} else {
				cpBuilder.append(artifactPath);
			}
		}
		return cpBuilder.toString();
	}

	private List<IsolatedField> discoverIsolatedFields() {
		//Gather all fields from instance and parent classes
		ArrayList<Field> fields = new ArrayList<>();
		fields.addAll(Arrays.asList(getClass().getFields()));
		fields.addAll(Arrays.asList(getClass().getDeclaredFields()));

		//Iterate over all fields and find the ones that are okay to be passed into an isolated JVM
		return fields.stream()
				.filter(field -> {
					field.setAccessible(true);
					return field.isAnnotationPresent(Isolate.class);
				})
				.map(field -> {
					try {
						return new IsolatedField(field.getName(), field.get(this));
					} catch (IllegalAccessException e) {
						throw new RuntimeException(e);
					}
				})
				.toList();
	}

	private void serializeFields(Path mavenParametersDirectory, List<IsolatedField> isolatedFields) {
		for (IsolatedField isolatedField : isolatedFields) {
			try (FileOutputStream fos = new FileOutputStream(mavenParametersDirectory.resolve(isolatedField.name()).toFile() + suffix);
				 ObjectOutputStream outputStream = new ObjectOutputStream(fos)) {
				outputStream.writeObject(isolatedField);
			} catch (IOException e) {
				getLog().error(e);
				throw new RuntimeException(e);
			}
		}
	}

	private void execMain(String cp, String canonicalName, Path mavenParametersDirectory) {
		ProcessBuilder pb = new ProcessBuilder();
		List<String> command = new ArrayList<>();
		command.add(System.getProperty("java.home") + "/bin/java");
		command.add("-Dfile.encoding=UTF-8");
		command.add("-Dsun.stdout.encoding=UTF-8");
		command.add("-Dsun.stderr.encoding=UTF-8");
		command.add("-cp");
		command.add(cp);
		command.add(canonicalName);
		command.add(mavenParametersDirectory.toString());
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

	public static void main(String... args) throws Exception {
		System.out.println("fork: " + ServiceProperties.jvmUuid());
		if (args.length < 2) {
			throw new IllegalArgumentException("Need to specify maven parameter directory");
		}
		Path isolatedFieldsDirectory = Path.of(args[0]);
		System.out.println("Maven parameters directory: " + isolatedFieldsDirectory);
		String canonicalName = args[1];

		//TODO-aks8m: Move the below methods into a helper class
		List<IsolatedField> f = deserializeIsolatedFields(isolatedFieldsDirectory);
		IsolatedTinkarMojo isolatedTinkarMojo = newIsolatedTinkarMojo(canonicalName);
		injectIsolatedFields(isolatedTinkarMojo, f);
		isolatedTinkarMojo.run();
	}

	private static List<IsolatedField> deserializeIsolatedFields(Path isolatedFieldsDirectory) {
		ArrayList<IsolatedField> isolatedFields = new ArrayList<>();

		try (Stream<Path> paths = Files.walk(isolatedFieldsDirectory)) {
			paths.filter(path -> !path.toFile().isDirectory() && path.getFileName().toString().endsWith(suffix))
					.forEach(path -> {
						try (FileInputStream fis = new FileInputStream(path.toFile());
							 ObjectInputStream inputStream = new ObjectInputStream(fis)) {
							IsolatedField isolatedField = (IsolatedField) inputStream.readObject();
							isolatedFields.add(isolatedField);
						} catch (IOException e) {
							throw new RuntimeException(e);
						} catch (ClassNotFoundException e) {
							throw new RuntimeException(e);
						}
					});
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

		return isolatedFields;
	}

	private static IsolatedTinkarMojo newIsolatedTinkarMojo(String canonicalName) {
		try {
			Class<IsolatedTinkarMojo> isolatedTinkarMojoClass = (Class<IsolatedTinkarMojo>) Class.forName(canonicalName);
			return isolatedTinkarMojoClass.getDeclaredConstructor(null).newInstance(null);
		} catch (InstantiationException | InvocationTargetException | IllegalAccessException | NoSuchMethodException |
				 ClassNotFoundException e) {
			throw new RuntimeException(e);
		}
	}

	private static void injectIsolatedFields(IsolatedTinkarMojo isolatedTinkarMojo, List<IsolatedField> isolatedFields) {
		//Inject Isolated Fields into class fields (super)
		Class<?> clazz =  isolatedTinkarMojo.getClass();
		for (Field field : clazz.getFields()) {
			isolatedFields.forEach(isolatedField -> {
				if (isolatedField.name().equals(field.getName())) {
					field.setAccessible(true);
					try {
						field.set(isolatedTinkarMojo, isolatedField.object());
					} catch (IllegalAccessException e) {
						throw new RuntimeException(e);
					}
				}
			});
		}
		//Inject Isolated Fields into class fields (instance)
		for (Field field : clazz.getDeclaredFields()) {
			isolatedFields.forEach(isolatedField -> {
				if (isolatedField.name().equals(field.getName())) {
					field.setAccessible(true);
					try {
						field.set(isolatedTinkarMojo, isolatedField.object());
					} catch (IllegalAccessException e) {
						throw new RuntimeException(e);
					}
				}
			});
		}
	}

}
