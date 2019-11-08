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

import lombok.ToString;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import zone.gryphon.maven.plugins.scm.git.GitScmMetadataProvider;
import zone.gryphon.maven.plugins.scm.model.PathPropertiesNotation;
import zone.gryphon.maven.plugins.scm.model.ScmMetadata;
import zone.gryphon.maven.plugins.scm.model.ScmUrl;
import zone.gryphon.maven.plugins.scm.provider.ScmMetadataProvider;
import zone.gryphon.maven.plugins.scm.util.LexicographicMapEntryComparator;
import zone.gryphon.maven.plugins.scm.util.Util;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static org.apache.maven.plugins.annotations.LifecyclePhase.INITIALIZE;
import static zone.gryphon.maven.plugins.scm.util.KnownScms.AUTO;
import static zone.gryphon.maven.plugins.scm.util.KnownScms.NONE;

/**
 * Generates metadata about the project's SCM, and injects it into the build context as
 * <a href="https://maven.apache.org/pom.html#Properties">Maven Properties</a>
 * for re-use by other plugins.
 *
 * <h3>Metadata Calculated:</h3>
 * <ul>
 * <li><code>revision</code> - the current project revision (e.g. git commit SHA)</li>
 * <li><code>revision.short</code> - a potentially truncated version of the <code>revision</code> property</li>
 * <li><code>branch</code> - the current SCM branch (e.g. <code>master</code>)</li>
 * <li><code>dirty</code> - <code>true</code> if there are any uncommitted local changes in files which are not excluded from SCM, <code>false</code> otherwise (equivalent to checking <code>git status --porcelain</code>)</li>
 * </ul>
 *
 * <h3>Remote Path Segment Properties</h3>
 * Additionally, properties prefixed with <code>remote.path.segment</code> will be injected for each path segment in the
 * <code>scm.developerConnection</code> configuration
 * (or <code>scm.connection</code> if <code>scm.developerConnection</code> is not set).
 * Each segment can be accessed by its positive 0 based index in the path, as well as a negative index based on its
 * position relative to the end of the path
 * (i.e. the last path segment can be accessed at index <code>-1</code>, the second to last at <code>-2</code>, etc...).
 * <br><br>
 * For example, the elements of the path <code>/alpha/bravo</code> can be accessed via the following indices:
 * <table>
 * <tr><th>Segment</th><th>Index</th><th>Negative Index</th></tr>
 * <tr><td><code>alpha</code></td><td>0</td><code>-2</code></tr>
 * <tr><td><code>bravo</code></td><td>1</td><td>-1</td></tr>
 * </table>
 * See the <code>remotePathNotation</code> configuration for details about the format of the
 * <code>remote.path.segment</code> properties.
 *
 * <h3>Property Prefix</h3>
 * Note that the name of each property is prefixed with the value of the <code>prefix</code> configuration option,
 * meaning the properties set when using the default configuration are:
 * <ul>
 * <li><code>scm.metadata.revision</code></li>
 * <li><code>scm.metadata.revision.short</code></li>
 * <li><code>scm.metadata.branch</code></li>
 * <li><code>scm.metadata.dirty</code></li>
 * <li><code>scm.metadata.remote.path.segment[*]</code></li>
 * </ul>
 *
 * @since 1.0
 */
@Mojo(
    name = "metadata",
    defaultPhase = INITIALIZE
)
@ToString(callSuper = true)
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

    /**
     * The maximum length of <code>revision.short</code>.
     *
     * @since 1.0
     */
    @Parameter(defaultValue = "8")
    private int shortRevisionLength;

    /**
     * A map of <code>String</code> to <code>String</code> which allows for properties to be renamed before they're set.
     * Each key is the name of a property (after the <code>prefix</code> is applied), and the value is what the property
     * should be renamed to.
     * <br><br>
     * For example, if using the default prefix "<code>scm.metadata.</code>", a map entry
     * "<code>scm.metadata.revision</code>" -> "<code>foo</code>" would result in the plugin setting the property
     * "<code>foo</code>" instead of "<code>scm.metadata.revision</code>" (the value of the property is unaffected).
     * <br><br>
     * Since the rename happens after the prefix is applied, any custom prefix should be included in the map key.
     * For example, if using the custom prefix "<code>bar.</code>",
     * the map entry "<code>bar.revision</code>" -> "<code>foo</code>" would result in the plugin setting the property
     * "<code>foo</code>" instead of "<code>bar.revision</code>" (again, the value of the property is unaffected).
     * <br><br>
     * When configuring the value in the POM, the more compact map syntax should be used:
     * <pre>
     * &lt;configuration&gt;
     *     &lt;rename&gt;
     *         &lt;scm.metadata.revision&gt;foo&lt;/scm.metadata.revision&gt;
     *     &lt;/rename&gt;
     * &lt;/configuration&gt;
     * </pre>
     *
     * @since 1.0
     */
    @Parameter
    private Map<String, String> rename;

    /**
     * A comma separated list of values indicating the format to use for the remote SCM path properties.
     * <br><br>
     * Valid values which can be included in the list:
     * <dl>
     * <dt><code>NONE</code></dt>
     * <dd>
     * Don't inject any properties for the remote SCM path.
     * Note that if there are other values in the list, they will still be injected.
     * </dd>
     * <dt><code>ARRAY</code></dt>
     * <dd>
     * Properties will be injected using the syntax "<code>remote.path.segment[*]</code>".
     * This syntax is easily human readable, but can cause potential issues due to the fact that neither
     * "<code>[</code>" nor "<code>]</code>" can be included in XML tag names, meaning for example that
     * the properties cannot be renamed using the <code>rename</code> configuration option.
     * </dd>
     * <dt><code>PROPERTY</code></dt>
     * <dd>
     * Properties will be injected using the syntax "<code>remote.path.segment.*</code>".
     * This syntax is less readable, however the generated properties are valid XML tag names,
     * meaning they're more compatible with the Maven POM
     * (for example, they can be renamed using the <code>rename</code> configuration option).
     * </dd>
     * </dl>
     * <br>
     * Concrete examples of the properties generated for an SCM with a remote path of <code>/alpha/bravo</code>:
     * <table>
     * <tr><th><code>PROPERTY</code> notation</th><th><code>ARRAY</code> notation</th><th>Property Value</th></tr>
     * <tr><td><code>remote.path.segment.0</code></td><td><code>remote.path.segment[0]</code></td><td><code>alpha</code></td></tr>
     * <tr><td><code>remote.path.segment.1</code></td><td><code>remote.path.segment[1]</code></td><td><code>bravo</code></td></tr>
     * <tr><td><code>remote.path.segment.-1</code></td><td><code>remote.path.segment[-1]</code></td><td><code>bravo</code></td></tr>
     * <tr><td><code>remote.path.segment.-2</code></td><td><code>remote.path.segment[-2]</code></td><td><code>alpha</code></td></tr>
     * </table>
     *
     * @since 1.0
     */
    @Parameter(defaultValue = "ARRAY")
    private String remotePathNotation;

    /**
     * The SCM URL, parsed from {@code project.scm.connection} or {@code project.scm.developerConnection}
     */
    private ScmUrl calculatedScmUrl;

    /**
     * The normalized version of {@link #type}
     */
    private String calculatedScmType;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {

        if (skip) {
            getLog().debug(String.format("skip set to \"%s\", not adding SCM information", skip));
            return;
        }

        if (NONE.equalsIgnoreCase(type)) {
            getLog().debug(String.format("SCM type set to \"%s\", not adding SCM information", type));
            return;
        }

        try {
            executeInternal();
        } catch (MojoFailureException e) {
            throw e;
        } catch (Throwable t) {
            throw new MojoExecutionException("Unexpected failure during plugin execution", t);
        }
    }

    private void executeInternal() throws Exception {
        Objects.requireNonNull(project, "Maven project cannot be null");
        Objects.requireNonNull(session, "Maven session cannot be null");

        if (rename == null) {
            rename = Collections.emptyMap();
        }

        calculatedScmUrl = calculateScmUrl();

        calculatedScmType = AUTO.equalsIgnoreCase(type) ? calculatedScmUrl.getProvider() : type;

        getLog().debug(String.format("Configured SCM: \"%s\", normalized value: \"%s\"", type, calculatedScmType));

        // calculate the metadata itself
        ScmMetadata metadata = loadMetadata();

        // calculate the properties based on the metadata
        Map<String, String> properties = calculateProperties(metadata);

        // log properties for debugging
        debugLogProperties(properties);

        // set the properties
        project.getProperties().putAll(properties);
        session.getUserProperties().putAll(properties);
    }

    private Map<String, String> calculateProperties(ScmMetadata metadata) throws MojoFailureException {
        Map<String, String> out = new HashMap<>();

        String revision = metadata.getRevision();
        String shortRevision = revision.length() <= shortRevisionLength ? revision : revision.substring(0, shortRevisionLength);

        out.put(calculatePropertyName("revision"), revision);
        out.put(calculatePropertyName("revision.short"), shortRevision);
        out.put(calculatePropertyName("branch"), metadata.getBranch());
        out.put(calculatePropertyName("dirty"), Boolean.toString(metadata.getUncommittedChangesPresent()));


        final Collection<PathPropertiesNotation> notation;

        try {
            notation = PathPropertiesNotation.parseCsv(remotePathNotation);
        } catch (IllegalArgumentException e) {
            throw new MojoFailureException(String.format("Value for \"%s\" is invalid: %s", "remotePathNotation", e.getMessage()), e);
        }

        List<String> paths = metadata.getRemotePathSegments();

        for (int index = 0; index < paths.size(); index++) {
            int negativeIndexDisplayed = -1 - index;
            int negativeIndex = negativeIndexDisplayed + paths.size();

            String segment = "remote.path.segment";

            if (notation.contains(PathPropertiesNotation.PROPERTY)) {
                out.put(calculatePropertyName(String.format("%s.%d", segment, index)), paths.get(index));
                out.put(calculatePropertyName(String.format("%s.%d", segment, negativeIndexDisplayed)), paths.get(negativeIndex));
            }

            if (notation.contains(PathPropertiesNotation.ARRAY)) {
                out.put(calculatePropertyName(String.format("%s[%d]", segment, index)), paths.get(index));
                out.put(calculatePropertyName(String.format("%s[%d]", segment, negativeIndexDisplayed)), paths.get(negativeIndex));
            }
        }

        return out;
    }

    private void debugLogProperties(Map<String, String> properties) {
        List<Map.Entry<String, String>> entries = new ArrayList<>(properties.entrySet());
        Collections.sort(entries, LexicographicMapEntryComparator.INSTANCE);

        for (Map.Entry<String, String> entry : entries) {
            getLog().debug(String.format("Calculated %s=%s", entry.getKey(), entry.getValue()));
        }
    }

    private String calculatePropertyName(String givenKey) {
        String key;

        if (Util.isNonBlank(prefix)) {
            key = String.format("%s%s", prefix, givenKey);
        } else {
            key = givenKey;
        }

        return rename.containsKey(key) ? rename.get(key) : key;
    }

    private ScmUrl calculateScmUrl() {
        String connection;

        if (project.getScm() != null) {
            connection = Util.firstNonNull(project.getScm().getDeveloperConnection(), project.getScm().getConnection());
        } else {
            connection = null;
        }

        if (Util.isBlank(connection)) {
            throw new IllegalArgumentException("one of \"scm.developerConnection\" or \"scm.connection\" must be set");
        }

        return ScmUrl.parse(connection);
    }

    private ScmMetadata loadMetadata() throws MojoFailureException {
        boolean foundMatchingProvider = false;

        for (ScmMetadataProvider provider : loadAllProviders()) {

            if (calculatedScmType.equalsIgnoreCase(provider.type())) {
                ScmMetadata output = provider.generate(directory, calculatedScmUrl.getUrl(), getLog());

                if (output != null) {
                    return output;
                }

                foundMatchingProvider = true;
            }

        }

        if (foundMatchingProvider) {
            throw new MojoFailureException(String.format("Project does not appear to use SCM \"%s\"", calculatedScmType));
        }

        throw new MojoFailureException(String.format("Unsupported SCM \"%s\"", calculatedScmType));
    }

    private List<ScmMetadataProvider> loadAllProviders() {
        List<ScmMetadataProvider> out = new ArrayList<>();
        // TODO dynamic SCM metadata provider loader
        out.add(new GitScmMetadataProvider());
        return Collections.unmodifiableList(out);
    }
}
