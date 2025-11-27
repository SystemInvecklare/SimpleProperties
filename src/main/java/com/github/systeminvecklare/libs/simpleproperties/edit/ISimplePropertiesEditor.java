package com.github.systeminvecklare.libs.simpleproperties.edit;

import com.github.systeminvecklare.libs.simpleproperties.SimpleProperties;

public interface ISimplePropertiesEditor {
	void edit(SimpleProperties original, ISimplePropertiesEdit transaction);
}
