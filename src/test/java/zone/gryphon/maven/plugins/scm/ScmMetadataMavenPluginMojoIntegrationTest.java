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

import lombok.extern.slf4j.Slf4j;
import org.apache.maven.project.MavenProject;
import org.junit.Test;

import java.io.File;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;

@SuppressWarnings("RedundantCast")
@Slf4j
public class ScmMetadataMavenPluginMojoIntegrationTest extends AbstractScmMetadataMavenPluginMojoIntegrationTest {

    private static final File TEST_POM_FOLDER = newFile(TARGET_FOLDER, "test-classes", "test-poms", "git");

    private final String[] DEFAULT_GIT_ARRAY_PROPERTIES = concat(DEFAULT_PROPERTIES, new String[]{
        "scm.metadata.remote.path.segment[0]",
        "scm.metadata.remote.path.segment[1]",

        "scm.metadata.remote.path.segment[-1]",
        "scm.metadata.remote.path.segment[-2]"
    });

    private final String[] DEFAULT_GIT_PROPERTY_PROPERTIES = concat(DEFAULT_PROPERTIES, new String[]{
        "scm.metadata.remote.path.segment.0",
        "scm.metadata.remote.path.segment.1",

        "scm.metadata.remote.path.segment.-1",
        "scm.metadata.remote.path.segment.-2"
    });

    @Test
    public void simpleTest() throws Exception {
        copy(new File(TEST_POM_FOLDER, "pom-simple.xml"), pom);

        MavenProject project = readProject();

        ScmMetadataMavenPluginMojo mojo = readScmMetadataMavenPluginMojo(project);

        assertThat(project.getProperties()).isEmpty();

        mojo.execute();

        Properties actual = project.getProperties();

        assertThat(actual).containsOnlyKeys(DEFAULT_GIT_ARRAY_PROPERTIES);

        // make sure the revision starts with the short revision
        assertThat(actual.getProperty("scm.metadata.revision")).startsWith(actual.getProperty("scm.metadata.revision.short"));
    }

    @Test
    public void propertyRemotePathNotationTest() throws Exception {
        copy(new File(TEST_POM_FOLDER, "pom-property-notation.xml"), pom);

        MavenProject project = readProject();

        ScmMetadataMavenPluginMojo mojo = readScmMetadataMavenPluginMojo(project);

        assertThat(project.getProperties()).isEmpty();

        mojo.execute();

        assertThat(project.getProperties()).containsOnlyKeys(DEFAULT_GIT_PROPERTY_PROPERTIES);
    }

    @Test
    public void allRemotePathNotationTest() throws Exception {
        copy(new File(TEST_POM_FOLDER, "pom-all-notation.xml"), pom);

        MavenProject project = readProject();

        ScmMetadataMavenPluginMojo mojo = readScmMetadataMavenPluginMojo(project);

        assertThat(project.getProperties()).isEmpty();

        mojo.execute();

        final String[] expected = new HashSet<>(Arrays.asList(concat(DEFAULT_GIT_ARRAY_PROPERTIES, DEFAULT_GIT_PROPERTY_PROPERTIES))).toArray(new String[0]);

        assertThat(project.getProperties()).containsOnlyKeys(expected);
    }

    @Test
    public void noRemotePathNotationTest() throws Exception {
        copy(new File(TEST_POM_FOLDER, "pom-no-notation.xml"), pom);

        MavenProject project = readProject();

        ScmMetadataMavenPluginMojo mojo = readScmMetadataMavenPluginMojo(project);

        assertThat(project.getProperties()).isEmpty();

        mojo.execute();

        assertThat(project.getProperties()).containsOnlyKeys(DEFAULT_PROPERTIES);
    }

    @Test
    public void renameTest() throws Exception {
        copy(new File(TEST_POM_FOLDER, "pom-with-renaming.xml"), pom);

        MavenProject project = readProject();

        ScmMetadataMavenPluginMojo mojo = readScmMetadataMavenPluginMojo(project);

        assertThat(project.getProperties()).isEmpty();

        mojo.execute();

        assertThat(project.getProperties()).containsOnlyKeys(
            "one",
            "two",
            "three",
            "four",
            "scm.metadata.remote.path.segment[0]",
            "scm.metadata.remote.path.segment[1]",
            "scm.metadata.remote.path.segment[-1]",
            "scm.metadata.remote.path.segment[-2]"
        );
    }

    @Test
    public void customPrefixTest() throws Exception {
        copy(new File(TEST_POM_FOLDER, "pom-with-prefix.xml"), pom);

        MavenProject project = readProject();

        ScmMetadataMavenPluginMojo mojo = readScmMetadataMavenPluginMojo(project);

        assertThat(project.getProperties()).isEmpty();

        mojo.execute();

        assertThat(project.getProperties()).containsOnlyKeys(
            "custom.prefix.revision",
            "custom.prefix.branch",
            "custom.prefix.dirty",
            "custom.prefix.revision.short",

            "custom.prefix.remote.path.segment[0]",
            "custom.prefix.remote.path.segment[1]",
            "custom.prefix.remote.path.segment[-1]",
            "custom.prefix.remote.path.segment[-2]"
        );
    }

    @Test
    public void renameAndCustomPrefixTest() throws Exception {
        copy(new File(TEST_POM_FOLDER, "pom-with-renaming-and-prefix.xml"), pom);

        MavenProject project = readProject();

        ScmMetadataMavenPluginMojo mojo = readScmMetadataMavenPluginMojo(project);

        assertThat(project.getProperties()).isEmpty();

        mojo.execute();

        assertThat(project.getProperties()).containsOnlyKeys(
            "one",
            "two",
            "three",
            "four",
            "custom.prefix.remote.path.segment[0]",
            "custom.prefix.remote.path.segment[1]",
            "custom.prefix.remote.path.segment[-1]",
            "custom.prefix.remote.path.segment[-2]"
        );
    }


    @Test
    public void existingPropertiesTest() throws Exception {
        copy(new File(TEST_POM_FOLDER, "pom-with-existing.xml"), pom);

        final String customProperty = "scm.metadata.foo";

        MavenProject project = readProject();

        ScmMetadataMavenPluginMojo mojo = readScmMetadataMavenPluginMojo(project);

        Properties initial = copyOf(project.getProperties());

        String[] expectedInitial = concat(
            new String[]{customProperty},
            DEFAULT_PROPERTIES
        );

        String[] expected = concat(
            new String[]{customProperty},
            DEFAULT_GIT_ARRAY_PROPERTIES
        );

        assertThat(initial).containsOnlyKeys((Object[]) expectedInitial);

        mojo.execute();

        Properties actual = new Properties();
        actual.putAll(project.getProperties());

        assertThat(actual).containsOnlyKeys((Object[]) expected);

        for (String property : expected) {

            // verify existing properties were overwritten, and that other properties were not
            if (property.equals(customProperty)) {
                assertThat(actual.getProperty(property)).isEqualTo(initial.getProperty(property));
            } else {
                assertThat(actual.getProperty(property)).isNotEqualTo(initial.getProperty(property));
            }
        }
    }


}
