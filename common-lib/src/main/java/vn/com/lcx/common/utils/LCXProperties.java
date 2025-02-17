package vn.com.lcx.common.utils;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import vn.com.lcx.common.constant.Constant;

import java.util.Properties;

@AllArgsConstructor
@NoArgsConstructor
public class LCXProperties {
    private YamlProperties yamlProperties;
    private Properties properties;

    public String getProperty(String key) {
        if (properties != null) {
            return properties.getProperty(key);
        } else if (yamlProperties != null) {
            return yamlProperties.getProperty(key);
        }
        return Constant.EMPTY_STRING;
    }

}
