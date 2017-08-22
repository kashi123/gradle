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

package org.gradle.language.cpp.plugins;

import org.gradle.api.Action;
import org.gradle.api.Incubating;
import org.gradle.api.Plugin;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.attributes.Usage;
import org.gradle.api.file.DirectoryVar;
import org.gradle.api.internal.project.ProjectInternal;
import org.gradle.api.internal.tasks.TaskContainerInternal;
import org.gradle.language.base.plugins.LifecycleBasePlugin;
import org.gradle.language.cpp.CppComponent;
import org.gradle.language.cpp.tasks.CppCompile;
import org.gradle.nativeplatform.toolchain.internal.plugins.StandardToolChainsPlugin;
import org.gradle.util.GUtil;

import java.util.Collections;

/**
 * A common base plugin for the C++ executable and library plugins
 *
 * @since 4.1
 */
@Incubating
public class CppBasePlugin implements Plugin<ProjectInternal> {
    /**
     * The name of the implementation configuration.
     */
    public static final String IMPLEMENTATION = "implementation";

    /**
     * The name of the C++ compile classpath configuration.
     */
    public static final String CPP_INCLUDE_PATH = "cppCompileIncludePath";

    /**
     * The name of the native link files configuration.
     */
    public static final String NATIVE_LINK = "nativeLink";

    /**
     * The name of the native runtime files configuration.
     */
    public static final String NATIVE_RUNTIME = "nativeRuntime";

    /**
     * The name of the native test link files configuration.
     *
     * @since 4.2
     */
    public static final String NATIVE_TEST_LINK = "nativeTestLink";

    /**
     * The name of the native test runtime files configuration.
     *
     * @since 4.2
     */
    public static final String NATIVE_TEST_RUNTIME = "nativeTestRuntime";

    @Override
    public void apply(ProjectInternal project) {
        project.getPluginManager().apply(LifecycleBasePlugin.class);
        project.getPluginManager().apply(StandardToolChainsPlugin.class);

        Configuration implementation = project.getConfigurations().create(IMPLEMENTATION);
        implementation.setCanBeConsumed(false);
        implementation.setCanBeResolved(false);

        Configuration includePath = project.getConfigurations().create(CPP_INCLUDE_PATH);
        includePath.extendsFrom(implementation);
        includePath.setCanBeConsumed(false);
        includePath.getAttributes().attribute(Usage.USAGE_ATTRIBUTE, project.getObjects().named(Usage.class, Usage.C_PLUS_PLUS_API));

        Configuration nativeLink = project.getConfigurations().create(NATIVE_LINK);
        nativeLink.extendsFrom(implementation);
        nativeLink.setCanBeConsumed(false);
        nativeLink.getAttributes().attribute(Usage.USAGE_ATTRIBUTE, project.getObjects().named(Usage.class, Usage.NATIVE_LINK));

        Configuration nativeRuntime = project.getConfigurations().create(NATIVE_RUNTIME);
        nativeRuntime.extendsFrom(implementation);
        nativeRuntime.setCanBeConsumed(false);
        nativeRuntime.getAttributes().attribute(Usage.USAGE_ATTRIBUTE, project.getObjects().named(Usage.class, Usage.NATIVE_RUNTIME));

        final TaskContainerInternal tasks = project.getTasks();
        final DirectoryVar buildDirectory = project.getLayout().getBuildDirectory();

        project.getComponents().withType(CppComponent.class, new Action<CppComponent>() {
            @Override
            public void execute(CppComponent component) {
                String compileTaskName = component.getName().equals("main") ? "compileCpp" : "compile" + GUtil.toCamelCase(component.getName()) + "Cpp";
                CppCompile compile = tasks.create(compileTaskName, CppCompile.class);
                compile.includes(component.getCompileIncludePath());
                compile.source(component.getCppSource());

                compile.setCompilerArgs(Collections.<String>emptyList());
                compile.setMacros(Collections.<String, String>emptyMap());
                compile.setObjectFileDir(buildDirectory.dir(component.getName() + "/objs"));
            }
        });
    }
}
