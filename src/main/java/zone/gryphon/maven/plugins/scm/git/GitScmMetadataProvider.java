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
import zone.gryphon.maven.plugins.scm.AbstractScmMetadataProvider;
import zone.gryphon.maven.plugins.scm.model.RemoteMetadata;
import zone.gryphon.maven.plugins.scm.model.ScmMetadata;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static zone.gryphon.maven.plugins.scm.KnownScms.GIT;

public class GitScmMetadataProvider extends AbstractScmMetadataProvider {

    public GitScmMetadataProvider() {
        super(GIT);
    }

    @Override
    public ScmMetadata generate(File directory, Log log) {
        try {
            return generateInternal(directory, log);
        } catch (Exception e) {
            throw e instanceof RuntimeException ? (RuntimeException) e : new RuntimeException(e);
        }
    }

    private ScmMetadata generateInternal(File directory, Log log) throws Exception {
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
                .remotes(Collections.<RemoteMetadata>emptySet())
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
