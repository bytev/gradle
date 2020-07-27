/*
 * Copyright 2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gradle.jvm.toolchain.install.internal;

import net.rubygrapefruit.platform.SystemInfo;
import org.gradle.internal.os.OperatingSystem;
import org.gradle.jvm.toolchain.JavaToolchainSpec;

import javax.inject.Inject;
import java.net.URI;

public class AdoptOpenJdkRemoteBinary {

    private final SystemInfo systemInfo;
    private final OperatingSystem operatingSystem;

    @Inject
    public AdoptOpenJdkRemoteBinary(SystemInfo systemInfo, OperatingSystem operatingSystem) {
        this.systemInfo = systemInfo;
        this.operatingSystem = operatingSystem;
    }

    public URI getUri(JavaToolchainSpec spec) {
        return URI.create(getServerBaseUri() +
            "v3/binary/latest/" + spec.getLanguageVersion().get().getMajorVersion() +
            "/" +
            determineReleaseState() +
            "/" +
            determineOs() +
            "/" +
            determineArch() +
            "/jdk/hotspot/normal/adoptopenjdk");
    }

    private String determineArch() {
        switch (systemInfo.getArchitecture()) {
            case i386:
                return "x32";
            case amd64:
                return "x64";
            case aarch64:
                return "aarch64";
        }
        return systemInfo.getArchitectureName();
    }

    private String determineOs() {
        if (operatingSystem.isWindows()) {
            return "windows";
        } else if (operatingSystem.isMacOsX()) {
            return "mac";
        } else if (operatingSystem.isLinux()) {
            return "linux";
        }
        return operatingSystem.getFamilyName();
    }

    // TODO: [bm] determine using API?
    // https://github.com/AdoptOpenJDK/openjdk-api-v3/issues/256
    private String determineReleaseState() {
        return "ga";
    }

    private String getServerBaseUri() {
        String baseUri = System.getProperty("org.gradle.jvm.toolchain.install.internal.adoptopenjdk.baseUri", "https://api.adoptopenjdk.net/");
        if (!baseUri.endsWith("/")) {
            baseUri += "/";
        }
        return baseUri;
    }

}
