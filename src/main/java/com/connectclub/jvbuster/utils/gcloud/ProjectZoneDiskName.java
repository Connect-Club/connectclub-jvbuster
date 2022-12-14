package com.connectclub.jvbuster.utils.gcloud;

import com.google.api.pathtemplate.PathTemplate;
import com.google.api.resourcenames.ResourceName;
import com.google.api.resourcenames.ResourceNameFactory;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;

import java.util.Map;
import java.util.Objects;

public final class ProjectZoneDiskName implements ResourceName {
    private final String disk;
    private final String project;
    private final String zone;
    private static final PathTemplate PATH_TEMPLATE =
            PathTemplate.createWithoutUrlEncoding("{project}/zones/{zone}/disks/{disk}");

    public static final String SERVICE_ADDRESS =
            "https://www.googleapis.com/compute/v1/projects/";

    private volatile Map<String, String> fieldValuesMap;

    public static Builder newBuilder() {
        return new Builder();
    }

    public Builder toBuilder() {
        return new Builder(this);
    }

    private ProjectZoneDiskName(Builder builder) {
        disk = Preconditions.checkNotNull(builder.getDisk());
        project = Preconditions.checkNotNull(builder.getProject());
        zone = Preconditions.checkNotNull(builder.getZone());
    }

    public static ProjectZoneDiskName of(String disk, String project, String zone) {
        return newBuilder().setDisk(disk).setProject(project).setZone(zone).build();
    }

    public static String format(String disk, String project, String zone) {
        return of(disk, project, zone).toString();
    }

    public String getDisk() {
        return disk;
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
                    fieldMapBuilder.put("disk", disk);
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

    public static ResourceNameFactory<ProjectZoneDiskName> newFactory() {
        return new ResourceNameFactory<ProjectZoneDiskName>() {
            public ProjectZoneDiskName parse(String formattedString) {
                return ProjectZoneDiskName.parse(formattedString);
            }
        };
    }

    public static ProjectZoneDiskName parse(String formattedString) {
        String resourcePath = formattedString;
        if (formattedString.startsWith(SERVICE_ADDRESS)) {
            resourcePath = formattedString.substring(SERVICE_ADDRESS.length());
        }
        Map<String, String> matchMap =
                PATH_TEMPLATE.validatedMatch(
                        resourcePath, "ProjectZoneDiskName.parse: formattedString not in valid format");
        return of(matchMap.get("disk"), matchMap.get("project"), matchMap.get("zone"));
    }

    public static boolean isParsableFrom(String formattedString) {
        String resourcePath = formattedString;
        if (formattedString.startsWith(SERVICE_ADDRESS)) {
            resourcePath = formattedString.substring(SERVICE_ADDRESS.length());
        }
        return PATH_TEMPLATE.matches(resourcePath);
    }

    public static class Builder {
        private String disk;
        private String project;
        private String zone;

        public String getDisk() {
            return disk;
        }

        public String getProject() {
            return project;
        }

        public String getZone() {
            return zone;
        }

        public Builder setDisk(String disk) {
            this.disk = disk;
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

        public Builder(ProjectZoneDiskName projectZoneDiskName) {
            disk = projectZoneDiskName.disk;
            project = projectZoneDiskName.project;
            zone = projectZoneDiskName.zone;
        }

        public ProjectZoneDiskName build() {
            return new ProjectZoneDiskName(this);
        }
    }

    @Override
    public String toString() {
        return SERVICE_ADDRESS
                + PATH_TEMPLATE.instantiate(
                "disk", disk,
                "project", project,
                "zone", zone);
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (o instanceof ProjectZoneDiskName) {
            ProjectZoneDiskName that = (ProjectZoneDiskName) o;
            return Objects.equals(this.disk, that.getDisk())
                    && Objects.equals(this.project, that.getProject())
                    && Objects.equals(this.zone, that.getZone());
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(disk, project, zone);
    }
}

