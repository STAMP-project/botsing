package eu.stamp.botsing.coupling.analyze.calls;

public class ClassPair implements Comparable<ClassPair> {

    // In hierarchy analysis, class1 is sub class and class2 is super class
    protected String class1;
    protected String class2;
    protected int score1 = 0;
    protected int score2 = 0;
    protected int totalScore = 0;

    public ClassPair(String class1, String class2, int score1, int score2){
        this.class1 = class1;
        this.class2 = class2;
        this.score1 = score1;
        this.score2 = score2;
        this.totalScore = score1+score2;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null || !(obj instanceof ClassPair)){
            return false;
        }

        ClassPair otherPair = (ClassPair) obj;
        if(! otherPair.class1.equals(this.class1) && !otherPair.class1.equals(this.class2)){
            return false;
        }

        if(! otherPair.class2.equals(this.class1) && !otherPair.class2.equals(this.class2)){
            return false;
        }

        return true;
    }

    @Override
    public int compareTo(ClassPair otherPair) {
        return Integer.compare(this.totalScore,otherPair.totalScore);
    }


    public String getClass1() {
        return class1;
    }

    public String getClass2() {
        return class2;
    }

    public int getScore1() {
        return score1;
    }

    public int getScore2() {
        return score2;
    }

    public int getTotalScore() {
        return totalScore;
    }
}
