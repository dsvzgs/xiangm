package mengxu.gp.terminal;

import mengxu.simulation.state.SystemState;
import mengxu.taskscheduling.Server;
import mengxu.taskscheduling.TaskOption;
import org.apache.commons.lang3.math.NumberUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The attributes of the job shop.
 * NOTE: All the attributes are relative to the current time.
 *       This is for making the decision making process memoryless,
 *       i.e. independent of the current time.
 *
 * @author yimei
 */

public enum JobShopAttribute1 {
    CURRENT_TIME("t"), // the current time
    // The machine-related attributes (independent of the jobs in the queue of the machine).
    NUM_OPS_IN_QUEUE("NIQ"), // the number of tasks in the queue
    MACHINE_READY_TIME("MRT"), // the ready time of the server or mobileDevice
    // The job/operation-related attributes (depend on the jobs in the queue).
    PROC_TIME("PT"), // the processing time of the task
    WEIGHT("W"), // the job weight
    RELEASE_TIME("RT"), // the release time
    TIME_IN_SYSTEM("TIS"),// time in system = t - releaseTime
    WORK_IN_QUEUE("WIQ"),
    UPLOAD_TIME("UT"),
    DOWNLOAD_TIME("DT"),
    TOTAL_TIME_IN_QUEUE("TTIQ"),
    NUM_TASK_OF_JOB("NTJ"),
    TASK_WAITING_TIME("TWT"),
    WORK_REMAINING("WR"),
    NUM_TASK_REMAINING("NTR");


    private final String name;

    JobShopAttribute1(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    // Reverse-lookup map
    private static final Map<String, JobShopAttribute1> lookup = new HashMap<>();

    static {
        for (JobShopAttribute1 a : JobShopAttribute1.values()) {
            lookup.put(a.getName(), a);
        }
    }

    public static JobShopAttribute1 get(String name) {
        return lookup.get(name);
    }

    public double value(TaskOption taskOption, Server server, SystemState systemState
    		) {
        double value = -1;

        switch (this) {
            case CURRENT_TIME:
                value = systemState.getClockTime();
                break;
            case NUM_OPS_IN_QUEUE:
                if(taskOption.getServer() == null){
                    value = taskOption.getMobileDevice().getQueue().size();
                }
                else{
                    value = server.getQueue().size();
                }
                break;
            case MACHINE_READY_TIME:
                if(taskOption.getServer() == null){
                    value = taskOption.getMobileDevice().getReadyTime();
                }
                else{
                    value = server.getReadyTime();
                }
                break;
            case PROC_TIME:
                value = taskOption.getProcTime();
                break;
            case WEIGHT:
                value = taskOption.getTask().getJob().getWeight();
                break;
            case RELEASE_TIME:
                value = taskOption.getTask().getJob().getReleaseTime();
                break;
            case TIME_IN_SYSTEM:
                value = systemState.getClockTime() - taskOption.getTask().getJob().getReleaseTime();
                break;
            case WORK_IN_QUEUE:
                if(taskOption.getServer() == null){
                    value = taskOption.getMobileDevice().totalProcTimeInQueue();
                }
                else{
                    value = server.totalProcTimeInQueue();
                }
                break;
            case TOTAL_TIME_IN_QUEUE:
                if(taskOption.getServer() == null){
                    value = taskOption.getMobileDevice().totalProcTimeAndUpLoadAndDownLoadTimeInQueue();
                }
                else{
                    value = server.totalProcTimeAndUpLoadAndDownLoadTimeInQueue();
                }
                break;
            case UPLOAD_TIME:
                value = taskOption.getUploadDelay();
                break;
            case DOWNLOAD_TIME:
                value = taskOption.getDownloadDelay();
                break;
            case NUM_TASK_OF_JOB:
                value = taskOption.getTask().getJob().getTaskList().size();
                break;
            case TASK_WAITING_TIME:
                value = systemState.getClockTime() - taskOption.getReadyTime();
                break;
//            case WORK_REMAINING:
//                value = taskOption.getWorkRemaining();
//                break;
            case NUM_TASK_REMAINING:
                value = taskOption.getTask().getJob().getTaskList().size()-taskOption.getTask().getJob().getCompletedTaskNumber();
                break;
           default:
                System.err.println("Undefined attribute " + name);
                System.exit(1);
        }

        return value;
    }

    public static double valueOfString(String attribute, TaskOption taskOption, Server server,
                                       SystemState systemState,
                                       List<JobShopAttribute1> ignoredAttributes) {
        JobShopAttribute1 a = get(attribute);
        if (a == null) {
            if (NumberUtils.isNumber(attribute)) {
                return Double.valueOf(attribute);
            } else {
                System.err.println(attribute + " is neither a defined attribute nor a number.");
                System.exit(1);
            }
        }

        if (ignoredAttributes.contains(a)) {
            return 1.0;
        } else {
        	  return a.value(taskOption, server, systemState);
        }
    }

    /**
     * Return the basic attributes.
     * @return the basic attributes.
     */
    public static JobShopAttribute1[] basicAttributes() {
        return new JobShopAttribute1[]{
                JobShopAttribute1.CURRENT_TIME,
                JobShopAttribute1.NUM_OPS_IN_QUEUE,
                JobShopAttribute1.MACHINE_READY_TIME,
                JobShopAttribute1.PROC_TIME,
                JobShopAttribute1.WEIGHT,
                JobShopAttribute1.WORK_IN_QUEUE,
                JobShopAttribute1.UPLOAD_TIME,
                JobShopAttribute1.DOWNLOAD_TIME,
        };
    }

    /**
     * The attributes relative to the current time.
     * @return the relative attributes.
     */
    //for flexible JSSP
    //fzhang 19.7.2018 for flexible, the next processing time do not know, because we do not know the next operation will
    //be allocated in which machine:  baseline
    public static JobShopAttribute1[] relativeAttributes() {
        return new JobShopAttribute1[]{
                //server related
                JobShopAttribute1.NUM_OPS_IN_QUEUE,
                JobShopAttribute1.WORK_IN_QUEUE,
                JobShopAttribute1.MACHINE_READY_TIME,
                JobShopAttribute1.UPLOAD_TIME,
                JobShopAttribute1.DOWNLOAD_TIME,
                JobShopAttribute1.PROC_TIME,
                JobShopAttribute1.TOTAL_TIME_IN_QUEUE,

                //job related
                JobShopAttribute1.TIME_IN_SYSTEM,
//                JobShopAttribute.NUM_TASK_OF_JOB,
                JobShopAttribute1.TASK_WAITING_TIME,
//                JobShopAttribute.WORK_REMAINING,
                JobShopAttribute1.NUM_TASK_REMAINING,
//                JobShopAttribute.WEIGHT,
        };
    }
}
