import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class TaskTracker {
    public static void main(String[] args) {
        if(args.length < 1){
            helpPrint();
            return;
        }

        String firstArg = args[0];
        if (Objects.equals(firstArg, "--help") || Objects.equals(firstArg, "-h") || Objects.equals(firstArg, "help")){
            if (args.length == 2) {
                helpPrint(args[1]);
            } else {
                helpPrint();
            }
        }

        Path jsonFilePath = Path.of("./tasks.json");
        if(!Files.exists(jsonFilePath)){
            try {
                Files.createFile(jsonFilePath);
            } catch (IOException e){
                System.err.println("Uh-oh IOException: " + e.getMessage());
            }
        }
        String JSONString;
        try{
             JSONString = Files.readString(jsonFilePath);
        } catch (IOException e){
            System.err.println("Uh-oh IOException: " + e.getMessage());
            return;
        }
        List<JSONObject> JSONObjects = parseJSON(JSONString);

        switch(firstArg){
            case "add":
                addTask(args, JSONObjects);
                break;
            case "update":
                updateTask(args, JSONObjects);
                break;
            case "delete":
                deleteTask(args, JSONObjects);
                break;
            case "mark-in-progress":
                markInProgress(args, JSONObjects);
                break;
            case "mark-done":
                markDone(args, JSONObjects);
                break;
            case "list":
                listTasks(args, JSONObjects);
                break;
            default:
                helpPrint();
        }
        saveToJSON(jsonFilePath, JSONObjects);
    }

    /**
     Saves list of given JSONObject objects by writing them to a file in JSON format
     @param saveToPath path to file to which the JSON object will be written
     @param JSONObjects list to get objects to write
     */
    private static void saveToJSON(Path saveToPath, List<JSONObject> JSONObjects) {
        try{
            FileWriter fileWriter = new FileWriter(String.valueOf(saveToPath));
            PrintWriter writer = new PrintWriter(fileWriter);
            writeJSON(JSONObjects, writer);
            fileWriter.close();
        } catch (IOException err){
            System.out.println("Error: There seems to be the problem with tasks.json. Closing...");
            System.exit(1);
        }
    }

    /**
     Writes JSON object with "task" as array of JSON objects in file
     @param JSONObjects list to get objects to write
     @param writer writer to write formatted strings in file
     */
    private static void writeJSON(List<JSONObject> JSONObjects, PrintWriter writer) {
        writer.write("""
              {
              \t"tasks": [
                """);
        writeJSONBody(JSONObjects, writer);
        writer.write("""
                \n\t]
                }""");
        writer.close();
    }

    /**
     Writes JSON objects in file for each JSONObject object in JSONObjects list
     @param JSONObjects list to get objects to write
     @param writer writer to write formatted strings in file
     */
    private static void writeJSONBody(List<JSONObject> JSONObjects, PrintWriter writer) {
        for(JSONObject task: JSONObjects){
            writer.printf("""
                        \t\t{
                        \t\t\t"id": "%d",
                        \t\t\t"description": "%s",
                        \t\t\t"status": "%d",
                        \t\t\t"createdAt": "%s",
                        \t\t\t"updatedAt": "%s"
                        \t\t}"""
                    , task.getId(),
                    task.getDescription(),
                    task.getStatus().ordinal(),
                    task.getCreatedAt().toString(),
                    task.getUpdatedAt().toString());

            if(JSONObjects.indexOf(task) != JSONObjects.indexOf(JSONObjects.getLast())){
                writer.write(",\n");
            }
        }
    }

    private static void addTask(String[] args, List<JSONObject> JSONObjects) {
        if(args.length != 2){helpPrint(args[0]); return;}

        int id;
        if(JSONObjects.isEmpty()){
            id = 0;
        } else {
            id = JSONObjects.getLast().getId() + 1;
        }

        String description = args[1];

        LocalDateTime createdAt = LocalDateTime.now();

        JSONObjects.add(new JSONObject(id, description, TaskStatus.todo, createdAt, createdAt));
        System.out.printf("Task \"%s\" added successfully(ID: %d)\n", description, id);
    }

    private static void updateTask(String[] args, List<JSONObject> JSONObjects) {
        if(args.length != 3){helpPrint(args[0]); return;}

        int id = parseID(args[1]);

        int objectIndex = indexValidator(id, JSONObjects);


        JSONObject updatedObject = JSONObjects.get(objectIndex);
        String oldDescription = updatedObject.getDescription();

        String newDescription = args[2];
        updatedObject.setDescription(newDescription);

        updateTime(updatedObject);
        System.out.printf("Task ID: %d \"%s\" updated to \"%s\" successfully\n", id, oldDescription, newDescription);
    }

    private static void deleteTask(String[] args, List<JSONObject> JSONObjects) {
        if(args.length != 2){helpPrint(args[0]); return;}

        int id = parseID(args[1]);
        int objectIndex = indexValidator(id, JSONObjects);
        String taskDescription = JSONObjects.get(objectIndex).getDescription();
        JSONObjects.remove(objectIndex);
        System.out.printf("Task ID: %d \"%s\" deleted successfully\n", id, taskDescription);
    }

    private static void markInProgress(String[] args, List<JSONObject> JSONObjects) {
        if(args.length != 2){helpPrint(args[0]); return;}

        int objectIndex = indexValidator(parseID(args[1]), JSONObjects);

        JSONObject editedObject = JSONObjects.get(objectIndex);

        TaskStatus currentStatus = editedObject.getStatus();
        int id = editedObject.getId();
        String taskDescription = editedObject.getDescription();

        if(currentStatus != TaskStatus.in_progress){
            editedObject.setStatus(TaskStatus.in_progress);
        } else {
            editedObject.setStatus(TaskStatus.todo);
        }
        updateTime(editedObject);
        System.out.printf("Task ID: %d \"%s\" marked as \"%s\"\n", id, editedObject.getDescription(), statusToString(editedObject.getStatus()));
    }

    private static void markDone(String[] args, List<JSONObject> JSONObjects) {
        if(args.length != 2){helpPrint(args[0]); return;}

        int id = parseID(args[1]);
        int objectIndex = indexValidator(id, JSONObjects);

        JSONObject editedObject = JSONObjects.get(objectIndex);

        TaskStatus currentStatus = editedObject.getStatus();

        if(currentStatus != TaskStatus.done){
            editedObject.setStatus(TaskStatus.done);
            updateTime(editedObject);
            System.out.printf("Task ID: %d \"%s\" marked as \"%s\"\n", id, editedObject.getDescription(), statusToString(editedObject.getStatus()));
        } else {
            System.out.printf("Task ID: %d already is marked as done!\n", id);
            System.exit(1);
        }
    }

    private static void listTasks(String[] args, List<JSONObject> JSONObjects) {
        if(args.length > 2){helpPrint(args[0]); return;}
        if(JSONObjects.isEmpty()){
            System.out.println("The task list is empty! Add some tasks to it");
            System.exit(1);
        }
        int condition;
        if(args.length == 1){
            condition = -1;
            conditionalAllTasksPrint(condition, JSONObjects);
            return;
        }
        switch(args[1].toLowerCase()){
            case "todo":
                condition = 0;
                break;
            case "in-progress":
                condition = 1;
                break;
            case "done":
                condition = 2;
                break;
            default:
                System.out.println("Wrong argument. Printing help");
                helpPrint("list");
                return;
        }
        conditionalAllTasksPrint(condition, JSONObjects);
    }

    /**
     Checks if status condition fits JSONObject object status to print the object
     @param condition status enum ordinal to compare to. -1 = no condition
     */
    private static void conditionalAllTasksPrint(int condition, List<JSONObject> JSONObjects){
        for(JSONObject task:JSONObjects){
            if(condition == task.getStatus().ordinal() || condition == -1){
                printTask(task);
            }
        }
    }

    /**
     Formats JSONObject object and prints it in console
     @param task object to format and print
     */
    private static void printTask(JSONObject task){
        String createdAt = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.FULL, FormatStyle.SHORT).format(task.getCreatedAt());
        String updatedAt = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.FULL, FormatStyle.SHORT).format(task.getUpdatedAt());
        System.out.printf(
                """
                        ID: %d
                        Task: %s
                        Marked as: %s
                        Created at: %s
                        Updated at: %s

                        """, task.getId(), task.getDescription(), statusToString(task.getStatus()), createdAt, updatedAt);
    }

    /**
     Converts status enums to strings
     @param statusCode enum object to convert to string
     @return converted status (examples: TODO; done; in-progress)
     */
    private static String statusToString(TaskStatus statusCode){
        return switch (statusCode) {
            case TaskStatus.todo -> "TODO";
            case TaskStatus.in_progress -> "in-progress";
            case TaskStatus.done -> "done";
            default -> "TODO";
        };
    }

    /**
     Makes list of JSON objects from tasks.json file to work with
     @param fileContents string from json file to parse
     @return reference to list with either JSONObject class objects in or empty list
     */
    private static List<JSONObject> parseJSON(String fileContents){
        if(fileContents.isEmpty()){
            return new ArrayList<JSONObject>();
        }
        String JSONMonolith = fileContents.substring(fileContents.indexOf('[')+1, fileContents.lastIndexOf(']'));

        JSONMonolith = JSONMonolith.strip().replaceAll("^\\{|}$", "");

        String[] Objects = JSONMonolith.split("},[\r\n]+\t*\\{", 0);
        if (Objects[0] == ""){
            return new ArrayList<JSONObject>();
        }
        List<JSONObject> JSONObjects = new ArrayList<JSONObject>();
        for(String Object:Objects){
            // splitting json object for arguments(id, description, etc.)
            String[] Attrs = Object.split(".+: |[,\r\n]+", 0);

            // removing "" from arguments
            int i = 0;
            for(String attr:Attrs){
                Attrs[i] =  attr.replaceAll("\"", "");
                i++;
            }

            int ID = Integer.parseInt(Attrs[2]);

            int statusCode = Integer.parseInt(Attrs[6]);
            TaskStatus status = TaskStatus.values()[statusCode];

            LocalDateTime createdAt = LocalDateTime.parse(Attrs[8]);

            LocalDateTime updatedAt = LocalDateTime.parse(Attrs[10]);

            JSONObjects.add(new JSONObject(ID, Attrs[4], status, createdAt , updatedAt));
        }

        return JSONObjects;

    }

    /**
     Binary search to find index of object with searched id
     @param idSearchGoal desired id
     @param JSONObjects List of JSONObject objects to search from
     @return Index of object with parameter id
     @throws Exception custom exception that says "No object with such id, try list"
     */
    private static int idBinarySearch(int idSearchGoal, List<JSONObject> JSONObjects) throws Exception {
        Exception NoObjectFound = new Exception("No object with such id, try list");
        if (JSONObjects.isEmpty()){throw NoObjectFound;}

        int midIndex;
        int midID;
        int firstIndex = 0;
        int lastIndex = JSONObjects.size()-1;


        if (idSearchGoal < JSONObjects.get(firstIndex).getId()
            || idSearchGoal > JSONObjects.get(lastIndex).getId()) {
            throw NoObjectFound;
        }

        while(firstIndex<=lastIndex){
            midIndex = (lastIndex+firstIndex)/2;
            midID = JSONObjects.get(midIndex).getId();

            if( midID == idSearchGoal){
                return midIndex;
            } else if (midID > idSearchGoal ){
                lastIndex = midIndex-1;
            } else {
                firstIndex = midIndex+1;
            }
        }

        throw NoObjectFound;
    }

    /**
     idBinarySearch wrapper to print out exception,
     see idBinarySearch function for more info
     */
    private static int indexValidator(int idSearchGoal, List<JSONObject> JSONObjects){
        try{
            return(idBinarySearch(idSearchGoal, JSONObjects));
        } catch(Exception notFound){
            System.err.println(notFound.getMessage());
            System.exit(1);
        }
        return 0;
    }

    /**
     Integer.parseInt wrapper to work with exception in user input,
     outputs error message if argument can't be parsed as int
     @param idArg argument from command line args that should correspond to ID
     @return Int equivalent of argument of id
     */
    private static int parseID(String idArg){
        try {
            return (Integer.parseInt(idArg));
        } catch (NumberFormatException ignored){
            System.err.println("Provided ID is not an integer.\nExamples of ID value: 1, 2, 10, 25");
            System.exit(1);
        }
        return 0;
    }

    /**
     Updates "updatedAt" field of given JSONObject object
     @param updatedObject JSONObject object to update time
     */
    private static void updateTime(JSONObject updatedObject){
        LocalDateTime updatedAt = LocalDateTime.now();
        updatedObject.setUpdatedAt(updatedAt);
    }
    private static void helpPrint(){
        System.out.println(
                """
                        About: This is task tracker made with java
                        Available commands:
                        -\t--help or -h: Outputs help.
                        \tIf using command after help like "task-tracker-java -h add"
                        \tProgram will output help specifically about given command

                        -\tadd: adds a task
                        -\tupdate: updates a task
                        -\tdelete: deletes a task
                        -\tmark-in-progress: marks task as in progress
                        -\tmark-done: marks task as done
                        -\tlist: lists tasks""");
    }
    private static void helpPrint(String arg){
        switch(arg){
            case "add":
                System.out.println(
                        "Name of the argument:" + arg +"\n" +
                        "Description: Adds a task to the task list\n" +
                        "Needs one argument - description of the task\n" +
                        "Example: task-tracker-java add \"Play with the cat\"\n" +
                        "Output: Task \"Play with the cat\" added successfully(ID: 1)");
                break;
            case "update":
                System.out.println(
                        "Name of the argument:" + arg +"\n" +
                        "Description: Updates an already existing task\n" +
                        "Needs two arguments:\n" +
                                "\t1)ID of the task\n" +
                                "\t2)Updated description of the task\n" +
                        "Example: task-tracker-java update 1\"Play with the dog\"\n" +
                        "Output: Task ID: 1 \"Play with the cat\" updated to \"Play with the dog\" successfully");
                break;
            case "delete":
                System.out.println(
                        "Name of the argument:" + arg +"\n" +
                        "Description: Deletes an already existing task\n" +
                        "Needs one argument - ID of the task\n" +
                        "Example: task-tracker-java delete 1\n" +
                        "Output: Task ID: 1 \"Play with the dog\" deleted successfully");
                break;
            case "mark-in-progress":
                System.out.println(
                        "Name of the argument:" + arg +"\n" +
                        "Description: " +
                                "\tMarks an already existing task as \"In progress\"\n" +
                                "\tor if its already marked as such resets it to \"TODO\"\n" +
                        "Needs one argument - ID of the task\n" +
                        "Example: task-tracker-java mark-in-progress 1\n" +
                        "Output: Task ID: 1 \"Play with the cat\" marked as \"in-progress\"");
                break;
            case "mark-done":
                System.out.println(
                        "Name of the argument:" + arg +"\n" +
                        "Description: Marks an already existing task as \"Done\"\n" +
                        "Needs one argument - ID of the task\n" +
                        "Example: task-tracker-java mark-done 1\n" +
                        "Output: Task ID: 1 \"Play with the cat\" marked as \"Done\"");
                break;
            case "list":
                System.out.println(
                        "Name of the argument:" + arg +"\n" +
                        "Description: Lists tasks\n" +
                        "Arguments (one):\n" +
                                "\t1)None: Lists every task\n" +
                                "\t2)done: Lists every task marked \"done\"\n" +
                                "\t3)todo: Lists every task marked \"TODO\"\n" +
                                "\t4)in-progress: Lists every task marked \"In progress\"\n" +
                        "Example:\n" +
                                "\t1)task-tracker-java list\n" +
                                "\t2)task-tracker-java list todo\n" +
                        "Output:\n" +
                                "\tID: 0 \n\tTask: \"Play with the cat\" \n\tMarked as: done\n\tCreated at: *date*\n\tUpdated at: *date*\n\n" +
                                "\tID: 1 \n\tTask: \"Buy groceries\" \n\tMarked as: TODO\n\tCreated at: *date*\n\tUpdated at: *date*");
                break;
            case "--help":
            case "-h":
                helpPrint();
                break;
            default:
                helpPrint();
        }
    }
}