import java.lang.String;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * TaskPrioritizer class that returns the most urgent
 * available task
 *
 * @author Brandon Newman
 */
public class TaskPrioritizer {
    private class Task {
        String taskId;
        int urgencyLevel;
        int addedTime;
        boolean isResolved;
        boolean isActuallyAdded;
        LinkedList<Task> dependsOn;
        LinkedList<Task> isNeededFor;

        Task(String taskId, int urgencyLevel, int addedTime, boolean isActuallyAdded) {
            this.taskId = taskId;
            this.urgencyLevel = urgencyLevel;
            this.addedTime = addedTime;
            this.isResolved = false;
            this.isActuallyAdded = isActuallyAdded;
            this.dependsOn = new LinkedList<>();
            this.isNeededFor = new LinkedList<>();
        }
    }

    private final int TABLE_SIZE = 500000; // Large power of 2 to reduce collisions
    private LinkedList<Task>[] hashTable;
    private LinkedList<Task>[] urgencyBuckets;
    private int globalTime;
    private final int MAX_URGENCY = 100000; // Assumed upper bound on urgency levels

    /**
     * Constructor to initialize the TaskPrioritizer
     */
    public TaskPrioritizer() {
        /*
         * set up the data structure to handle:
         * O(lgN) add
         * O(lgN) update
         * O(lgN) remove
         * 
         * these need to:
         * search in (lgN)
         * track priority
         */
        @SuppressWarnings("unchecked")
        LinkedList<Task>[] tempHashTable = new LinkedList[TABLE_SIZE];
        hashTable = tempHashTable;

        @SuppressWarnings("unchecked")
        LinkedList<Task>[] tempUrgencyBuckets = new LinkedList[MAX_URGENCY + 1];
        urgencyBuckets = tempUrgencyBuckets;
        for (int i = 0; i < urgencyBuckets.length; i++) {
            urgencyBuckets[i] = new LinkedList<>();
        }
        globalTime = 0;
    }

    private int hash(String taskId) {
        int hash = 0;
        for (int i = 0; i < taskId.length(); i++) {
            hash = (hash * 31 + taskId.charAt(i)) & 0x7fffffff;
        }
        return hash % TABLE_SIZE;
    }

    private Task findTask(String taskId) {
        int idx = hash(taskId);
        LinkedList<Task> bucket = hashTable[idx];
        if (bucket == null)
            return null;
        for (Task task : bucket) {
            if (task.taskId.equals(taskId))
                return task;
        }
        return null;
    }

    private void putTask(Task task) {
        int idx = hash(task.taskId);
        if (hashTable[idx] == null) {
            hashTable[idx] = new LinkedList<>();
        }
        hashTable[idx].add(task);
    }

    /**
     * A method to add a new task
     *
     * @param taskId       The string taskId of the task we want to add
     * @param urgencyLevel The integer urgencyLevel of the task we want to add
     * @param dependencies The array of taskIds of tasks the added task depends on
     */
    public void add(String taskId, int urgencyLevel, String[] dependencies) {
        if (findTask(taskId) != null && findTask(taskId).isActuallyAdded)
            return;

        Task task = findTask(taskId);
        if (task == null) {
            task = new Task(taskId, urgencyLevel, globalTime++, true);
            putTask(task);
        } else {
            task.urgencyLevel = urgencyLevel;
            task.addedTime = globalTime++;
            task.isActuallyAdded = true;
        }

        for (String depId : dependencies) {
            Task dep = findTask(depId);
            if (dep == null) {
                dep = new Task(depId, 0, -1, false);
                putTask(dep);
            }
            task.dependsOn.add(dep);
            dep.isNeededFor.add(task);
        }

        urgencyBuckets[urgencyLevel].add(task);
    }

    /**
     * A method to change the urgency of a task
     *
     * @param taskId       The string taskId of the task we want to change the
     *                     urgency of
     * @param urgencyLevel The new integer urgencyLevel of the task
     */
    public void update(String taskId, int newUrgencyLevel) {
        Task task = findTask(taskId);
        if (task == null || task.isResolved || !task.isActuallyAdded)
            return;

        urgencyBuckets[task.urgencyLevel].remove(task);
        task.urgencyLevel = newUrgencyLevel;
        urgencyBuckets[newUrgencyLevel].add(task);
    }

    /**
     * A method to resolve the greatest urgency task which has had all of its
     * dependencies satisfied
     *
     * @return The taskId of the resolved task
     * @return null if there are no unresolved tasks left
     */
    public String resolve() {
        for (int urgency = MAX_URGENCY; urgency >= 0; urgency--) {
            LinkedList<Task> bucket = urgencyBuckets[urgency];
            for (int i = 0; i < bucket.size(); i++) {
                Task task = bucket.get(i);
                if (!task.isResolved && task.isActuallyAdded && task.dependsOn.isEmpty()) {
                    task.isResolved = true;
                    bucket.remove(i);
                    for (Task dependent : task.isNeededFor) {
                        dependent.dependsOn.remove(task);
                    }
                    return task.taskId;
                }
            }
        }
        return null;
    }
}