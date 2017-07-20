/*
 * Copyright 2013 the original author or authors.
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

package org.gradle.api.internal.changedetection.rules;

import com.google.common.base.Objects;
import org.gradle.api.tasks.incremental.InputFileDetails;
import org.gradle.internal.nativeintegration.filesystem.FileType;

import java.io.File;

public class FileChange implements TaskStateChange, InputFileDetails {
    private final String path;
    private final ChangeType change;
    private final String fileType;
    private final FileType previousType;
    private final FileType currentType;

    public static FileChange added(String path, String title) {
        return new FileChange(path, ChangeType.ADDED, title, null, null);
    }

    public static FileChange removed(String path, String title) {
        return new FileChange(path, ChangeType.REMOVED, title, null, null);
    }

    public static FileChange modified(String path, String title, FileType previousType, FileType currentType) {
        assert previousType != null;
        assert currentType != null;
        assert !(previousType == FileType.Missing && currentType == FileType.Missing);
        assert !(previousType == FileType.Directory && currentType == FileType.Directory);

        return new FileChange(path, ChangeType.MODIFIED, title, previousType, currentType);
    }

    private FileChange(String path, ChangeType change, String fileType, FileType previousType, FileType currentType) {
        this.path = path;
        this.change = change;
        this.fileType = fileType;
        this.previousType = previousType;
        this.currentType = currentType;
    }

    public String getMessage() {
        ChangeType displayedChange = change != ChangeType.MODIFIED ? change : displayedChangeType(previousType, currentType);
        return fileType + " file " + path + " " + displayedChange.describe() + ".";
    }

    private static ChangeType displayedChangeType(FileType previous, FileType current) {
        if (previous == FileType.Missing) {
            return ChangeType.ADDED;
        }
        if (current == FileType.Missing) {
            return ChangeType.REMOVED;
        }
        return ChangeType.MODIFIED;
    }

    @Override
    public String toString() {
        return getMessage();
    }

    public String getPath() {
        return path;
    }

    public File getFile() {
        return new File(path);
    }

    public ChangeType getType() {
        return change;
    }

    public boolean isAdded() {
        return change == ChangeType.ADDED;
    }

    public boolean isModified() {
        return change == ChangeType.MODIFIED;
    }

    public boolean isRemoved() {
        return change == ChangeType.REMOVED;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        FileChange that = (FileChange) o;
        return Objects.equal(path, that.path)
            && change == that.change
            && Objects.equal(fileType, that.fileType)
            && Objects.equal(previousType, that.previousType)
            && Objects.equal(currentType, that.currentType);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(path, change, fileType, previousType, currentType);
    }
}
