package com.sam.qa.models;

public class Product {
    private String name;
    private double temp;

    public Product() {} 

    public Product(String name, double temp) {
        this.name = name;
        this.temp = temp;
    }

    public String getName() { return name; }
    public double getTemp() { return temp; }
}