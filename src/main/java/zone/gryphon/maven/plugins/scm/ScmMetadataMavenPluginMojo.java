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

@Mojo(
    name = "metadata",
    defaultPhase = INITIALIZE
)
@Data
@EqualsAndHashCode(callSuper = true)
public class ScmMetadataMavenPluginMojo extends AbstractMojo {

    /**
     * Maven project
     */
    @Parameter(defaultValue = "${project}", required = true, readonly = true)
    protected MavenProject project;

    @Parameter(defaultValue = "${session}", required = true, readonly = true)
    private MavenSession session;

    @Parameter(defaultValue = "false")
    private boolean skip;

    @Parameter(defaultValue = AUTO)
    private String type;

    @Parameter(defaultValue = "${project.basedir}")
    private File directory;

    @Parameter(defaultValue = "scm.metadata")
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

            ScmMetadata metadata = loadMetadata();

            Map<String, String> properties = calculateProperties(metadata);

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

        out.forEach((key, value) -> getLog().debug(String.format("Calculated %s=%s", key, value)));

        return out;
    }

    private String calculatePropertyName(String key) {
        String calculated;

        if (prefix == null || prefix.isEmpty()) {
            calculated = key;
        } else {
            calculated = String.format("%s.%s", prefix, key);
        }

        return calculated;
    }

    private boolean typeMatches(String checkedType) {
        return (type == null && checkedType == null) || (type != null && type.equalsIgnoreCase(checkedType));
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
        out.add(new GitScmMetadataProvider());
        return Collections.unmodifiableList(out);
    }
}
