/*
 * Copyright © 2015 The Gravitee team (http://gravitee.io)
 *
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
import static org.junit.Assert.assertEquals;

import io.gravitee.kubernetes.mapper.ObjectMeta;
import org.junit.Test;

/**
 * @author Antoine CORDIER (antoine.cordier at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ObjectMetaTest {

    @Test
    public void shouldSanitizeName() {
        var meta = new ObjectMeta("Some_invalid @RFC-1123 name!");
        assertEquals("some-invalid-rfc-1123-name", meta.getName());
    }

    @Test
    public void shouldNotChangeName() {
        var meta = new ObjectMeta("some-valid.rfc.1123.name");
        assertEquals("some-valid.rfc.1123.name", meta.getName());
    }
}
