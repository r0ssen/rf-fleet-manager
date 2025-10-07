package dk.rf.fleetmanager.controller;

import dk.rf.fleetmanager.model.TaskRequest;
import dk.rf.fleetmanager.repository.TestEntityRepository;
import dk.rf.fleetmanager.service.TaskService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/tasks")
@RequiredArgsConstructor
public class TaskController {
    private TaskService taskService;

    @GetMapping("/ping")
    public String ping() {
        return "pong";
    }

    @PostMapping
    public void createTask(@RequestBody @Valid TaskRequest taskRequest) {
        if (!taskRequest.endTime().isAfter(taskRequest.startTime())) {
            throw new IllegalArgumentException("incorrect input");
        }
        taskService.createTask(taskRequest);
    }
}
