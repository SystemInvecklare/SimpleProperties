package com.github.systeminvecklare.libs.simpleproperties.edit;

import com.github.systeminvecklare.libs.simpleproperties.result.Result;

public interface ISimplePropertiesEdit {
	Result setProperty(String name, String value);
	Result clearProperty(String name);
}
