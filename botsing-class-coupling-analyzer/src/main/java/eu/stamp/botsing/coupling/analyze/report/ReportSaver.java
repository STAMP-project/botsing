package eu.stamp.botsing.coupling.analyze.report;

import eu.stamp.botsing.coupling.analyze.calls.ClassPair;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

public class ReportSaver {

    private static void save(String csvFileDir, String csvFileName, List<String> titles, List<ClassPair> list){

        try {
            // Prepare csv
            File file = new File(csvFileDir);
            if(!file.exists()){
                file.mkdirs();
            }

            file = new File(Paths.get(csvFileDir, csvFileName).toString());

            if(!file.exists()){
                file.createNewFile();
            }
            FileWriter csvWriter = new FileWriter(file.getAbsoluteFile());
            int counter = 0;
            for (String title: titles){
                counter++;
                csvWriter.append(title);
                if(counter != titles.size()){
                    csvWriter.append(",");
                }
            }

            csvWriter.append("\n");

            // Save the list in the csv file
            for(ClassPair classPair : list){
                csvWriter.append(classPair.getClass1());
                csvWriter.append(",");
                csvWriter.append(classPair.getCallScore1()+"");
                csvWriter.append(",");
                csvWriter.append(classPair.getNumberOfBranchesInClass1()+"");
                csvWriter.append(",");
                csvWriter.append(classPair.getClass2());
                csvWriter.append(",");
                csvWriter.append(classPair.getCallScore2()+"");
                csvWriter.append(",");
                csvWriter.append(classPair.getNumberOfBranchesInClass2()+"");
                csvWriter.append("\n");
            }


            csvWriter.flush();
            csvWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public static void saveMethodCallAnalyzerReport(String outputDir, List<ClassPair> list){
        String csvFileName = "method-calls-coupling.csv";
        String csvFileDir = Paths.get(outputDir).toString();
        List<String> titles = Arrays.asList("Class1Name", "Class1CallScore",  "Class1ComplexityScore", "Class2Name", "Class2CallScore",  "Class2ComplexityScore");

        save(csvFileDir,csvFileName,titles,list);
    }


    public static void saveSuperSubClassReport(String outputDir, List<ClassPair> list){
        String csvFileName = "super-sub-classes-coupling.csv";
        String csvFileDir = Paths.get(outputDir).toString();
        List<String> titles = Arrays.asList("subClass", "subClassScore", "superClass", "superClassScore", "totalScore");

        save(csvFileDir,csvFileName,titles,list);
    }
}
