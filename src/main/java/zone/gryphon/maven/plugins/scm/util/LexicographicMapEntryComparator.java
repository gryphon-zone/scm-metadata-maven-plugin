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

package zone.gryphon.maven.plugins.scm.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.Comparator;
import java.util.Map;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class LexicographicMapEntryComparator implements Comparator<Map.Entry<String, String>> {

    public static final Comparator<Map.Entry<String, String>> INSTANCE = new LexicographicMapEntryComparator();


    @Override
    public int compare(Map.Entry<String, String> o1, Map.Entry<String, String> o2) {

        if (o1 == null || o2 == null) {
            return compareNullObjects(o1, o2);
        }

        String key1 = o1.getKey();
        String key2 = o2.getKey();

        if (key1 == null || key2 == null) {
            return compareNullObjects(key1, key2);
        }


        return String.CASE_INSENSITIVE_ORDER.compare(key1, key2);
    }

    private int compareNullObjects(Object one, Object two) {

        if (one != null && two != null) {
            throw new IllegalArgumentException("at least one argument must be null");
        }

        if (one == null) {

            if (two == null) {
                return 0;
            }

            return -1;
        }

        return 1;
    }

}
