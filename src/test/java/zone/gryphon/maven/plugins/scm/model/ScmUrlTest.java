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

package zone.gryphon.maven.plugins.scm.model;

import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
@RunWith(Parameterized.class)
public class ScmUrlTest {

    private static ScmUrl gitScmUrl(String url) {
        return ScmUrl.builder()
            .provider("git")
            .delimiter("#")
            .url(url)
            .build();
    }


    @Parameterized.Parameters
    public static Collection<ScmUrl> data() {
        Set<ScmUrl> out = new HashSet<>();

        // git URLs
        for (String suffix : new String[]{"", ".git", ".git/"}) {
            out.add(gitScmUrl("git://git.gryphon.zone/path/to/repository" + suffix));
            out.add(gitScmUrl("http://git.gryphon.zone/path/to/repository" + suffix));
            out.add(gitScmUrl("https://git.gryphon.zone/path/to/repository" + suffix));
            out.add(gitScmUrl("ssh://git.gryphon.zone/path/to/repository" + suffix));
            out.add(gitScmUrl("ssh://git@git.gryphon.zone/path/to/repository" + suffix));
            out.add(gitScmUrl("git://git.gryphon.zone:9418/path/to/repository" + suffix));
            out.add(gitScmUrl("http://git.gryphon.zone:80/path/to/repository" + suffix));
            out.add(gitScmUrl("https://git.gryphon.zone:443/path/to/repository" + suffix));
            out.add(gitScmUrl("ssh://git.gryphon.zone:22/path/to/repository" + suffix));
            out.add(gitScmUrl("ssh://git@git.gryphon.zone:22/path/to/repository" + suffix));

            out.add(gitScmUrl("file://localhost/path/to/repository" + suffix));

            out.add(gitScmUrl("git.gryphon.zone/path/to/repository" + suffix));
            out.add(gitScmUrl("git.gryphon.zone:22/path/to/repository" + suffix));

            out.add(gitScmUrl("git@git.gryphon.zone/path/to/repository" + suffix));
            out.add(gitScmUrl("git@git.gryphon.zone:22/path/to/repository" + suffix));
        }

        return out;
    }

    @Parameterized.Parameter
    public ScmUrl datum;


    @Test
    public void testWithPipeDelimiter() {
        test("|");
    }

    @Test
    public void testWithColonDelimiter() {
        test(":");
    }

    private void test(String delimiter) {
        ScmUrl expected = datum.toBuilder().delimiter(delimiter).build();
        String url = String.format("scm:%s%s%s", expected.getProvider(), expected.getDelimiter(), expected.getUrl());
        ScmUrl actual = ScmUrl.parse(url);

        assertThat(actual).isEqualTo(expected);
        assertThat(actual.getDelimiter()).isEqualTo(delimiter);
    }


}
