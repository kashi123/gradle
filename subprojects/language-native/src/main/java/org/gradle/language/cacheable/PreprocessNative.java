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

import org.gradle.api.Action;
import org.gradle.api.file.EmptyFileVisitor;
import org.gradle.api.file.FileVisitDetails;
import org.gradle.api.internal.changedetection.changes.IncrementalTaskInputsInternal;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.incremental.IncrementalTaskInputs;
import org.gradle.internal.UncheckedException;
import org.gradle.process.ExecSpec;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

public class PreprocessNative extends AbstractNativeTask {
    private File preprocessedSourcesDir;
    private final PreprocessedCFileParser parser = new PreprocessedCFileParser();

    @OutputDirectory
    public File getPreprocessedSourcesDir() {
        return preprocessedSourcesDir;
    }

    public void setPreprocessedSourcesDir(File preprocessedSourcesDir) {
        this.preprocessedSourcesDir = preprocessedSourcesDir;
    }

    @TaskAction
    public void preprocess(IncrementalTaskInputs incrementalTaskInputs) {
        final ConcurrentHashMap<String, Boolean> seenIncludes = new ConcurrentHashMap<String, Boolean>();
        final ConcurrentHashMap<String, Boolean> discoveredInputFiles = new ConcurrentHashMap<String, Boolean>();
        final IncrementalTaskInputsInternal inputs = (IncrementalTaskInputsInternal) incrementalTaskInputs;
        final File headersFile = getProject().file("src/main/headers");
        getSource().visit(new EmptyFileVisitor() {
            @Override
            public void visitFile(final FileVisitDetails fileVisitDetails) {
                String name = fileVisitDetails.getName();
                if (!(name.endsWith(".cpp") || name.endsWith(".c"))) {
                    return;
                }
                String extension = name.endsWith(".cpp") ? "cpp" : "c";
                String base = name.replaceAll("\\.c(pp)?", "");
                String preprocessedName = base + (extension.equals("cpp") ? ".ii" : ".i");
                final File preprocessedFile = fileVisitDetails.getRelativePath().getParent().append(true, preprocessedName).getFile(getPreprocessedSourcesDir());
                preprocessedFile.getParentFile().mkdirs();
                getProject().exec(new Action<ExecSpec>() {
                    @Override
                    public void execute(ExecSpec execSpec) {
                        configureExec(execSpec);
                        execSpec.args("-E",
                            "-I", headersFile,
                            "-o", preprocessedFile.getAbsolutePath(),
                            fileVisitDetails.getFile().getAbsolutePath()
                        );
                    }
                });
                discoverIncludes(inputs, preprocessedFile, seenIncludes, discoveredInputFiles);
            }
        });
    }

    private void discoverIncludes(final IncrementalTaskInputsInternal inputs, File preprocessedFile, final ConcurrentHashMap<String, Boolean> seenIncludes, final ConcurrentHashMap<String, Boolean> discoveredInputFiles) {
        parser.parseFile(preprocessedFile, new Action<String>() {
            @Override
            public void execute(String foundInclude) {
                if (seenIncludes.putIfAbsent(foundInclude, Boolean.TRUE) == null) {
                    try {
                        File canonicalFile = new File(foundInclude).getCanonicalFile();
                        if (discoveredInputFiles.putIfAbsent(canonicalFile.getAbsolutePath(), Boolean.TRUE) == null) {
                            inputs.newInput(canonicalFile);
                        }
                    } catch (IOException e) {
                        throw UncheckedException.throwAsUncheckedException(e);
                    }
                }

            }
        });
    }

}
