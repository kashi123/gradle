/*
 * Copyright 2015 the original author or authors.
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
package org.gradle.plugins.buildtypes

import org.gradle.api.Project
import org.gradle.api.Plugin
import org.gradle.api.GradleException

class BuildTypesPlugin implements Plugin<Project> {
    void apply(Project project) {
        project.extensions.add("buildTypes", project.container(BuildType))
        project.buildTypes.all { buildType ->
            register(buildType, project)
        }
    }

    static void register(BuildType buildType, Project project) {
        def name = buildType.name

        project.task(name) {
            group = "Build Type"
            def abbreviation = name[0] + name[1..-1].replaceAll("[a-z]", "")
            def taskNames = project.gradle.startParameter.taskNames
            def usedName = taskNames.find { it in [name, abbreviation] || it.endsWith(":$name") || it.endsWith(":$abbreviation")}
            int index = taskNames.indexOf(usedName)
            if (usedName && !((taskNames[index - 1] == '--task') && (taskNames[index - 2] ==~ /h(e(lp?)?)?/))) {
                buildType.active = true
                def subproject = ''
                if (usedName.contains(':')) {
                    subproject = usedName.substring(0, usedName.lastIndexOf(':') + 1)
                }
                project.afterEvaluate {
                    taskNames.remove(index)
                    if (subproject.empty || project.findProject(subproject)) {
                        buildType.tasks.reverse().each {
                            def path = subproject + it
                            if (project.tasks.findByPath(path)) {
                                taskNames.add(index, path)
                            } else {
                                println "Skipping task '${path}' requested by build type ${name}, as it does not exist."
                            }
                        }
                    } else {
                        println "Skipping execution of build type '${buildType.name}'. Project '$subproject' not found in root project '${project.name}'."
                    }
                    if (taskNames.empty) {
                        taskNames.add('help') //do not trigger the default tasks
                    }
                    project.gradle.startParameter.taskNames = taskNames
                }
                buildType.propertiesAction = { props ->
                    props.each { k, v ->
                        if (!project.hasProperty(k)) {
                            project.ext."$k" = null
                        }
                        project."$k" = v
                    }
                }
            }

            doFirst {
                throw new GradleException("'$name' is a build type and has to be invoked directly, and its name can only be abbreviated to '$abbreviation'.")
            }
        }
    }
}
