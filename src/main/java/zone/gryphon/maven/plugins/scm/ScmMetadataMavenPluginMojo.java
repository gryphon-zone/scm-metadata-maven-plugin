/*
 * Copyright 2019-2019 Gryphon Zone
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package zone.gryphon.maven.plugins.scm;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import zone.gryphon.maven.plugins.scm.git.GitScmMetadataProvider;
import zone.gryphon.maven.plugins.scm.model.ScmMetadata;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.apache.maven.plugins.annotations.LifecyclePhase.INITIALIZE;
import static zone.gryphon.maven.plugins.scm.KnownScms.AUTO;
import static zone.gryphon.maven.plugins.scm.KnownScms.NONE;

/**
 * Generates metadata about the project's SCM, and injects it into the build context as
 * <a href="https://maven.apache.org/pom.html#Properties">Maven Properties</a>
 * for re-use by other plugins.
 * <br>
 * The metadata calculated is:
 * <ul>
 * <li><code>revision</code> - the current project revision (e.g. git commit SHA)</li>
 * <li><code>branch</code> - the current SCM branch (e.g. <code>master</code>)</li>
 * <li><code>dirty</code> - <code>true</code> if there are any uncommitted local changes in files which are not excluded from SCM, <code>false</code> otherwise (equivalent to checking <code>git status --porcelain</code>)</li>
 * </ul>
 * <br>
 * Note that the name of each property is prefixed with the value of the <code>prefix</code> configuration option,
 * meaning the properties set when using the default configuration are:
 * <ul>
 * <li><code>scm.metadata.revision</code></li>
 * <li><code>scm.metadata.branch</code></li>
 * <li><code>scm.metadata.dirty</code></li>
 * </ul>
 *
 * @since 1.0
 */
@Mojo(
    name = "metadata",
    defaultPhase = INITIALIZE
)
@Data
@EqualsAndHashCode(callSuper = true)
public class ScmMetadataMavenPluginMojo extends AbstractMojo {

    /**
     * Maven project
     *
     * @since 1.0
     */
    @Parameter(defaultValue = "${project}", required = true, readonly = true)
    protected MavenProject project;

    /**
     * Maven session
     *
     * @since 1.0
     */
    @Parameter(defaultValue = "${session}", required = true, readonly = true)
    private MavenSession session;

    /**
     * If true, plugin execution will be skipped
     *
     * @since 1.0
     */
    @Parameter(defaultValue = "false")
    private boolean skip;

    /**
     * SCM implementation to use when calculating metadata. Valid options:
     * <ul>
     * <li><code>none</code> - don't inject SCM metadata (equivalent to setting <code>skip</code> to true</li>
     * <li><code>auto</code> - attempt to automatically determine the SCM implementation based on the <a href="https://maven.apache.org/pom.html#SCM"><code>scm.connection</code></a> value set in the POM</li>
     * <li><code>git</code> - use <code>git</code> to look up SCM information</li>
     * </ul>
     *
     * @since 1.0
     */
    @Parameter(defaultValue = AUTO)
    private String type;

    /**
     * Directory to start search for SCM configuration in.
     * Parent directories will be recursively checked until the SCM configuration is discovered, or the root folder is reached.
     *
     * @since 1.0
     */
    @Parameter(defaultValue = "${project.basedir}")
    private File directory;

    /**
     * Prefix to apply to all property names.
     *
     * @since 1.0
     */
    @Parameter(defaultValue = "scm.metadata.")
    private String prefix;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {

        if (skip) {
            getLog().debug(String.format("skip set to \"%s\", not adding SCM information", skip));
            return;
        }

        if (typeMatches(NONE)) {
            getLog().debug(String.format("type set to \"%s\", not adding SCM information", type));
            return;
        }

        try {

            // calculate the metadata itself
            ScmMetadata metadata = loadMetadata();

            // calculate the values of the properties.
            // handles any renaming
            Map<String, String> properties = calculateProperties(metadata);

            // set the properties
            project.getProperties().putAll(properties);
            session.getUserProperties().putAll(properties);

        } catch (MojoFailureException e) {
            throw e;
        } catch (Throwable t) {
            throw new MojoExecutionException("Unexpected failure during plugin execution", t);
        }
    }

    private Map<String, String> calculateProperties(ScmMetadata metadata) {
        Map<String, String> out = new HashMap<>();

        out.put(calculatePropertyName("revision"), metadata.getRevision());
        out.put(calculatePropertyName("branch"), metadata.getBranch());
        out.put(calculatePropertyName("dirty"), Boolean.toString(metadata.getUncommittedChangesPresent()));

        for (Map.Entry<String, String> entry : out.entrySet()) {
            getLog().debug(String.format("Calculated %s=%s", entry.getKey(), entry.getValue()));
        }

        return out;
    }

    private String calculatePropertyName(String key) {
        String calculated;

        if (prefix == null || prefix.isEmpty()) {
            calculated = key;
        } else {
            calculated = String.format("%s%s", prefix, key);
        }

        return calculated;
    }

    /**
     * Returns true if the given SCM implementation matches the configured {@link #getType()}.
     *
     * @param scmTypeToCheck The SCM type to check
     * @return True if the given SCM type matches the configured SCM type
     */
    private boolean typeMatches(String scmTypeToCheck) {
        return (type == null && scmTypeToCheck == null) || (type != null && type.equalsIgnoreCase(scmTypeToCheck));
    }

    private ScmMetadata loadMetadata() throws MojoFailureException {

        boolean isAuto = typeMatches(AUTO);
        boolean foundMatchingProvider = false;

        for (ScmMetadataProvider provider : loadAllProviders()) {
            boolean providerMatches = typeMatches(provider.type());

            if (isAuto || providerMatches) {
                ScmMetadata maybeOutput = provider.generate(directory, getLog());

                if (maybeOutput != null) {
                    return maybeOutput;
                }
            }

            foundMatchingProvider |= providerMatches;
        }

        if (isAuto) {
            throw new MojoFailureException("Unable to automatically determine SCM in use");
        }

        if (foundMatchingProvider) {
            throw new MojoFailureException(String.format("Project does not appear to use SCM \"%s\"", type));
        }

        throw new MojoFailureException(String.format("Unsupported SCM \"%s\"", type));
    }

    private List<ScmMetadataProvider> loadAllProviders() {
        List<ScmMetadataProvider> out = new ArrayList<>();
        // TODO dynamic SCM metadata provider loader
        out.add(new GitScmMetadataProvider());
        return Collections.unmodifiableList(out);
    }
}
