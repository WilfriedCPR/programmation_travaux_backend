package com.gescli.ProgrammationTravaux.controller;

import com.gescli.ProgrammationTravaux.dto.ActivityLogDTO;
import com.gescli.ProgrammationTravaux.service.ActivityLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/activity-log")
@RequiredArgsConstructor
public class ActivityLogController {
    private final ActivityLogService service;

    @GetMapping
    public List<ActivityLogDTO> recent() { return service.recent(); }

    @GetMapping("/agent/{agentId}")
    public List<ActivityLogDTO> byAgent(@PathVariable String agentId) { return service.byAgent(agentId); }
}
