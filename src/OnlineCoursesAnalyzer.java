import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

/**
 * This is just a demo for you, please run it on JDK17
 * (some statements may be not allowed in lower version).
 * This is just a demo, and you can extend and implement functions
 * based on this demo, or implement it in a different way.
 */

public class OnlineCoursesAnalyzer {
    List<Course> courses = new ArrayList<>();

    public OnlineCoursesAnalyzer(String datasetPath) {
        BufferedReader br = null;
        String line;
        try {
            br = new BufferedReader(new FileReader(datasetPath, StandardCharsets.UTF_8));
            br.readLine();
            while ((line = br.readLine()) != null) {
                String[] info = line.split(",(?=([^\\\"]*\\\"[^\\\"]*\\\")*[^\\\"]*$)", -1);
                Course course = new Course(
                        info[0], info[1], new Date(info[2]), info[3], info[4], info[5],
                        Integer.parseInt(info[6]), Integer.parseInt(info[7]),
                        Integer.parseInt(info[8]), Integer.parseInt(info[9]),
                        Integer.parseInt(info[10]), Double.parseDouble(info[11]),
                        Double.parseDouble(info[12]), Double.parseDouble(info[13]),
                        Double.parseDouble(info[14]), Double.parseDouble(info[15]),
                        Double.parseDouble(info[16]), Double.parseDouble(info[17]),
                        Double.parseDouble(info[18]), Double.parseDouble(info[19]),
                        Double.parseDouble(info[20]), Double.parseDouble(info[21]),
                        Double.parseDouble(info[22]));
                courses.add(course);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    //1. TreeMap Sort?
    public Map<String, Integer> getPtcpCountByInst() {
        return courses.stream().collect(Collectors.groupingBy(Course::getInstitution,
                TreeMap::new, Collectors.summingInt(Course::getParticipants)));
    }

    //2
    public Map<String, Integer> getPtcpCountByInstAndSubject() {
        Map<String, Integer> result = new HashMap<>();
        courses.forEach(course -> {
            String combine = course.getInstitution() + "-" + course.getSubject();
            result.put(combine, result.getOrDefault(combine, 0) + course.getParticipants());
        });
        return result.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (u, v) -> u, LinkedHashMap::new));
    }

    //3
    public Map<String, List<List<String>>> getCourseListOfInstructor() {
        Map<String, List<Course>> instructors = new HashMap<>();
        for (Course c : courses) {
            for (String instructor : c.getInstructorList()) {
                List<Course> courseList = instructors.getOrDefault(instructor, new ArrayList<>());
                courseList.add(c);
                instructors.put(instructor, courseList);
            }
        }

        return instructors.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey,
                        entry -> {
                          List<Course> courseList = entry.getValue();
                          List<String> independent = courseList.stream()
                                    .filter(course -> course.getInstructorList().size() == 1)
                                    .map(Course::getTitle)
                                    .distinct()
                                    .sorted(String::compareTo)
                                    .collect(Collectors.toList());

                          List<String> cooperate = courseList.stream()
                                    .filter(course -> course.getInstructorList().size() > 1)
                                    .map(Course::getTitle)
                                    .distinct()
                                    .sorted(String::compareTo)
                                    .collect(Collectors.toList());

                          return Arrays.asList(independent, cooperate);
                        }
                ));
    }

    //4
    public List<String> getCourses(int topK, String by) {
        Comparator<Course> comparator;
        if (by.equals("hours")) {
            comparator = Comparator.comparing(Course::getTotalHours).reversed().thenComparing(Course::getTitle);
        } else if (by.equals("participants")) {
            comparator = Comparator.comparing(Course::getParticipants).reversed().thenComparing(Course::getTitle);
        } else {
            throw new IllegalArgumentException("Invalid sort type");
        }
        List<Course> sortedCourses = courses.stream().sorted(comparator).distinct().limit(topK).toList();
        List<String> result = new ArrayList<>();
        for (Course course : sortedCourses) {
            result.add(course.getTitle());
        }
        return result;
    }

    //5
    public List<String> searchCourses(String courseSubject, double percentAudited, double totalCourseHours) {
        return courses.stream()
                .filter(c -> c.getSubject().toLowerCase().contains(courseSubject.toLowerCase()))
                .filter(c -> c.getPercentAudited() >= percentAudited)
                .filter(c -> c.getTotalHours() <= totalCourseHours)
                .sorted(Comparator.comparing(Course::getTitle))
                .map(Course::getTitle)
                .distinct()
                .collect(Collectors.toList());
    }

    //6
    public List<String> recommendCourses(int age, int gender, int isBachelorOrHigher) {
        // Step 1: Calculate the average Median Age, average Male, and average Bachelor's Degree or Higher for each course
        Map<String, double[]> total_sum = courses.stream()
                .collect(Collectors.toMap(Course::getNumber,
                        c -> new double[]{c.getMedianAge(), c.getPercentMale(), c.getPercentDegree()},
                        (a, b) -> {
                            double[] result = new double[a.length];
                            for (int i = 0; i < a.length; i++) {
                                result[i] = (a[i] + b[i]);
                            }
                            return result;
                        }
                ));

        Map<String, double[]> average = new HashMap<>();
        for (Map.Entry<String, double[]> entry : total_sum.entrySet()) {
            String number = entry.getKey();
            if (!average.containsKey(number)) {
                double[] sum = entry.getValue();
                int count = courses.stream().filter(c -> c.getNumber().equals(number)).toArray().length;
                double[] result = new double[sum.length];
                for (int i = 0; i < sum.length; i++) {
                    result[i] = sum[i] / count;
                }
                average.put(number, result);
            }
        }

        // Step 2: Calculate similarity value between input user and each course
        Map<String, Double> similarity = average.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey,
                        e -> Math.pow(age - e.getValue()[0], 2)
                                + Math.pow(gender * 100 - e.getValue()[1], 2)
                                + Math.pow(isBachelorOrHigher * 100 - e.getValue()[2], 2)));

        // Step 3: Return the top 10 courses with the smallest similarity value
        return similarity.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue()
                        .thenComparing(Map.Entry.comparingByKey(
                                Comparator.comparing(courseNumber -> courses.stream()
                                        .filter(course -> course.getNumber().equals(courseNumber))
                                        .max(Comparator.comparing(Course::getLaunchDate))
                                        .map(Course::getTitle)
                                        .orElse("")))))
                .map(entry -> courses.stream()
                        .filter(course -> course.getNumber().equals(entry.getKey()))
                        .max(Comparator.comparing(Course::getLaunchDate))
                        .map(Course::getTitle)
                        .orElse(""))
                .distinct().limit(10)
                .collect(Collectors.toList());
    }

}

class Course {
    String institution;
    String number;
    Date launchDate;
    String title;
    String instructors;

    String[] instructor_list;
    String subject;
    int year;
    int honorCode;
    int participants;
    int audited;
    int certified;
    double percentAudited;
    double percentCertified;
    double percentCertified50;
    double percentVideo;
    double percentForum;
    double gradeHigherZero;
    double totalHours;
    double medianHoursCertification;
    double medianAge;
    double percentMale;
    double percentFemale;
    double percentDegree;

    public Course(String institution, String number, Date launchDate,
                  String title, String instructors, String subject,
                  int year, int honorCode, int participants,
                  int audited, int certified, double percentAudited,
                  double percentCertified, double percentCertified50,
                  double percentVideo, double percentForum, double gradeHigherZero,
                  double totalHours, double medianHoursCertification,
                  double medianAge, double percentMale, double percentFemale,
                  double percentDegree) {
        this.institution = institution;
        this.number = number;
        this.launchDate = launchDate;
        if (title.startsWith("\"")) title = title.substring(1);
        if (title.endsWith("\"")) title = title.substring(0, title.length() - 1);
        this.title = title;
        if (instructors.startsWith("\"")) instructors = instructors.substring(1);
        if (instructors.endsWith("\"")) instructors = instructors.substring(0, instructors.length() - 1);
        this.instructors = instructors;
        this.instructor_list = instructors.split(", ");
        if (subject.startsWith("\"")) subject = subject.substring(1);
        if (subject.endsWith("\"")) subject = subject.substring(0, subject.length() - 1);
        this.subject = subject;
        this.year = year;
        this.honorCode = honorCode;
        this.participants = participants;
        this.audited = audited;
        this.certified = certified;
        this.percentAudited = percentAudited;
        this.percentCertified = percentCertified;
        this.percentCertified50 = percentCertified50;
        this.percentVideo = percentVideo;
        this.percentForum = percentForum;
        this.gradeHigherZero = gradeHigherZero;
        this.totalHours = totalHours;
        this.medianHoursCertification = medianHoursCertification;
        this.medianAge = medianAge;
        this.percentMale = percentMale;
        this.percentFemale = percentFemale;
        this.percentDegree = percentDegree;
    }

    public String getInstitution() {
        return institution;
    }

    public String getNumber() {
        return number;
    }

    public Date getLaunchDate() {
        return launchDate;
    }

    public String getTitle() {
        return title;
    }

    public String getInstructors() {
        return instructors;
    }

    public List<String> getInstructorList() {
        return List.of(instructor_list);
    }

    public String getSubject() {
        return subject;
    }

    public int getParticipants() {
        return participants;
    }

    public int getAudited() {
        return audited;
    }

    public double getTotalHours() {
        return totalHours;
    }

    public double getPercentAudited() {
        return percentAudited;
    }

    public double getMedianAge() {
        return medianAge;
    }

    public double getPercentMale() {
        return percentMale;
    }

    public double getPercentFemale() {
        return percentFemale;
    }

    public double getPercentDegree() {
        return percentDegree;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Course c = (Course) o;
        return Objects.equals(title, c.title);
    }

    @Override
    public int hashCode() {
        return Objects.hash(title);
    }
}