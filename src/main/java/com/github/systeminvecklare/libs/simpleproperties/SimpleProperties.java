package com.github.systeminvecklare.libs.simpleproperties;

import java.io.Console;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Scanner;
import java.util.function.Consumer;

import com.github.systeminvecklare.libs.simpleproperties.edit.ISimplePropertiesEdit;
import com.github.systeminvecklare.libs.simpleproperties.edit.ISimplePropertiesEditor;
import com.github.systeminvecklare.libs.simpleproperties.result.Result;
import com.github.systeminvecklare.libs.simpleproperties.validator.IPropertyValidator;

public class SimpleProperties {
	public static class Builder {
		private String programName;
		private File configPath;
		private boolean createConfigPathParentDir = true;
		private boolean printLoadedConfigPath = true;
		private boolean readOnly = false;
		private final Map<String, Property> properties = new LinkedHashMap<String, SimpleProperties.Property>();

		private Builder(String programName) {
			this.programName = programName;
			this.configPath = Paths.get(System.getProperty("user.home"), "."+programName, "config.properties").toFile();
		}
		
		public Builder setConfigFilePath(File configPath) {
			this.configPath = configPath;
			return this;
		}
		
		public Builder setConfigFileName(String fileName) {
			this.configPath = Paths.get(System.getProperty("user.home"), "."+programName, fileName).toFile();
			return this;
		}
		
		public Builder createConfigPathParentDir(boolean createConfigPathParentDir) {
			this.createConfigPathParentDir = createConfigPathParentDir;
			return this;
		}
		
		public Builder printLoadedConfigPath(boolean printLoadedConfigPath) {
			this.printLoadedConfigPath = printLoadedConfigPath;
			return this;
		}
		
		public Builder readOnly(boolean readOnly) {
			this.readOnly = readOnly;
			return this;
		}
		
		public Builder addProperty(String name, boolean required, IPropertyBuilder builder) {
			if(properties.containsKey(name)) {
				throw new IllegalStateException("A property with key \""+name+"\" has already beed added to this builder.");
			}
			properties.put(name, new Property(name, required, builder));
			return this;
		}
		
		public SimpleProperties build() throws IOException {
			if(createConfigPathParentDir) {
				Files.createDirectories(configPath.toPath().getParent());
			}
			Properties javaProperties = new Properties();
			if(configPath.exists()) {
				try(FileInputStream fis = new FileInputStream(configPath)) {
					javaProperties.load(fis);
					if(printLoadedConfigPath) {
						System.out.println("Loaded config file from "+configPath.getAbsolutePath());
					}
				}
			}
			
			Map<String, Property> copiedProperties = new LinkedHashMap<String, SimpleProperties.Property>();
			
			boolean edited = false;
			for(Entry<String, Property> entry : properties.entrySet()) {
				Property property = entry.getValue();
				boolean propertyValueNeeded = false;
				String valueInJavaProperties = javaProperties.getProperty(property.name);
				if(valueInJavaProperties == null) {
					propertyValueNeeded = property.required;
				} else {
					for(IPropertyValidator validator : property.validators) {
						try {
							if(validator != null) {
								String errorMessage = validator.validate(valueInJavaProperties);
								if(errorMessage != null) {
									System.out.println("Loaded value for property "+property.name+" was invalid:");
									System.out.println(errorMessage);
									System.out.println();
									if(!property.checkForSkip()) {
										propertyValueNeeded = true;
									}
									break;
								}
							}
						} catch(Exception e) {
							System.out.println("Validator "+validator.getClass().getName()+" failed to validate: "+e.getMessage());
							System.out.println();
							if(!property.checkForSkip()) {
								propertyValueNeeded = true;
							}
							break;
						}
					}
				}
				if(propertyValueNeeded) {
					String validValue = null;
					getValidValue: while(validValue == null) {
						String value = promptUserForValue(property.promptMessage+(property.promptMessage.endsWith(":") ? "" : ":"));
						for(IPropertyValidator validator : property.validators) {
							try {
								if(validator != null) {
									String errorMessage = validator.validate(value);
									if(errorMessage != null) {
										System.out.println(errorMessage);
										continue getValidValue;
									}
								}
							} catch(Exception e) {
								System.out.println("Validator "+validator.getClass().getName()+" failed to validate: "+e.getMessage());
								e.printStackTrace();
								continue getValidValue;
							}
						}
						validValue = value;
					}
					javaProperties.put(property.name, validValue);
					edited = true;
				}
				copiedProperties.put(entry.getKey(), property.copy());
			}
			
			SimpleProperties simpleProperties = new SimpleProperties(copiedProperties, javaProperties, configPath, readOnly);
			
			if(edited && !readOnly) {
				simpleProperties.save();
			}
			
			return simpleProperties;
		}

		public SimpleProperties buildUnchecked() {
			try {
				return build();
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
	}
	
	public static Builder builder(String programName) {
		return new Builder(programName);
	}

	private final Map<String, Property> properties;
	private Properties javaProperties;
	private final File configPath;
	private final boolean readOnly;

	private SimpleProperties(Map<String, Property> properties, Properties javaProperties, File configPath, boolean readOnly) {
		this.properties = properties;
		this.javaProperties = javaProperties;
		this.configPath = configPath;
		this.readOnly = readOnly;
	}
	
	/**
	 * Allows editing of this {@code SimpleProperties} in a transactional manner.
	 * <p>
	 * The provided {@link ISimplePropertiesEditor} receives a view of the properties
	 * and a transactional edit interface ({@link ISimplePropertiesEdit}) to apply changes.
	 * Each edit operation ({@link ISimplePropertiesEdit#setProperty} or
	 * {@link ISimplePropertiesEdit#clearProperty}) returns a {@link Result} that
	 * must be checked by the caller to detect validation failures or other issues.
	 * </p>
	 *
	 * <p>This method itself returns a {@link Result} indicating whether the edit
	 * completed successfully or if an external error occurred (for example, an
	 * exception thrown by the editor or a failed save to persistent storage).</p>
	 *
	 * <p><b>Example usage:</b></p>
	 * <pre>{@code
	 * Result result = simpleProperties.edit((original, transaction) -> {
	 *     for (String key : editableKeys) {
	 *         Result r = transaction.setProperty(key, "newValue");
	 *         if (!r.success) {
	 *             System.out.println("Failed to set " + key + ": " + r.errorMessage);
	 *         }
	 *     }
	 * });
	 * if (!result.success) {
	 *     System.err.println("Edit failed: " + result.errorMessage);
	 * }
	 * }</pre>
	 *
	 * @param editor the editor that performs property modifications
	 * @return a {@link Result} indicating success or external failure
	 */
	public Result edit(ISimplePropertiesEditor editor) {
		final Properties javaPropertiesClone = (Properties) javaProperties.clone();
		final boolean edited[] = new boolean[] {false};
		try {
			editor.edit(this, new ISimplePropertiesEdit() {
				@Override
				public Result setProperty(String name, String value) {
					Property property = getPropertyObject(name);
					for(IPropertyValidator validator : property.validators) {
						try {
							if(validator != null) {
								String errorMessage = validator.validate(value);
								if(errorMessage != null) {
									return Result.error("Failed to set property "+property.name+": "+errorMessage);
								}
							}
						} catch(Exception e) {
							return Result.error("Failed to set property "+property.name+": Validator "+validator.getClass().getName()+" failed to validate: "+e.getMessage());
						}
					}
					javaPropertiesClone.setProperty(name, value);
					edited[0] = true;
					return Result.success();
				}
				
				@Override
				public Result clearProperty(String name) {
					Property property = getPropertyObject(name);
					if(property.required) {
						return Result.error("Can not clear property "+property.name+" since it is required.");
					} else {
						javaPropertiesClone.remove(property.name);
						edited[0] = true;
						return Result.success();
					}
				}
			});
		} catch(RuntimeException e) {
			return Result.error(e.getMessage());
		}
		if(edited[0]) {
			javaProperties = javaPropertiesClone;
			if(!readOnly) {
				try {
					save();
				} catch(Exception e) {
					return Result.error("Failed to save to file: "+ e.getMessage());
				}
			}
		}
		return Result.success();
	}
	
	private void save() throws IOException {
		if(readOnly) {
			throw new UnsupportedOperationException(SimpleProperties.class.getSimpleName()+" is readOnly!");
		}
		try(FileOutputStream fos = new FileOutputStream(configPath)) {
			javaProperties.store(fos, null);
		}
	}

	/**
	 * Returns the value of the property with the given name.
	 * <p>
	 * Throws a {@link RuntimeException} if the property does not exist, or if it
	 * exists but has no value.
	 * </p>
	 *
	 * @param name the name of the property to retrieve
	 * @return the property value
	 * @throws RuntimeException if the property is unknown or has no value
	 */
	public String getProperty(String name) {
		String value = getProperty(name, null);
		if(value == null) {
			throw new RuntimeException("Optional property \""+name+"\" did not have a value.");
		}
		return value;
	}
	
	/**
	 * Returns the value of the property with the given name, or a fallback value
	 * if the property exists but is unset.
	 * <p>
	 * Throws a {@link RuntimeException} if the property does not exist.
	 * </p>
	 *
	 * @param name the name of the property to retrieve
	 * @param fallback the value to return if the property exists but has no value
	 * @return the property value, or {@code fallback} if the property is unset
	 * @throws RuntimeException if the property is unknown
	 */
	public String getProperty(String name, String fallback) {
		getPropertyObject(name);
		String value = javaProperties.getProperty(name);
		if(value == null) {
			return fallback;
		}
		return value;
	}
	
	private Property getPropertyObject(String name) {
		Property property = properties.get(name);
		if(property == null) {
			throw new RuntimeException("Unknown property: \""+name+"\"");
		}
		return property;
	}
	
	private static class Property {
		private final String name;
		private final boolean required;
		private String promptMessage;
		private final List<IPropertyValidator> validators = new ArrayList<IPropertyValidator>();
		
		public Property(String name, boolean required, IPropertyBuilder propertyBuilder) {
			this.name = name;
			this.required = required;
			this.promptMessage = "Please input value for property \""+name+"\"";
			propertyBuilder.build(new IPropertyConstruction() {
				@Override
				public void setPromptMessage(String message) {
					promptMessage = message;
				}
				
				@Override
				public void addValidator(IPropertyValidator propertyValidator) {
					validators.add(propertyValidator);
				}
			});
		}
		
		public boolean checkForSkip() {
			boolean skip = false;
			if(!required) {
				skip = promptUserForYesNo("Propery "+name+" is optional. Would you like to skip it?", skip);
			}
			return skip;
		}

		public Property copy() {
			Property copy = new Property(name, required, new IPropertyBuilder() {
				@Override
				public void build(IPropertyConstruction construction) {
				}
			});
			copy.promptMessage = this.promptMessage;
			copy.validators.addAll(validators);
			return copy;
		}
	}
	
	private static String promptUserForValue(String message) {
		System.out.println();
		Console console = System.console();
		String input = null;
		if(console != null) {
			input = console.readLine(message + "\n");
		} 
		if(console == null || input == null) {
			System.out.println(message);
			try (Scanner scanner = new Scanner(System.in)) {
				input = scanner.nextLine();
			}
		}
		if(input == null) {
			throw new RuntimeException("Could not find a way to read user input!");
		}
		return input.trim();
	}
	
	private static boolean promptUserForYesNo(String message, boolean defaultValue) {
		while(true) {
			String value = promptUserForValue(message+" ("+(defaultValue ? "Y" : "y")+"/"+(defaultValue ? "n" : "N")+"):").trim();
			if("".equals(value != null ? value.trim() : "")) {
				return defaultValue;
			}
			if("y".equalsIgnoreCase(value)) {
				return true;
			} else if("n".equalsIgnoreCase(value)) {
				return false;
			} else {
				System.out.println("Invalid choice: "+value);
				System.out.println("Please type 'y' for Yes or 'n' for No");
			}
		}
	}
}
