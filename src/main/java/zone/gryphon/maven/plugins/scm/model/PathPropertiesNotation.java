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

import zone.gryphon.maven.plugins.scm.util.Util;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public enum PathPropertiesNotation {

    NONE,
    ARRAY,
    PROPERTY;

    private static final Map<String, PathPropertiesNotation> names;

    static {
        Map<String, PathPropertiesNotation> local = new HashMap<>();

        for (PathPropertiesNotation value : PathPropertiesNotation.values()) {
            local.put(value.name(), value);
        }

        names = Collections.unmodifiableMap(local);
    }

    public static Collection<PathPropertiesNotation> parseCsv(String input) {

        if (Util.isBlank(input)) {
            return Collections.emptyList();
        }

        final Set<PathPropertiesNotation> out = new HashSet<>();

        final String[] parts = input.split(",");

        for (String part : parts) {
            PathPropertiesNotation value = names.get(part.trim().toUpperCase());

            if (value == null) {
                throw new IllegalArgumentException(String.format("Illegal value \"%s\" in input string \"%s\". Legal values: %s", part, input, names.keySet()));
            }

            out.add(value);
        }

        return Collections.unmodifiableSet(out);
    }

}
