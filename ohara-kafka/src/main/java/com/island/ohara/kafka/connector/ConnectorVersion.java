/*
 * Copyright 2019 is-land
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

package com.island.ohara.kafka.connector;

import com.island.ohara.common.annotations.Optional;
import com.island.ohara.common.util.CommonUtils;
import com.island.ohara.common.util.VersionUtils;
import java.util.Objects;

/**
 * used to display the version information of connector. this is not a required field in custom
 * connector. However, you won't like the default since everything is unknown.
 */
public final class ConnectorVersion {
  /** this default value is equal to ohara's version */
  public static final ConnectorVersion DEFAULT =
      ConnectorVersion.builder()
          .version(VersionUtils.VERSION)
          .revision(VersionUtils.REVISION)
          .author(VersionUtils.USER)
          .build();

  public static Builder builder() {
    return new Builder();
  }

  private final String version;
  private final String revision;
  private final String author;

  private ConnectorVersion(String version, String revision, String author) {
    this.version = CommonUtils.requireNonEmpty(version);
    this.revision = CommonUtils.requireNonEmpty(revision);
    this.author = CommonUtils.requireNonEmpty(author);
  }

  public String version() {
    return version;
  }

  public String revision() {
    return revision;
  }

  public String author() {
    return author;
  }

  @Override
  public int hashCode() {
    return Objects.hash(version, revision, author);
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == this) return true;
    if (obj instanceof ConnectorVersion) {
      ConnectorVersion another = (ConnectorVersion) obj;
      return version().equals(another.version())
          && revision().equals(another.revision())
          && author().equals(another.author());
    }
    return false;
  }

  @Override
  public String toString() {
    return "version:" + version + "revision:" + revision + "author:" + author;
  }

  public static class Builder implements com.island.ohara.common.pattern.Builder<ConnectorVersion> {
    private String version = "unknown";
    private String revision = "unknown";
    private String author = "unknown";

    private Builder() {}

    @Optional("default is unknown")
    public Builder version(String version) {
      this.version = CommonUtils.requireNonEmpty(version);
      return this;
    }

    @Optional("default is unknown")
    public Builder revision(String revision) {
      this.revision = CommonUtils.requireNonEmpty(revision);
      return this;
    }

    @Optional("default is unknown")
    public Builder author(String author) {
      this.author = CommonUtils.requireNonEmpty(author);
      return this;
    }

    @Override
    public ConnectorVersion build() {
      return new ConnectorVersion(version, revision, author);
    }
  }
}
