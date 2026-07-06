package app;

import java.util.Scanner;

public class Main {

    public static void main(String[] args) {

        Scanner scanner = new Scanner(System.in);

        TaskManager manager = new TaskManager();

        while(true){

            System.out.println("\nTask Manager");

            System.out.println("1. Add Task");
            System.out.println("2. View Tasks");
            System.out.println("3. Complete Task");
            System.out.println("4. Delete Task");
            System.out.println("5. Health Check");
            System.out.println("6. Exit");

            System.out.print("\nEnter your choice: ");
            String choice = scanner.nextLine();
            boolean isDigit = choice.matches("\\d+");
            if (!isDigit) {
                System.out.println("Input a valid digit ");
                continue;
            }
            int validChoice = Integer.parseInt(choice);

            switch(validChoice){

                case 1:
                    System.out.print("Task name: ");
                    String name = scanner.nextLine();
                    System.out.println("[LOG] Adding task...");
                    manager.addTask(name);
                    break;

                case 2:
                    System.out.println("[LOG] Viewing tasks...");
                    manager.viewTasks();
                    break;

                case 3:
                    System.out.print("Task number: ");
                    manager.completeTask(scanner.nextInt()-1);
                    break;

                case 4:
                    System.out.print("Task number: ");
                    manager.deleteTask(scanner.nextInt()-1);
                    break;

                case 5:
                    System.out.println("Application is running.");
                    break;

                case 6:
                    System.exit(0);

                default:
                    System.out.println("Choice not valid:(");
            }
        }
    }
}