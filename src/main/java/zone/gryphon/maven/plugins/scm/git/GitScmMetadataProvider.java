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

package zone.gryphon.maven.plugins.scm.git;

import lombok.NonNull;
import org.apache.maven.plugin.logging.Log;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.Status;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import zone.gryphon.maven.plugins.scm.model.ScmMetadata;
import zone.gryphon.maven.plugins.scm.provider.AbstractScmMetadataProvider;
import zone.gryphon.maven.plugins.scm.util.Util;

import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import static java.util.regex.Pattern.CASE_INSENSITIVE;
import static zone.gryphon.maven.plugins.scm.util.KnownScms.GIT;

public class GitScmMetadataProvider extends AbstractScmMetadataProvider {

    /**
     * Pattern to match URLs with a valid {@code scheme} which is supported by {@code git}, as documented in
     * <a href="https://www.ietf.org/rfc/rfc2396.txt">section 3.1 of RFC 2396</a>
     * and <a href="https://git-scm.com/docs/git-clone#_git_urls_a_id_urls_a">GIT URLS</a>
     */
    private static final Pattern SCHEME_PATTERN = Pattern.compile("^(?:(?:ssh)|(?:git)|(?:https?)|(?:ftps?)|(?:file)):.+", CASE_INSENSITIVE);

    public GitScmMetadataProvider() {
        super(GIT);
    }

    @Override
    public ScmMetadata generate(File directory, String url, Log log) {
        try {
            return generateInternal(directory, url, log);
        } catch (Exception e) {
            throw e instanceof RuntimeException ? (RuntimeException) e : new RuntimeException(e);
        }
    }

    private String stripGitSuffix(String input) {
        for (String suffix : new String[]{".git", ".git/"}) {
            if (input.endsWith(suffix)) {
                return input.substring(0, input.length() - suffix.length());
            }
        }

        return input;
    }

    /**
     * Parses the path component from a git remote URL
     */
    String parsePath(String input) {

        if (Util.isBlank(input)) {
            return "";
        }

        final String url = stripGitSuffix(input);

        if (SCHEME_PATTERN.matcher(url).find()) {
            // if the URL has a scheme, then we can simply use a URI to parse it as-is (hopefully...)
            return URI.create(url).getPath();
        }

        int colonIndex = url.indexOf(':');

        // Only two protocols are supported without a scheme, file and ssh.
        //
        // scheme-less SSH URLs require a colon, meaning if there isn't one this is a local file path,
        // and thus the entire URL is the path.
        if (colonIndex < 0) {
            return url;
        }

        int slashIndex = url.indexOf('/');

        // Per the git documentation at https://git-scm.com/docs/git-clone#_git_urls_a_id_urls_a:
        //
        //    [the scheme-less ssh] syntax is only recognized if there are no slashes before the first colon.
        //    This helps differentiate a local path that contains a colon.
        //    For example the local path foo:bar could be specified as an absolute path
        //    or ./foo:bar to avoid being misinterpreted as an ssh url.
        //
        // meaning if a slash is present, and it's before the colon, it's a file path
        if (slashIndex >= 0 && slashIndex < colonIndex) {
            return url;
        }

        return url.substring(colonIndex + 1);
    }

    private ScmMetadata generateInternal(File directory, String url, Log log) throws Exception {
        File gitDir = findGitDir(directory);

        // neither directory nor any of its parents are a git repo
        if (gitDir == null) {
            log.debug(String.format("not a git repository (or any of the parent directories): %s", directory.getPath()));
            return null;
        }

        try (
            Repository repo = new FileRepositoryBuilder()
                .setGitDir(gitDir)
                .readEnvironment() // scan environment GIT_* variables
                .build();

            Git git = new Git(repo);
        ) {

            ObjectId head = repo.resolve("HEAD");
            Status status = git.status().call();

            Set<String> uncommitted = status.getUncommittedChanges();
            Set<String> untracked = status.getUntracked();
            boolean clean = uncommitted.isEmpty() && untracked.isEmpty();

            logFiles(uncommitted, "uncommitted", log);
            logFiles(untracked, "untracked", log);

            return ScmMetadata.builder()
                .branch(repo.getBranch())
                .revision(head.getName())
                .uncommittedChangesPresent(!clean)
                .remotePathSegments(chunkPath(parsePath(url)))
                .build();
        }
    }

    private void logFiles(Collection<String> files, String name, Log log) {

        if (!log.isDebugEnabled()) {
            return;
        }

        if (files.isEmpty()) {
            log.debug(String.format("No %s files", name));
        } else {
            List<String> sorted = new ArrayList<>(files);
            Collections.sort(sorted);

            log.debug(String.format("%d %s file%s", files.size(), name, files.size() == 1 ? "" : "s"));
            for (int i = 0; i < sorted.size(); i++) {
                log.debug(String.format("  %d) %s", i, sorted.get(i)));
            }
        }
    }

    private File findGitDir(@NonNull File provided) {
        File file = provided.getAbsoluteFile();

        while (file.getParent() != null) {
            File git = new File(file, ".git");

            if (git.exists() && git.isDirectory()) {
                return git;
            }

            file = file.getParentFile();
        }

        return null;
    }

}
