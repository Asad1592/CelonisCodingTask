package com.celonis.challenge.services;

import java.net.URL;
import java.time.LocalDate;
import java.time.Period;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;

import org.springframework.stereotype.Service;

import com.celonis.challenge.exceptions.InternalException;
import com.celonis.challenge.exceptions.NotFoundException;
import com.celonis.challenge.model.ProjectGenerationTask;
import com.celonis.challenge.model.ProjectGenerationTaskRepository;

@Service
public class TaskService {

    private final ProjectGenerationTaskRepository projectGenerationTaskRepository;

    private final FileService fileService;


    public TaskService(ProjectGenerationTaskRepository projectGenerationTaskRepository,
                       FileService fileService) {
        this.projectGenerationTaskRepository = projectGenerationTaskRepository;
        this.fileService = fileService;
    }


    public List<ProjectGenerationTask> listTasks() {
        return projectGenerationTaskRepository.findAll();
    }

    public ProjectGenerationTask createTask(ProjectGenerationTask projectGenerationTask) {
        projectGenerationTask.setId(null);
        projectGenerationTask.setCreationDate(new Date());
        return projectGenerationTaskRepository.save(projectGenerationTask);
    }

    public ProjectGenerationTask getTask(String taskId) {
        return get(taskId);
    }

    public ProjectGenerationTask update(String taskId, ProjectGenerationTask projectGenerationTask) {
        ProjectGenerationTask existing = get(taskId);
        existing.setCreationDate(projectGenerationTask.getCreationDate());
        existing.setName(projectGenerationTask.getName());
        return projectGenerationTaskRepository.save(existing);
    }

    public void delete(String taskId) {
        projectGenerationTaskRepository.delete(taskId);
    }

    public void executeTask(String taskId) {
        URL url = Thread.currentThread().getContextClassLoader().getResource("challenge.zip");
        if (url == null) {
            throw new InternalException("Zip file not found");
        }
        try {
            fileService.storeResult(taskId, url);
        } catch (Exception e) {
            throw new InternalException(e);
        }
    }

    private ProjectGenerationTask get(String taskId) {
        ProjectGenerationTask projectGenerationTask = projectGenerationTaskRepository.findOne(taskId);
        if (projectGenerationTask == null) {
            throw new NotFoundException();
        }
        return projectGenerationTask;
    }

    public ProjectGenerationTask monitorTask(String taskId, ProjectGenerationTask projectGenerationTask) {
        ProjectGenerationTask existing = get(taskId);
        existing.setX(projectGenerationTask.getX());
        existing.setY(projectGenerationTask.getY());
        ProjectGenerationTask newObj = projectGenerationTaskRepository.save(existing);
        updateProgress(newObj);
        return newObj;
    }

    public ProjectGenerationTask getMonitorTask(String taskId) {
        return get(taskId);
    }

    public synchronized void  updateProgress(ProjectGenerationTask projectGenerationTask) {
        new Thread(new Runnable() {
            public void run()
            {
                while  (projectGenerationTask.getX() != projectGenerationTask.getY()) {
                    projectGenerationTask.setX(projectGenerationTask.getX()+1);
                    projectGenerationTaskRepository.save(projectGenerationTask);
                    try {
                        Thread.sleep(10000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }).start();
    }

    public void clean() {
        List<ProjectGenerationTask> listOfTasks = listTasks();
        LocalDate now = LocalDate.now();
        ZoneId defaultZoneId = ZoneId.systemDefault();
        for (ProjectGenerationTask task : listOfTasks) {
            LocalDate taskDate = task.getCreationDate().toInstant().atZone(defaultZoneId).toLocalDate();
            Period period = Period.between(now, taskDate);
            int diff = period.getDays();
            if (diff >7) {
                projectGenerationTaskRepository.delete(task.getId());
            }
        }
    }

}
