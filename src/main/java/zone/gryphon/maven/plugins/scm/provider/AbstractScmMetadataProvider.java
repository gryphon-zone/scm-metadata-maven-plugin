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

package zone.gryphon.maven.plugins.scm.provider;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import zone.gryphon.maven.plugins.scm.util.Util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@RequiredArgsConstructor
public abstract class AbstractScmMetadataProvider implements ScmMetadataProvider {

    @NonNull
    private final String type;

    @Override
    public String type() {
        return type;
    }

    protected List<String> chunkPath(String path) {
        if (Util.isBlank(path)) {
            return Collections.emptyList();
        }

        String[] parts = path.split("/");

        if (parts.length == 0) {
            return Collections.emptyList();
        }

        List<String> out = new ArrayList<>(parts.length);

        for (String part : parts) {
            if (Util.isNonBlank(part)) {
                out.add(part);
            }
        }

        return out;
    }
}
