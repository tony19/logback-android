/**
 * Copyright 2019 Anthony Trinh
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ch.qos.logback.core.net.ssl;


/**
 * Various constants used by the SSL implementation.
 *
 * @author Carl Harris
 */
public interface SSL {

  /** Default secure transport protocol */
  String DEFAULT_PROTOCOL = "SSL";

  /** Default key store type */
  String DEFAULT_KEYSTORE_TYPE = "JKS";

  /** Default key store passphrase */
  String DEFAULT_KEYSTORE_PASSWORD = "changeit";

  /** Default secure random generator algorithm */
  String DEFAULT_SECURE_RANDOM_ALGORITHM = "SHA1PRNG";
}
