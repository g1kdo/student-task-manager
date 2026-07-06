package app;

import java.util.ArrayList;

public class TaskManager {

    private ArrayList<Task> tasks = new ArrayList<>();

    public void addTask(String name) {
        tasks.add(new Task(name));
        System.out.println("Task added:)");
    }

    public void viewTasks() {

        if (tasks.isEmpty()) {
            System.out.println("No tasks:(");
            return;
        }

        for (int i=0;i<tasks.size();i++) {

            Task task = tasks.get(i);

            String status = task.isCompleted() ? "Done" : "Pending";

            System.out.println((i+1)+". "+task.getName()+" - "+status);
        }
    }

    public void completeTask(int index) {

        if(index>=0 && index<tasks.size()){

            tasks.get(index).complete();

            System.out.println("Task completed:)");
        } else {
            System.out.println("Task not found:(");
        }
    }

    public void deleteTask(int index) {

        if(index>=0 && index<tasks.size()){

            tasks.remove(index);

            System.out.println("Task deleted:)");
        } else {
            System.out.println("Task not found:(");
        }
    }
}