package com.microsoft.fluency;
public class Predictions extends java.util.AbstractList<Prediction> {
 private final java.util.List<Prediction> predictions;
 public Predictions(java.util.List<Prediction> predictions){this.predictions=predictions;}
 public Prediction get(int i){return predictions.get(i);}public int size(){return predictions.size();}
}
