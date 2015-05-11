/*
 * Copyright 2014 the original author or authors.
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

package org.gradle.language.mirah.fixtures

import org.gradle.integtests.fixtures.jvm.IncrementalTestJvmComponent
import org.gradle.integtests.fixtures.jvm.JvmSourceFile
import org.gradle.language.mirah.MirahLanguageSourceSet
import org.gradle.test.fixtures.file.TestFile

class TestMirahComponent extends IncrementalTestJvmComponent {

    String languageName = "mirah"
    String sourceSetTypeName = MirahLanguageSourceSet.class.name

    List<JvmSourceFile> sources = [
            new JvmSourceFile("compile/test", "Person.mirah", '''
package compile.test

class Person(name: String, age: Integer) {
    override def toString(): String = name + ", " + age;
}'''),
            new JvmSourceFile("compile/test", "Person2.mirah", '''
package compile.test

class Person2 {
}
''')
    ]

    @Override
    void changeSources(List<TestFile> sourceFiles) {
        def personMirahFile = sourceFiles.find { it.name == "Person.mirah" }
        personMirahFile.text = personMirahFile.text.replace("name", "lastName")
    }

    @Override
    void writeAdditionalSources(TestFile testFile) {
        testFile.file("mirah/Extra.mirah") << """
object Extra {
  def someMethod(args: Array[String]) {
  }
}
"""

    }
}
