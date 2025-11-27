package com.github.systeminvecklare.libs.simpleproperties.validator;

import java.io.File;

public class PropertyValidators {
	private PropertyValidators() {}
	
	public static final IPropertyValidator NOT_EMPTY = new IPropertyValidator() {
		@Override
		public String validate(String input) {
			if("".equals(input.trim())) {
				return "is empty";
			}
			return null;
		}
	};
	
	public static final IPropertyValidator IS_FILE = new IPropertyValidator() {
		@Override
		public String validate(String input) {
			if(!new File(input).isFile()) {
				return "not a file";
			}
			return null;
		}
	};
	
	public static final IPropertyValidator IS_DIRECTORY = new IPropertyValidator() {
		@Override
		public String validate(String input) {
			if(!new File(input).isDirectory()) {
				return "not a directory";
			}
			return null;
		}
	};
	
	public static final IPropertyValidator PATH_EXISTS = new IPropertyValidator() {
		@Override
		public String validate(String input) {
			if(!new File(input).exists()) {
				return "does not exist";
			}
			return null;
		}
	};
	
	public static final IPropertyValidator PARENT_PATH_EXISTS = new IPropertyValidator() {
		@Override
		public String validate(String input) {
			File parentFile = new File(input).getParentFile();
			if(parentFile == null) {
				return "does not have a parent directory";
			}
			if(!parentFile.exists()) {
				return "parent directory does not exist";
			}
			return null;
		}
	};
}
