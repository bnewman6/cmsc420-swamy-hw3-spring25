import java.lang.String;
import java.util.ArrayList;
import java.util.LinkedList;

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

    private class UrgencyBucket {
        int urgency;
        LinkedList<Task> tasks;

        UrgencyBucket(int urgency) {
            this.urgency = urgency;
            this.tasks = new LinkedList<>();
        }
    }

    private final int TABLE_SIZE = 500000;
    private LinkedList<Task>[] hashTable;
    private ArrayList<UrgencyBucket> urgencyList;
    private int globalTime;

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

        urgencyList = new ArrayList<>();
        globalTime = 0;
    }

    /*
     * hash function
     */
    private int hash(String taskId) {
        int hash = 0;
        for (int i = 0; i < taskId.length(); i++) {
            hash = (hash * 31 + taskId.charAt(i)) & 0x7fffffff;
        }
        return hash % TABLE_SIZE;
    }

    /*
     * find the task with the given taskId in the hash table.
     * If not found, return null
     */
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

    /*
     * add the given task to the hash table.
     */
    private void putTask(Task task) {
        int idx = hash(task.taskId);
        if (hashTable[idx] == null) {
            hashTable[idx] = new LinkedList<>();
        }
        hashTable[idx].add(task);
    }

    private UrgencyBucket getOrCreateBucket(int urgency) {
        for (UrgencyBucket bucket : urgencyList) {
            if (bucket.urgency == urgency)
                return bucket;
        }
        UrgencyBucket newBucket = new UrgencyBucket(urgency);
        int i = 0;
        while (i < urgencyList.size() && urgencyList.get(i).urgency > urgency)
            i++;
        urgencyList.add(i, newBucket);
        return newBucket;
    }

    // When a task is updated, it needs to be moved to the correct spot in the new
    // urgency bucket
    private void insertSorted(LinkedList<Task> list, Task task) {
        int i = 0;
        while (i < list.size() && list.get(i).addedTime < task.addedTime) {
            i++;
        }
        list.add(i, task);
    }

    /**
     * A method to add a new task
     *
     * @param taskId       The string taskId of the task we want to add
     * @param urgencyLevel The integer urgencyLevel of the task we want to add
     * @param dependencies The array of taskIds of tasks the added task depends on
     */
    public void add(String taskId, int urgencyLevel, String[] dependencies) {
        // checks if task already added
        if (findTask(taskId) != null && findTask(taskId).isActuallyAdded)
            return;

        Task task = findTask(taskId);
        if (task == null) {
            task = new Task(taskId, urgencyLevel, globalTime++, true);
            putTask(task);
        } else {
            // adding existing dependency
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
            if (!dep.isResolved) {
                task.dependsOn.add(dep);
                dep.isNeededFor.add(task);
            }
        }

        if (task.dependsOn.isEmpty()) {
            UrgencyBucket bucket = getOrCreateBucket(urgencyLevel);
            bucket.tasks.add(task);
        }
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

        for (UrgencyBucket bucket : urgencyList) {
            bucket.tasks.remove(task);
        }

        task.urgencyLevel = newUrgencyLevel;
        if (task.dependsOn.isEmpty()) {
            UrgencyBucket bucket = getOrCreateBucket(newUrgencyLevel);
            int i = 0;
            while (i < bucket.tasks.size() && bucket.tasks.get(i).addedTime < task.addedTime)
                i++;
            bucket.tasks.add(i, task);
        }
    }

    /**
     * A method to resolve the greatest urgency task which has had all of its
     * dependencies satisfied
     *
     * @return The taskId of the resolved task
     * @return null if there are no unresolved tasks left
     */
    public String resolve() {
        for (UrgencyBucket bucket : urgencyList) {
            LinkedList<Task> tasks = bucket.tasks;
            for (int i = 0; i < tasks.size(); i++) {
                Task task = tasks.get(i);
                if (!task.isResolved && task.isActuallyAdded && task.dependsOn.isEmpty()) {
                    task.isResolved = true;
                    tasks.remove(i);
                    for (Task dependent : task.isNeededFor) {
                        dependent.dependsOn.remove(task);
                        if (!dependent.isResolved && dependent.isActuallyAdded && dependent.dependsOn.isEmpty()) {
                            UrgencyBucket depBucket = getOrCreateBucket(dependent.urgencyLevel);
                            int j = 0;
                            while (j < depBucket.tasks.size() && depBucket.tasks.get(j).addedTime < dependent.addedTime)
                                j++;
                            depBucket.tasks.add(j, dependent);
                        }
                    }
                    return task.taskId;
                }
            }
        }
        return null;
    }
}