/*
 * Copyright 2020 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.connectclub.jvbuster.utils.gcloud;

import com.google.api.pathtemplate.PathTemplate;
import com.google.api.resourcenames.ResourceName;
import com.google.api.resourcenames.ResourceNameFactory;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;

import java.util.Map;
import java.util.Objects;

public final class ProjectZoneDiskTypeName implements ResourceName {
  private final String diskType;
  private final String project;
  private final String zone;
  private static final PathTemplate PATH_TEMPLATE =
      PathTemplate.createWithoutUrlEncoding("{project}/zones/{zone}/diskTypes/{diskType}");

  public static final String SERVICE_ADDRESS =
      "https://www.googleapis.com/compute/v1/projects/";

  private volatile Map<String, String> fieldValuesMap;

  public static Builder newBuilder() {
    return new Builder();
  }

  public Builder toBuilder() {
    return new Builder(this);
  }

  private ProjectZoneDiskTypeName(Builder builder) {
    diskType = Preconditions.checkNotNull(builder.getDiskType());
    project = Preconditions.checkNotNull(builder.getProject());
    zone = Preconditions.checkNotNull(builder.getZone());
  }

  public static ProjectZoneDiskTypeName of(String diskType, String project, String zone) {
    return newBuilder().setDiskType(diskType).setProject(project).setZone(zone).build();
  }

  public static String format(String diskType, String project, String zone) {
    return of(diskType, project, zone).toString();
  }

  public String getDiskType() {
    return diskType;
  }

  public String getProject() {
    return project;
  }

  public String getZone() {
    return zone;
  }

  @Override
  public Map<String, String> getFieldValuesMap() {
    if (fieldValuesMap == null) {
      synchronized (this) {
        if (fieldValuesMap == null) {
          ImmutableMap.Builder<String, String> fieldMapBuilder = ImmutableMap.builder();
          fieldMapBuilder.put("diskType", diskType);
          fieldMapBuilder.put("project", project);
          fieldMapBuilder.put("zone", zone);
          fieldValuesMap = fieldMapBuilder.build();
        }
      }
    }
    return fieldValuesMap;
  }

  public String getFieldValue(String fieldName) {
    return getFieldValuesMap().get(fieldName);
  }

  public static ResourceNameFactory<ProjectZoneDiskTypeName> newFactory() {
    return new ResourceNameFactory<ProjectZoneDiskTypeName>() {
      public ProjectZoneDiskTypeName parse(String formattedString) {
        return ProjectZoneDiskTypeName.parse(formattedString);
      }
    };
  }

  public static ProjectZoneDiskTypeName parse(String formattedString) {
    String resourcePath = formattedString;
    if (formattedString.startsWith(SERVICE_ADDRESS)) {
      resourcePath = formattedString.substring(SERVICE_ADDRESS.length());
    }
    Map<String, String> matchMap =
        PATH_TEMPLATE.validatedMatch(
            resourcePath, "ProjectZoneDiskTypeName.parse: formattedString not in valid format");
    return of(matchMap.get("diskType"), matchMap.get("project"), matchMap.get("zone"));
  }

  public static boolean isParsableFrom(String formattedString) {
    String resourcePath = formattedString;
    if (formattedString.startsWith(SERVICE_ADDRESS)) {
      resourcePath = formattedString.substring(SERVICE_ADDRESS.length());
    }
    return PATH_TEMPLATE.matches(resourcePath);
  }

  public static class Builder {
    private String diskType;
    private String project;
    private String zone;

    public String getDiskType() {
      return diskType;
    }

    public String getProject() {
      return project;
    }

    public String getZone() {
      return zone;
    }

    public Builder setDiskType(String diskType) {
      this.diskType = diskType;
      return this;
    }

    public Builder setProject(String project) {
      this.project = project;
      return this;
    }

    public Builder setZone(String zone) {
      this.zone = zone;
      return this;
    }

    private Builder() {}

    public Builder(ProjectZoneDiskTypeName projectZoneDiskTypeName) {
      diskType = projectZoneDiskTypeName.diskType;
      project = projectZoneDiskTypeName.project;
      zone = projectZoneDiskTypeName.zone;
    }

    public ProjectZoneDiskTypeName build() {
      return new ProjectZoneDiskTypeName(this);
    }
  }

  @Override
  public String toString() {
    return SERVICE_ADDRESS
        + PATH_TEMPLATE.instantiate(
            "diskType", diskType,
            "project", project,
            "zone", zone);
  }

  @Override
  public boolean equals(Object o) {
    if (o == this) {
      return true;
    }
    if (o instanceof ProjectZoneDiskTypeName) {
      ProjectZoneDiskTypeName that = (ProjectZoneDiskTypeName) o;
      return Objects.equals(this.diskType, that.getDiskType())
          && Objects.equals(this.project, that.getProject())
          && Objects.equals(this.zone, that.getZone());
    }
    return false;
  }

  @Override
  public int hashCode() {
    return Objects.hash(diskType, project, zone);
  }
}
