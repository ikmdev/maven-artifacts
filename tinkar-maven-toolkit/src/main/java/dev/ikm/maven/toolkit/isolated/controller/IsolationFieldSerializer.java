package dev.ikm.maven.toolkit.isolated.controller;

import dev.ikm.maven.toolkit.isolated.boundary.Isolate;
import dev.ikm.maven.toolkit.isolated.boundary.IsolatedTinkarMojo;
import dev.ikm.maven.toolkit.isolated.entity.IsolatedField;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

/**
 * Class focused on providing serialization and deserialization of discoverable fields
 */
public class IsolationFieldSerializer {

	private final Path isolatedFieldsDirectory;
	private final List<IsolatedField> isolatedFields;
	private final String suffix = ".if";

	public IsolationFieldSerializer(Path isolatedFieldsDirectory) {
		this.isolatedFieldsDirectory = isolatedFieldsDirectory;
		this.isolatedFields = new ArrayList<>();
	}

	/**
	 * Discover all fields that are annotated with @Isolate from concrete and parent classes
	 * @param isolatedTinkarMojo
	 */
	public void discoverIsolatedFields(IsolatedTinkarMojo isolatedTinkarMojo) {
		//Gather all fields from instance and parent classes
		ArrayList<Field> fields = new ArrayList<>();
		fields.addAll(Arrays.asList(isolatedTinkarMojo.getClass().getFields()));
		fields.addAll(Arrays.asList(isolatedTinkarMojo.getClass().getDeclaredFields()));

		//Iterate over all fields and find the ones that are okay to be passed into an isolated JVM
		isolatedFields.clear();
		isolatedFields.addAll(fields.stream()
				.filter(field -> {
					field.setAccessible(true);
					return field.isAnnotationPresent(Isolate.class);
				})
				.map(field -> {
					try {
						return new IsolatedField(field.getName(), field.get(isolatedTinkarMojo));
					} catch (IllegalAccessException e) {
						throw new RuntimeException(e);
					}
				})
				.toList());
	}

	/**
	 * Write out discovered fields
	 */
	public void serializeFields() {
		for (IsolatedField isolatedField : isolatedFields) {
			try (FileOutputStream fos = new FileOutputStream(this.isolatedFieldsDirectory.resolve(isolatedField.name()).toFile() + suffix);
				 ObjectOutputStream outputStream = new ObjectOutputStream(fos)) {
				outputStream.writeObject(isolatedField);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
	}

	/**
	 * Read in fields that were serialized
	 * @return
	 */
	public List<IsolatedField> deserializeIsolatedFields() {
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
}
