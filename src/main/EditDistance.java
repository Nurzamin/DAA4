package main;

public interface EditDistance {

    int distance(String a, String b);

    String name();

    long operations();

    long memoryCells();
}
