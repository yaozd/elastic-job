/*
 * Copyright 1999-2015 dangdang.com.
 * <p>
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
 * </p>
 */

package com.dangdang.ddframe.job.api.config.impl;

import com.dangdang.ddframe.job.api.config.JobCoreConfiguration;
import com.dangdang.ddframe.job.api.config.JobRootConfiguration;
import com.dangdang.ddframe.job.api.config.JobTypeConfiguration;
import com.dangdang.ddframe.job.api.type.JobType;
import com.dangdang.ddframe.job.api.type.dataflow.api.DataflowJobConfiguration;
import com.dangdang.ddframe.job.api.type.script.api.ScriptJobConfiguration;
import com.dangdang.ddframe.job.api.type.simple.api.SimpleJobConfiguration;
import com.dangdang.ddframe.job.event.JobEventConfiguration;
import com.dangdang.ddframe.job.event.JobTraceEvent.LogLevel;
import com.dangdang.ddframe.job.event.log.JobLogEventConfiguration;
import com.dangdang.ddframe.job.event.rdb.JobRdbEventConfiguration;
import com.dangdang.ddframe.json.GsonFactory;
import com.google.common.collect.Iterables;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 作业配置的Json转换适配器.
 * 
 * @param <T> 作业配置对象泛型
 *     
 * @author zhangliang
 * @author caohao
 */
public abstract class AbstractJobConfigurationGsonTypeAdapter<T extends JobRootConfiguration> extends TypeAdapter<T> {
    
    @Override
    public T read(final JsonReader in) throws IOException {
        String jobName = "";
        String cron = "";
        int shardingTotalCount = 0;
        String shardingItemParameters = "";
        String jobParameter = "";
        boolean failover = false;
        boolean misfire = failover;
        String description = "";
        JobProperties jobProperties = new JobProperties();
        JobEventConfiguration[] jobEventConfigs = null;
        JobType jobType = null;
        String jobClass = "";
        boolean streamingProcess = false;
        String scriptCommandLine = "";
        Map<String, Object> customizedValueMap = new HashMap<>(32, 1);
        in.beginObject();
        while (in.hasNext()) {
            String jsonName = in.nextName();
            switch (jsonName) {
                case "jobName":
                    jobName = in.nextString();
                    break;
                case "cron":
                    cron = in.nextString();
                    break;
                case "shardingTotalCount":
                    shardingTotalCount = in.nextInt();
                    break;
                case "shardingItemParameters":
                    shardingItemParameters = in.nextString();
                    break;
                case "jobParameter":
                    jobParameter = in.nextString();
                    break;
                case "failover":
                    failover = in.nextBoolean();
                    break;
                case "misfire":
                    misfire = in.nextBoolean();
                    break;
                case "description":
                    description = in.nextString();
                    break;
                case "jobProperties":
                    jobProperties = getJobProperties(in);
                    break;
                case "jobEventConfigs":
                    jobEventConfigs = getJobEventConfigs(in);
                    break;
                case "jobType":
                    jobType = JobType.valueOf(in.nextString());
                    break;
                case "jobClass":
                    jobClass = in.nextString();
                    break;
                case "streamingProcess":
                    streamingProcess = in.nextBoolean();
                    break;
                case "scriptCommandLine":
                    scriptCommandLine = in.nextString();
                    break;
                default:
                    addToCustomizedValueMap(jsonName, in, customizedValueMap);
                    break;
            }
        }
        in.endObject();
        JobCoreConfiguration coreConfig = getJobCoreConfiguration(jobName, cron, shardingTotalCount, shardingItemParameters,
                jobParameter, failover, misfire, description, jobProperties, jobEventConfigs);
        JobTypeConfiguration typeConfig = getJobTypeConfiguration(coreConfig, jobType, jobClass, streamingProcess, scriptCommandLine);
        return getJobRootConfiguration(typeConfig, customizedValueMap);
    }
    
    private JobProperties getJobProperties(final JsonReader in) throws IOException {
        JobProperties result = new JobProperties();
        in.beginObject();
        while (in.hasNext()) {
            switch (in.nextName()) {
                case "job_exception_handler":
                    result.put(JobProperties.JobPropertiesEnum.JOB_EXCEPTION_HANDLER.getKey(), in.nextString());
                    break;
                case "executor_service_handler":
                    result.put(JobProperties.JobPropertiesEnum.EXECUTOR_SERVICE_HANDLER.getKey(), in.nextString());
                    break;
                default:
                    break;
            }
        }
        in.endObject();
        return result;
    }
    
    private JobEventConfiguration[] getJobEventConfigs(final JsonReader in) throws IOException {
        List<JobEventConfiguration> result = new ArrayList<>(2);
        in.beginObject();
        while (in.hasNext()) {
            String name = in.nextName();
            switch (name) {
                case "log":
                    in.beginObject();
                    result.add(new JobLogEventConfiguration());
                    in.endObject();
                    break;
                case "rdb":
                    String url = "";
                    String username = "";
                    String password = "";
                    String driverClassName = "";
                    String logLevel = "";
                    in.beginObject();
                    while (in.hasNext()) {
                        switch (in.nextName()) {
                            case "url":
                                url = in.nextString();
                                break;
                            case "username":
                                username = in.nextString();
                                break;
                            case "password":
                                password = in.nextString();
                                break;
                            case "driverClassName":
                                driverClassName = in.nextString();
                                break;
                            case "logLevel":
                                logLevel = in.nextString();
                                break;
                            default:
                                break;
                        }
                    }
                    in.endObject();
                    result.add(new JobRdbEventConfiguration(driverClassName, url, username, password, LogLevel.valueOf(logLevel.toUpperCase())));
                    break;
                default:
                    break;
            }
        }
        in.endObject();
        return Iterables.toArray(result, JobEventConfiguration.class);
    }
    
    protected abstract void addToCustomizedValueMap(final String jsonName, final JsonReader in, final Map<String, Object> customizedValueMap) throws IOException;
    
    private JobCoreConfiguration getJobCoreConfiguration(final String jobName, final String cron, final int shardingTotalCount, 
                                                         final String shardingItemParameters, final String jobParameter, final boolean failover, 
                                                         final boolean misfire, final String description, 
                                                         final JobProperties jobProperties, final JobEventConfiguration[] jobEventConfigs) {
        return JobCoreConfiguration.newBuilder(jobName, cron, shardingTotalCount)
                .shardingItemParameters(shardingItemParameters).jobParameter(jobParameter).failover(failover).misfire(misfire).description(description)
                .jobProperties(JobProperties.JobPropertiesEnum.JOB_EXCEPTION_HANDLER.getKey(), jobProperties.get(JobProperties.JobPropertiesEnum.JOB_EXCEPTION_HANDLER))
                .jobProperties(JobProperties.JobPropertiesEnum.EXECUTOR_SERVICE_HANDLER.getKey(), jobProperties.get(JobProperties.JobPropertiesEnum.EXECUTOR_SERVICE_HANDLER))
                .jobEventConfiguration(jobEventConfigs)
                .build();
    }
    
    private JobTypeConfiguration getJobTypeConfiguration(
            final JobCoreConfiguration coreConfig, final JobType jobType, final String jobClass, final boolean streamingProcess, final String scriptCommandLine) {
        JobTypeConfiguration result;
        switch (jobType) {
            case SIMPLE:
                result = new SimpleJobConfiguration(coreConfig, jobClass);
                break;
            case DATAFLOW:
                result = new DataflowJobConfiguration(coreConfig, jobClass, streamingProcess);
                break;
            case SCRIPT:
                result = new ScriptJobConfiguration(coreConfig, scriptCommandLine);
                break;
            default:
                throw new UnsupportedOperationException(jobType.name());
        }
        return result;
    }
    
    protected abstract T getJobRootConfiguration(final JobTypeConfiguration typeConfig, final Map<String, Object> customizedValueMap);
    
    @Override
    public void write(final JsonWriter out, final T value) throws IOException {
        out.beginObject();
        out.name("jobName").value(value.getTypeConfig().getCoreConfig().getJobName());
        out.name("jobClass").value(value.getTypeConfig().getJobClass());
        out.name("jobType").value(value.getTypeConfig().getJobType().name());
        out.name("cron").value(value.getTypeConfig().getCoreConfig().getCron());
        out.name("shardingTotalCount").value(value.getTypeConfig().getCoreConfig().getShardingTotalCount());
        out.name("shardingItemParameters").value(value.getTypeConfig().getCoreConfig().getShardingItemParameters());
        out.name("jobParameter").value(value.getTypeConfig().getCoreConfig().getJobParameter());
        out.name("failover").value(value.getTypeConfig().getCoreConfig().isFailover());
        out.name("misfire").value(value.getTypeConfig().getCoreConfig().isMisfire());
        out.name("description").value(value.getTypeConfig().getCoreConfig().getDescription());
        out.name("jobProperties").jsonValue(value.getTypeConfig().getCoreConfig().getJobProperties().json());
        out.name("jobEventConfigs").jsonValue(GsonFactory.getGson().toJson(value.getTypeConfig().getCoreConfig().getJobEventConfigs()));
        if (value.getTypeConfig().getJobType() == JobType.DATAFLOW) {
            DataflowJobConfiguration dataflowJobConfig = (DataflowJobConfiguration) value.getTypeConfig();
            out.name("streamingProcess").value(dataflowJobConfig.isStreamingProcess());
        } else if (value.getTypeConfig().getJobType() == JobType.SCRIPT) {
            ScriptJobConfiguration scriptJobConfig = (ScriptJobConfiguration) value.getTypeConfig();
            out.name("scriptCommandLine").value(scriptJobConfig.getScriptCommandLine());
        }
        writeCustomized(out, value);
        out.endObject();
    }
    
    protected abstract void writeCustomized(final JsonWriter out, final T value) throws IOException;
}
