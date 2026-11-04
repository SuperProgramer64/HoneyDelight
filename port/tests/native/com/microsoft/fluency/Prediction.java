package com.microsoft.fluency;
public class Prediction extends java.util.AbstractList<Term> {
 private final String text; private final double probability;
 public Prediction(String text,double probability){this.text=text;this.probability=probability;}
 public String getPrediction(){return text;} public double getProbability(){return probability;}
 public String getSource(){return null;}public Term get(int i){throw new IndexOutOfBoundsException();}public int size(){return 0;}
}
