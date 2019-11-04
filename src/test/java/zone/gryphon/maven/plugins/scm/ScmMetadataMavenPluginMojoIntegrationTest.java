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
import org.apache.maven.plugin.testing.MojoRule;
import org.apache.maven.project.MavenProject;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.io.File;
import java.util.Properties;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
public class ScmMetadataMavenPluginMojoIntegrationTest {

    private final Random random = new Random();

    private final String className = getClass().getSimpleName();

    private final File target = new File("target").getAbsoluteFile();

    @Rule
    public final MojoRule rule = new MojoRule();

    private File tempFolder;

    @Before
    public void setup() {
        tempFolder = newFile(target, "testing", className, hex());
        assertThat(tempFolder.mkdirs() || tempFolder.exists()).isTrue();
    }

    @After
    public void cleanup() {
        assertThat(tempFolder.delete() || !tempFolder.exists()).isTrue();
    }

    @Test
    public void simpleGitTest() throws Exception {
        File pom = new File("target/test-classes/project-to-test/pom.xml");

        assertThat(pom).isFile();

        MavenProject project = rule.readMavenProject(pom.getParentFile());

        ScmMetadataMavenPluginMojo mojo = (ScmMetadataMavenPluginMojo) rule.lookupConfiguredMojo(project, "metadata");

        assertThat(project.getProperties()).containsOnlyKeys(
            "scm.metadata.branch",
            "foo"
        );

        String originalScmPropertyValue = project.getProperties().getProperty("scm.metadata.branch");
        String originalOtherPropertyValue = project.getProperties().getProperty("foo");

        mojo.execute();

        Properties actual = project.getProperties();

        assertThat(actual).containsOnlyKeys(
            "scm.metadata.branch",
            "scm.metadata.dirty",
            "scm.metadata.revision",
            "scm.metadata.revision.short",
            "foo"
        );

        // verify we didn't modify the other property, but did modify the value of the SCM property
        assertThat(actual).containsEntry("foo", originalOtherPropertyValue);
        assertThat(actual).doesNotContainEntry("scm.metadata.branch", originalScmPropertyValue);

        // make sure the revision starts with the short revision
        assertThat(actual.getProperty("scm.metadata.revision")).startsWith(actual.getProperty("scm.metadata.revision.short"));
    }

    private File newFile(final File parent, final String... parts) {
        File out = parent;

        for (String path : parts) {
            out = new File(out, path);
        }

        return out;
    }

    private String hex() {
        return Integer.toHexString(random.nextInt());
    }


}
