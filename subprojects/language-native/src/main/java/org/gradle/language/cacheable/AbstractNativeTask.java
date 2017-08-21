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

package org.gradle.language.cacheable;

import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.SourceTask;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class AbstractNativeTask extends SourceTask {
    protected List<String> compilerOptions = new ArrayList<String>();
    private File gccExecutable;
    private List<File> includeRoots = new ArrayList<File>();

    @Input
    public File getGccExecutable() {
        return gccExecutable;
    }

    public void setGccExecutable(File gccExecutable) {
        this.gccExecutable = gccExecutable;
    }

    @Internal
    public List<File> getIncludeRoots() {
        return includeRoots;
    }

    public void setIncludeRoots(List<File> includeRoots) {
        this.includeRoots = includeRoots;
    }

    protected List<String> args(String... additionalArgs) {
        ArrayList<String> result = new ArrayList<String>();
        result.addAll(getCompilerOptions());
        result.add("-m64");
        for (File header : includeRoots) {
            result.add("-I" + header.getAbsolutePath());
        }
        result.addAll(Arrays.asList(additionalArgs));
        return result;
    }

    public List<String> getCompilerOptions() {
        return compilerOptions;
    }
}
