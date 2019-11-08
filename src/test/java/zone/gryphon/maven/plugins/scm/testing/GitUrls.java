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

package zone.gryphon.maven.plugins.scm.testing;

import lombok.experimental.UtilityClass;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@UtilityClass
@SuppressWarnings("WeakerAccess")
public final class GitUrls {

    private static final int GIT_PORT = 9418;

    private static final int HTTPS_PORT = 443;

    private static final int HTTP_PORT = 80;

    private static final int SSH_PORT = 22;

    private static final List<String> HOSTS = Collections.unmodifiableList(Arrays.asList(
        "gryphon",
        "gryphon.zone",
        "git.gryphon.zone"
    ));

    private static Set<String> newSet() {
        return new HashSet<>();
    }

    private static Collection<String> immutable(Set<String> set) {
        List<String> out = new ArrayList<>(set);
        Collections.sort(out);
        return Collections.unmodifiableList(out);
    }

    private static String concat(String... parts) {
        StringBuilder builder = new StringBuilder();

        for (String part : parts) {
            builder.append(part);
        }

        return builder.toString();
    }

    public static Collection<String> sshProtocolUrls() {
        final Set<String> out = newSet();

        final String sshPrefix = "ssh://";
        final String rsyncPrefix = "rsync://";

        return immutable(out);
    }

    public static Collection<String> gitProtocolUrls() {
        final Set<String> out = newSet();
        final String gitPrefix = "git://";

        for (String host : HOSTS) {


            out.add("ssh://user:pass@" + host + "");
            out.add("ssh://@" + host + "");
            out.add("ssh://%00@" + host + "");
            out.add("ssh://user:pass@" + host + "/");
            out.add("ssh://@" + host + "/");
            out.add("ssh://" + host + "/");
            out.add("ssh://" + host + "?x");
            out.add("ssh://" + host + "#x");
            out.add("ssh://" + host + "/@");
            out.add("ssh://" + host + "?@x");
            out.add("ssh://" + host + "#@x");

            out.add("ssh://q@" + host + ":");
            out.add("ssh://q@" + host + ":456/");
            out.add("ssh://q@" + host + ":0000001?");
            out.add("ssh://q@" + host + ":065535#");
            out.add("ssh://q@" + host + ":65535");

            out.add("ssh://@" + host + ":~/");
            out.add("ssh://@" + host + ":~/foo");
            out.add("ssh://@" + host + ":~user/");
            out.add("ssh://@" + host + ":~user/foo");

            out.add("ssh://@" + host + "/~/");
            out.add("ssh://@" + host + "/~/foo");
            out.add("ssh://@" + host + "/~user/");
            out.add("ssh://@" + host + "/~user/foo");

            out.add("ssh://@" + host + ":22/~/");
            out.add("ssh://@" + host + ":22/~/foo");
            out.add("ssh://@" + host + ":22/~user/");
            out.add("ssh://@" + host + ":22/~user/foo");

            // SCP syntax
            for (String user : new String[]{"", "user@"}) {
                for (String path : new String[]{"", "foo", "foo/bar", "foo/bar.git", "/foo", "/foo/bar", "/foo/bar.git"}) {
                    out.add(user + host + ":" + path);
                }
            }

        }


        out.add("ssh://[::1]");
        out.add("ssh://[::1]/");
        out.add("ssh://[::1]:456/");
        out.add("ssh://[::1]:/");


        return immutable(out);
    }

    public static Collection<String> httpProtocolUrls() {
        final Set<String> out = newSet();
        final String httpPrefix = "http://";
        final String httpsPrefix = "https://";

        for (String host : HOSTS) {
            out.add("http://q@" + host + ":80");
            out.add("https://q@" + host + ":443");
            out.add("http://q@" + host + ":80/");
            out.add("https://q@" + host + ":443?");
        }

        return immutable(out);
    }

    public static Collection<String> fileProtocolUrls() {
        final Set<String> out = newSet();

        out.add("file://user:pass@");
        out.add("file://?");
        out.add("file://#");
        out.add("file:///");
        out.add("file://:");

        out.add("");
        out.add("~");
        out.add("/");

        for (String prefix : new String[]{"", "/", "~/", "file://", "file:///"}) {
            for (String suffix : new String[]{"", ".git", ".git/"}) {

                out.add(prefix + "one" + suffix);
                out.add(prefix + "two/one" + suffix);
                out.add(prefix + "three/two/one" + suffix);
            }
        }

        return immutable(out);
    }

    public static Collection<String> urls() {
        final Set<String> out = newSet();
        out.addAll(sshProtocolUrls());
        out.addAll(gitProtocolUrls());
        out.addAll(httpProtocolUrls());
        out.addAll(fileProtocolUrls());
        return immutable(out);
    }

}
