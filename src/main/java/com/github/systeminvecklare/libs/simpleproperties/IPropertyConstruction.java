package com.github.systeminvecklare.libs.simpleproperties;

import com.github.systeminvecklare.libs.simpleproperties.validator.IPropertyValidator;

public interface IPropertyConstruction {
	void setPromptMessage(String message);
	void addValidator(IPropertyValidator propertyValidator);
}
