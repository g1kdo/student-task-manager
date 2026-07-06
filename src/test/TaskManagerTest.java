package test;

import app.Task;
import app.TaskManager;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TaskManagerTest {

    @Test
    public void testAddTask(){

        TaskManager manager = new TaskManager();

        manager.addTask("Study");

        assertEquals(1, manager.getTaskCount());
    }

    @Test
    public void testCompleteTask() {

        TaskManager manager = new TaskManager();

        manager.addTask("Revise");
        manager.completeTask(0);

        assertTrue(manager.isTaskCompleted(0), "Should be true!");
    }
}
