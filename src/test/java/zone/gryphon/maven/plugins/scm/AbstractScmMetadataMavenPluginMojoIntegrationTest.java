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
import zone.gryphon.maven.plugins.scm.testing.TestNameLogger;

import java.io.File;
import java.io.IOException;
import java.util.Properties;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
@SuppressWarnings({"WeakerAccess"})
public abstract class AbstractScmMetadataMavenPluginMojoIntegrationTest {

    protected static final File TARGET_FOLDER = new File("target").getAbsoluteFile();

    protected static final Random random = new Random();

    @Rule
    public final MojoRule rule = new MojoRule();

    @Rule
    public final TestNameLogger testNameLogger = new TestNameLogger();

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
