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

import lombok.experimental.UtilityClass;

import java.util.Objects;

@UtilityClass
@SuppressWarnings("WeakerAccess")
public final class Util {

    /**
     * Return the first non-null element, or null if all elements are null
     *
     * @param args Elements
     * @param <T>  The type
     * @return The first non-null element, or null if all elements are null
     */
    public static <T> T firstNonNull(T... args) {

        for (T t : Objects.requireNonNull(args)) {
            if (t != null) {
                return t;
            }
        }

        return null;
    }

    public static boolean isNonBlank(String input) {
        return !isBlank(input);
    }

    public static boolean isBlank(String input) {
        return input == null || input.isEmpty();
    }


}
