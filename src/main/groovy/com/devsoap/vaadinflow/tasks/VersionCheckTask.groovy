/*
 * Copyright 2018-2020 Devsoap Inc.
 *
 * Licensed under the Creative Commons Attribution-NoDerivatives 4.0
 * International Public License (the "License"); you may not use this file
 * except in compliance with the License.
 *
 * You may obtain a copy of the License at
 *
 *      https://creativecommons.org/licenses/by-nd/4.0/
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.devsoap.vaadinflow.tasks

import com.devsoap.vaadinflow.VaadinFlowPlugin
import com.devsoap.vaadinflow.util.Versions
import groovy.transform.Memoized
import groovy.transform.PackageScope
import groovy.util.logging.Log
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.util.VersionNumber

import java.util.concurrent.TimeUnit
import java.util.regex.Matcher

/**
 * Checks the plugin version for a new version
 *
 *  @author John Ahlroos
 *  @since 1.0
 */
@Log('LOGGER')
class VersionCheckTask extends DefaultTask {

    static final String NAME = 'vaadinPluginVersionCheck'

    static final String VERSION_CACHE_FILE = '.vaadin-gradle-flow-version.check'

    private static final String URL = "https://plugins.gradle.org/plugin/$VaadinFlowPlugin.PLUGIN_ID"

    private final File versionCacheFile = new File(project.buildDir, VERSION_CACHE_FILE)

    VersionCheckTask() {
        description = 'Checks if there is a newer version of the plugin available'
        group = 'Vaadin'
        project.afterEvaluate {
            boolean firstRun = false
            if (!getVersionCacheFile()) {
                new File(project.buildDir, VERSION_CACHE_FILE).with {
                    it.parentFile.mkdirs()
                    it.createNewFile()
                }
                firstRun = true
            }

            long cacheAge = System.currentTimeMillis() - getVersionCacheFile().lastModified()
            long cacheTime = TimeUnit.DAYS.toMillis(1)
            outputs.upToDateWhen { !firstRun && cacheAge < cacheTime }
            onlyIf { firstRun || cacheAge > cacheTime }
        }
    }

    /**
     * Checks for a new version
     */
    @TaskAction
    void run() {
        VersionNumber pluginVersion =  Versions.version('vaadin.plugin.version')
        VersionNumber latestVersion = getLatestReleaseVersion(project)
        if (latestVersion > pluginVersion) {
            LOGGER.warning "A newer version of the Gradle Vaadin Flow plugin is available ($latestVersion)"
        } else {
            LOGGER.info('You are using the latest plugin. Excellent!')
        }
        getVersionCacheFile().text = latestVersion.toString()
    }

    /**
     * Gets the latest released Gradle plugin version
     *
     * @return
     *      the latest released version number
     */
    static VersionNumber getLatestReleaseVersion(Project project) {
        VersionNumber version = VersionNumber.UNKNOWN

        if (project.gradle.startParameter.offline) {
            return version
        }

        try {
            String html = URL.toURL().text
            Matcher matcher = html =~ /Version (\S+)/
            if (matcher.find()) {
                version = VersionNumber.parse(matcher.group(1))
            }
        } catch (IOException | URISyntaxException e) {
            version = VersionNumber.UNKNOWN
        }
        version
    }

    /**
     * Get version cache file
     */
    @OutputFile
    protected File getVersionCacheFile() {
        versionCacheFile
    }
}
