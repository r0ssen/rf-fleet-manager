package dk.rf.fleetmanager.service;

import dk.rf.fleetmanager.model.TaskRequest;
import dk.rf.fleetmanager.repository.TaskRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class TaskService {
    private TaskRepository taskRepository;

    public void createTask(TaskRequest taskRequest) {
        taskRepository.save(null);
    }
}
