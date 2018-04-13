package com.eis.upgrade.feign;

import com.eis.exception.EISException;
import com.eis.exception.EISResult;
import io.swagger.annotations.Api;
import org.springframework.cloud.netflix.feign.FeignClient;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient("eis-task")
@Api(value = "eis-task",description = "调度系统接口")
public interface TaskRemoteService {

    @PutMapping("/v1/task/campaign/pauseCampaignJob")
    public EISResult pauseCampaignJob(@RequestParam(value = "jobName") String jobName, @RequestParam("jobGroup") String jobGroup) throws EISException;

    @PutMapping("/v1/task/campaign/resumeCampaignJob")
    public EISResult resumeCampaignJob(@RequestParam(value = "jobName") String jobName, @RequestParam(value = "jobGroup") String jobGroup) throws EISException;
}
