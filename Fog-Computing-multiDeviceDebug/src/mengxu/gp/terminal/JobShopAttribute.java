package mengxu.gp.terminal;

import mengxu.rule.RuleType;
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

public enum JobShopAttribute {
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

    private static final Map<JobShopAttribute, Double> activationProbabilitiess = new HashMap<>();
//qgl
    private final String name;
    private static final Map<RuleType, Map<JobShopAttribute, Double>> activationProbabilities = new HashMap<>();

    JobShopAttribute(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
    public static double getActivationProbability(JobShopAttribute attr) {
        return activationProbabilitiess.getOrDefault(attr, 0.05);
    }//qgl
    public static double getActivationProbability(JobShopAttribute attr, RuleType ruleType) {
        //System.out.println("有规则");
        return activationProbabilities.get(ruleType).getOrDefault(attr, 0.05);
    }

    // Reverse-lookup map
    private static final Map<String, JobShopAttribute> lookup = new HashMap<>();

    static {
        for (JobShopAttribute attr : JobShopAttribute.values()) {
        activationProbabilitiess.put(attr, 0.5);}
        for (RuleType ruleType : RuleType.values()) {
            activationProbabilities.put(ruleType, new HashMap<>());
            for (JobShopAttribute attr : JobShopAttribute.values()) {
                if(ruleType == RuleType.ROUTING || attr == JobShopAttribute.PROC_TIME)
                {
                    activationProbabilities.get(ruleType).put(attr, 1.0);
                }
                    if( ruleType == RuleType.SEQUENCING || attr == JobShopAttribute.NUM_TASK_REMAINING){
                        activationProbabilities.get(ruleType).put(attr, 0.8);
                    }
                activationProbabilities.get(ruleType).put(attr, 0.5);  // ✅ 初始化 SEQUENCING & ROUTING AP
            }
        }
    }
    public boolean isDynamic() {
        return this == NUM_OPS_IN_QUEUE || this == WORK_IN_QUEUE || this == TIME_IN_SYSTEM ||
                this == NUM_TASK_REMAINING || this == CURRENT_TIME || this == TOTAL_TIME_IN_QUEUE ||
                this == WORK_REMAINING || this == TASK_WAITING_TIME || this == NUM_TASK_OF_JOB;
    }

    /**
     * 判断属性是否是静态元素
     * @return 是否是静态元素
     */
    public boolean isStatic() {
        return this == PROC_TIME || this == WEIGHT || this == RELEASE_TIME ||
                this == UPLOAD_TIME || this == DOWNLOAD_TIME || this == MACHINE_READY_TIME;
    }


    public static void updateActivationProbabilities(Map<JobShopAttribute, Integer> activeCounts,
                                                     Map<JobShopAttribute, Integer> totalCounts,
                                                     RuleType ruleType) {

        for (JobShopAttribute attr : JobShopAttribute.values()) {
            // System.out.println(ruleType);    //qgl
           // System.out.println("Attribute " + attr.getName());
           // System.out.println(activeCounts);


            int active = activeCounts.getOrDefault(attr,   0);
           // System.out.println("Attribute " + attr.getName() + " has active count " + active);
            int total = totalCounts.getOrDefault(attr, 0);
            //System.out.println( "Attribute " + attr.getName() + " has total count " + total);
            double AP =  total == 0 ? 0.05 : Math.max(0.05, (double) (active) / (total));  // 默认 revive 概率 0.05

//
//            if (ruleType == RuleType.ROUTING) {
//                if (attr == JobShopAttribute.PROC_TIME ) {
//                    AP = Math.max(AP, 0.8);
//                }
//            }
//            if (ruleType == RuleType.SEQUENCING) {
//                if (attr == JobShopAttribute.NUM_TASK_REMAINING) {
//                    AP = Math.max(AP, 0.8);
//                }
//            }

           // activationProbabilitiess.put(attr, AP);
            System.out.println(ruleType+"Attribute " + attr.getName() + " has AP " + AP);
           activationProbabilities.get(ruleType).put(attr, AP);
        }
    }


    public static JobShopAttribute get(String name) {
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
                                       List<JobShopAttribute> ignoredAttributes) {
        JobShopAttribute a = get(attribute);
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
    public static JobShopAttribute[] basicAttributes() {
        return new JobShopAttribute[]{
                JobShopAttribute.CURRENT_TIME,
                JobShopAttribute.NUM_OPS_IN_QUEUE,
                JobShopAttribute.MACHINE_READY_TIME,
                JobShopAttribute.PROC_TIME,
                JobShopAttribute.WEIGHT,
                JobShopAttribute.WORK_IN_QUEUE,
                JobShopAttribute.UPLOAD_TIME,
                JobShopAttribute.DOWNLOAD_TIME,
        };
    }

    /**
     * The attributes relative to the current time.
     * @return the relative attributes.
     */
    //for flexible JSSP
    //fzhang 19.7.2018 for flexible, the next processing time do not know, because we do not know the next operation will
    //be allocated in which machine:  baseline
    public static JobShopAttribute[] relativeAttributes() {
        return new JobShopAttribute[]{
                //server related
                JobShopAttribute.NUM_OPS_IN_QUEUE,
                JobShopAttribute.WORK_IN_QUEUE,
                JobShopAttribute.MACHINE_READY_TIME,
                JobShopAttribute.UPLOAD_TIME,
                JobShopAttribute.DOWNLOAD_TIME,
                JobShopAttribute.PROC_TIME,
                JobShopAttribute.TOTAL_TIME_IN_QUEUE,

                //job related
                JobShopAttribute.TIME_IN_SYSTEM,
//                JobShopAttribute.NUM_TASK_OF_JOB,
                JobShopAttribute.TASK_WAITING_TIME,
//                JobShopAttribute.WORK_REMAINING,
                JobShopAttribute.NUM_TASK_REMAINING,
//                JobShopAttribute.WEIGHT,
        };
    }
}
