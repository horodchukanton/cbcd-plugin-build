package com.cloudbees.cd.plugins.build.allure

import io.qameta.allure.gradle.AllureExtension
import io.qameta.allure.gradle.config.SpockConfig
import org.gradle.api.Action
import org.gradle.api.Project

class AllureConfiguration {

    public static void injectAllureConfig(Project project) {

        // Inject allure plugin
        project.plugins.apply('io.qameta.allure')

        // Adding Spock configuration for 'allure' extension
        AllureExtension ext = project.extensions.getByName('allure') as AllureExtension

        ext.setVersion('2.8.1')
        ext.setAspectjweaver(true)
        ext.setClean(true)

        ext.reportDir = project.file('build/reports/allure-report')
        ext.useSpock(new Action<SpockConfig>() {
            @Override
            void execute(SpockConfig spockConfig) {
                spockConfig.setVersion('2.8.1')
            }
        })
    }

}
