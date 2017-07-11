/*
 * Copyright 2017 the original author or authors.
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

package org.gradle.workers.internal

import spock.lang.Specification

class DaemonForkOptionsTest extends Specification {
    def "is compatible with itself"() {
        def settings = new DaemonForkOptions("128m", "1g", ["-server", "-esa"], [new File("lib/lib1.jar"), new File("lib/lib2.jar")], ["foo.bar", "foo.baz"], KeepAliveMode.SESSION)

        expect:
        settings.isCompatibleWith(settings)
    }

    def "is compatible with same settings"() {
        def settings1 = new DaemonForkOptions("128m", "1g", ["-server", "-esa"], [new File("lib/lib1.jar"), new File("lib/lib2.jar")], ["foo.bar", "foo.baz"], KeepAliveMode.SESSION)
        def settings2 = new DaemonForkOptions("128m", "1g", ["-server", "-esa"], [new File("lib/lib1.jar"), new File("lib/lib2.jar")], ["foo.bar", "foo.baz"], KeepAliveMode.SESSION)

        expect:
        settings1.isCompatibleWith(settings2)
    }


    def "is compatible with different representation of same memory requirements"() {
        def settings1 = new DaemonForkOptions("1024m", "2g", [], KeepAliveMode.SESSION)
        def settings2 = new DaemonForkOptions("1g", "2048m", [], KeepAliveMode.SESSION)

        expect:
        settings1.isCompatibleWith(settings2)
    }

    def "is compatible with lower memory requirements"() {
        def settings1 = new DaemonForkOptions("128m", "1g", [], KeepAliveMode.SESSION)
        def settings2 = new DaemonForkOptions("64m", "512m", [], KeepAliveMode.SESSION)

        expect:
        settings1.isCompatibleWith(settings2)
    }

    def "is not compatible with higher memory requirements"() {
        def settings1 = new DaemonForkOptions("128m", "1g", [], KeepAliveMode.SESSION)
        def settings2 = new DaemonForkOptions("256m", "512m", [], KeepAliveMode.SESSION)

        expect:
        !settings1.isCompatibleWith(settings2)
    }

    def "is compatible with same set of JVM args"() {
        def settings1 = new DaemonForkOptions(null, null, ["-server", "-esa"], KeepAliveMode.SESSION)
        def settings2 = new DaemonForkOptions(null, null, ["-esa", "-server"], KeepAliveMode.SESSION)

        expect:
        settings1.isCompatibleWith(settings2)
    }

    def "is compatible with subset of JVM args"() {
        def settings1 = new DaemonForkOptions(null, null, ["-server", "-esa"], KeepAliveMode.SESSION)
        def settings2 = new DaemonForkOptions(null, null, ["-server"], KeepAliveMode.SESSION)

        expect:
        settings1.isCompatibleWith(settings2)
    }

    def "is not compatible with different set of JVM args"() {
        def settings1 = new DaemonForkOptions(null, null, ["-server", "-esa"], KeepAliveMode.SESSION)
        def settings2 = new DaemonForkOptions(null, null, ["-client", "-esa"], KeepAliveMode.SESSION)

        expect:
        !settings1.isCompatibleWith(settings2)
    }

    def "is compatible with same class path"() {
        def settings1 = new DaemonForkOptions(null, null, [], [new File("lib/lib1.jar"), new File("lib/lib2.jar")], [], KeepAliveMode.SESSION)
        def settings2 = new DaemonForkOptions(null, null, [], [new File("lib/lib1.jar"), new File("lib/lib2.jar")], [], KeepAliveMode.SESSION)

        expect:
        settings1.isCompatibleWith(settings2)
    }

    def "is compatible with subset of class path"() {
        def settings1 = new DaemonForkOptions(null, null, [], [new File("lib/lib1.jar"), new File("lib/lib2.jar")], [], KeepAliveMode.SESSION)
        def settings2 = new DaemonForkOptions(null, null, [], [new File("lib/lib1.jar")], [], KeepAliveMode.SESSION)

        expect:
        settings1.isCompatibleWith(settings2)
    }

    def "is not compatible with different class path"() {
        def settings1 = new DaemonForkOptions(null, null, [], [new File("lib/lib1.jar"), new File("lib/lib2.jar")], [], KeepAliveMode.SESSION)
        def settings2 = new DaemonForkOptions(null, null, [], [new File("lib/lib1.jar"), new File("lib/lib3.jar")], [], KeepAliveMode.SESSION)

        expect:
        !settings1.isCompatibleWith(settings2)
    }

    def "is compatible with same set of shared packages"() {
        def settings1 = new DaemonForkOptions(null, null, [], [], ["foo.bar", "foo.baz"], KeepAliveMode.SESSION)
        def settings2 = new DaemonForkOptions(null, null, [], [], ["foo.bar", "foo.baz"], KeepAliveMode.SESSION)

        expect:
        settings1.isCompatibleWith(settings2)
    }

    def "is compatible with subset of shared packages"() {
        def settings1 = new DaemonForkOptions(null, null, [], [], ["foo.bar", "foo.baz"], KeepAliveMode.SESSION)
        def settings2 = new DaemonForkOptions(null, null, [], [], ["foo.baz"], KeepAliveMode.SESSION)

        expect:
        settings1.isCompatibleWith(settings2)
    }

    def "is not compatible with different set of shared packages"() {
        def settings1 = new DaemonForkOptions(null, null, [], [], ["foo.bar", "foo.baz"], KeepAliveMode.SESSION)
        def settings2 = new DaemonForkOptions(null, null, [], [], ["foo.pic", "foo.baz"], KeepAliveMode.SESSION)

        expect:
        !settings1.isCompatibleWith(settings2)
    }

    def "is compatible with same keep alive modes"() {
        def settings1 = new DaemonForkOptions(null, null, [], [], [], KeepAliveMode.SESSION)
        def settings2 = new DaemonForkOptions(null, null, [], [], [], KeepAliveMode.SESSION)

        expect:
        settings1.isCompatibleWith(settings2)
    }

    def "is not compatible with different keep alive modes"() {
        def settings1 = new DaemonForkOptions(null, null, [], [], [], KeepAliveMode.SESSION)
        def settings2 = new DaemonForkOptions(null, null, [], [], [], KeepAliveMode.DAEMON)

        expect:
        !settings1.isCompatibleWith(settings2)
    }

    def "string values are trimmed"() {
        def settings1 = new DaemonForkOptions("128m ", "1g", [" -server", "-esa"], KeepAliveMode.SESSION)
        def settings2 = new DaemonForkOptions("128m", " 1g", ["-server", "-esa "], KeepAliveMode.SESSION)

        expect:
        settings1.isCompatibleWith(settings2)
    }

    def "capitalization of memory options is irrelevant"() {
        def settings1 = new DaemonForkOptions("128M", "1g", [], KeepAliveMode.SESSION)
        def settings2 = new DaemonForkOptions("128m", "1G", [], KeepAliveMode.SESSION)

        expect:
        settings1.isCompatibleWith(settings2)
    }

    def "capitalization of JVM args is relevant"() {
        def settings1 = new DaemonForkOptions("128M", "1g", ["-Server", "-esa"], KeepAliveMode.SESSION)
        def settings2 = new DaemonForkOptions("128M", "1g", ["-server", "-esa"], KeepAliveMode.SESSION)

        expect:
        !settings1.isCompatibleWith(settings2)
    }

    def "unspecified class path and shared packages default to empty list"() {
        when:
        def options = new DaemonForkOptions("128m", "1g", ["-server", "-esa"], KeepAliveMode.SESSION)

        then:
        options.classpath == []
        options.sharedPackages == []
    }

    def "unspecified memory options are only compatible with unspecified memory options"() {
        def settings1 = new DaemonForkOptions(null, null, [], KeepAliveMode.SESSION)
        def settings2 = new DaemonForkOptions(null, null, [], KeepAliveMode.SESSION)
        def settings3 = new DaemonForkOptions("8m", "64m", [], KeepAliveMode.SESSION)

        expect:
        settings1.isCompatibleWith(settings2)
        !settings1.isCompatibleWith(settings3)
    }
}
