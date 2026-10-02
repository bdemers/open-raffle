package org.openraffle.ui;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.info.BuildProperties;
import org.springframework.stereotype.Component;

/** The version from Maven's build-info; "dev" when running without one (e.g. from an IDE). */
@Component
public class AppVersion {

    private final String version;

    public AppVersion(ObjectProvider<BuildProperties> buildProperties) {
        BuildProperties props = buildProperties.getIfAvailable();
        this.version = props == null || props.getVersion() == null ? "dev" : props.getVersion();
    }

    public String get() {
        return version;
    }
}
