package com.cloudbees.cd.plugins.build.specs


import groovy.transform.CompileStatic
import org.gradle.api.Action
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.DependencySet
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction

@CompileStatic
class InjectDependenciesTask extends DefaultTask {

    // Libraries
    @Input
    String pluginsSpecCoreVersion = '1.9.2'

    @Input
    String spockLibraryVersion = '1.1-groovy-2.4'

    @Input
    String groovyVersion = '2.4.5:indy'

    @TaskAction
    void configureProject() {
        Project project = this.getProject()

        ArrayList<String> dependencies = new ArrayList<>()
        dependencies.add("org.codehaus.groovy:groovy-all:" + groovyVersion)
        dependencies.add("org.spockframework:spock-core:" + spockLibraryVersion)
        dependencies.add("com.electriccloud:ec-specs-plugins-core:" + pluginsSpecCoreVersion)
        dependencies.add("org.slf4j:slf4j-api:1.7.25")

        injectSpecsDependencies(project, dependencies)
    }

    static void injectSpecsDependencies(Project project, ArrayList<String> libs) {
        final Configuration config = project.getConfigurations().getByName("pluginSpecsDeps")
                .setVisible(true)
                .setDescription("Adding plugin specs")

        config.defaultDependencies(new Action<DependencySet>() {
            void execute(DependencySet dependencies) {
                for (String dep : libs) {
                    Map dependency = ["implementation": dep]
                    dependencies.add(project.getDependencies().project(dependency))
                }
            }
        })
    }
}
