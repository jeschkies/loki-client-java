/*
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
package io.github.jeschkies.loki.client;

import java.time.Instant;

public final class Time {
    private Time() {
    throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
  }

  public static Long now() {
    // This precision is fine for us.
    var now = Instant.now();
    return nanosFromInstant(now);
  }

  public static Long nanosFromInstant(Instant i) {
    return i.getEpochSecond() * 1000000000L + i.getNano(); // as nanoseconds
  }
}
