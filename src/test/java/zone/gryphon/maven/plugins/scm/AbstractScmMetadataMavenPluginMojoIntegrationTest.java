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
import org.apache.commons.io.FileUtils;
import org.apache.maven.plugin.testing.MojoRule;
import org.apache.maven.project.MavenProject;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.Properties;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
@SuppressWarnings("WeakerAccess")
public abstract class AbstractScmMetadataMavenPluginMojoIntegrationTest {

    protected static final File TARGET_FOLDER = new File("target").getAbsoluteFile();

    private static final File TEST_POM_FOLDER = newFile(TARGET_FOLDER, "test-classes", "test-poms", "generic");

    protected static final Random random = new Random();

    @Rule
    public final MojoRule rule = new MojoRule();

    /**
     * <h3>implementation note:</h3>
     * not marked {@code static} so that separate test runs cannot interfere with each other
     * if one of them mutates the array
     */
    protected final String[] DEFAULT_PROPERTIES = new String[]{
        "scm.metadata.branch",
        "scm.metadata.dirty",
        "scm.metadata.revision",
        "scm.metadata.revision.short"
    };

    protected final String className = getClass().getSimpleName();

    /**
     * Transient temporary folder into which test files can be written
     */
    protected File folder;

    /**
     * POM file in {@link #folder}
     */
    protected File pom;

    @Before
    public void setup() {
        folder = newFile(TARGET_FOLDER, "testing", className, hex());
        assertThat(folder.mkdirs() || folder.exists()).isTrue();
        pom = new File(folder, "pom.xml");
    }

    @After
    public void cleanup() {
        FileUtils.deleteQuietly(folder);
    }

    @Test
    public void simpleTest() throws Exception {
        copy(new File(TEST_POM_FOLDER, "pom-simple.xml"), pom);

        MavenProject project = readProject();

        ScmMetadataMavenPluginMojo mojo = readScmMetadataMavenPluginMojo(project);

        assertThat(project.getProperties()).isEmpty();

        mojo.execute();

        Properties actual = project.getProperties();

        assertThat(actual).containsOnlyKeys(DEFAULT_PROPERTIES);

        // make sure the revision starts with the short revision
        assertThat(actual.getProperty("scm.metadata.revision")).startsWith(actual.getProperty("scm.metadata.revision.short"));
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
            "four"
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
            "custom.prefix.revision.short"
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
            "four"
        );
    }


    @Test
    public void existingPropertiesTest() throws Exception {
        copy(new File(TEST_POM_FOLDER, "pom-with-existing.xml"), pom);

        final String customProperty = "scm.metadata.foo";

        MavenProject project = readProject();

        ScmMetadataMavenPluginMojo mojo = readScmMetadataMavenPluginMojo(project);

        Properties initial = copyOf(project.getProperties());

        String[] expected = concat(
            new String[]{customProperty},
            DEFAULT_PROPERTIES
        );

        assertThat(initial).containsOnlyKeys(expected);

        mojo.execute();

        Properties actual = new Properties();
        actual.putAll(project.getProperties());

        assertThat(actual).containsOnlyKeys(expected);

        for (String property : expected) {

            // verify existing properties were overwritten, and that other properties were not
            if (property.equals(customProperty)) {
                assertThat(actual.getProperty(property)).isEqualTo(initial.getProperty(property));
            } else {
                assertThat(actual.getProperty(property)).isNotEqualTo(initial.getProperty(property));
            }
        }
    }

    protected String[] concat(String[] one, String[] two) {
        String[] out = new String[one.length + two.length];

        System.arraycopy(one, 0, out, 0, one.length);
        System.arraycopy(two, 0, out, one.length, two.length);

        return out;
    }

    protected Properties copyOf(Properties input) {
        Properties out = new Properties();
        out.putAll(input);
        return out;
    }


    protected static File newFile(final File parent, final String... parts) {
        File out = parent;

        for (String path : parts) {
            out = new File(out, path);
        }

        return out;
    }

    protected MavenProject readProject() {
        try {
            return rule.readMavenProject(pom.getParentFile());
        } catch (Exception e) {
            throw new RuntimeException("Failed to read Maven POM from \"" + pom.getAbsolutePath() + "\"", e);
        }
    }

    protected ScmMetadataMavenPluginMojo readScmMetadataMavenPluginMojo(MavenProject project) {
        try {
            return (ScmMetadataMavenPluginMojo) rule.lookupConfiguredMojo(project, "metadata");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    protected String hex() {
        return Integer.toHexString(random.nextInt());
    }

    protected void copy(File source, File dest) {
        assertThat(dest).doesNotExist();
        assertThat(source).isFile();

        try {
            FileUtils.copyFile(source, dest);
        } catch (IOException e) {
            throw new RuntimeException(String.format("Failed to copy file from %s to %s", source.getAbsolutePath(), dest.getAbsolutePath()), e);
        }

        assertThat(dest).isFile();
    }


}
