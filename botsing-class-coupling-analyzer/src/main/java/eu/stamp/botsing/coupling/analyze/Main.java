package eu.stamp.botsing.coupling.analyze;

public class Main {

    @SuppressWarnings("checkstyle:systemexit")
    public static void main(String[] args) {
        ClassCouplingAnalyzer main = new ClassCouplingAnalyzer();
        main.parseCommandLine(args);
        System.exit(0);
    }
}
