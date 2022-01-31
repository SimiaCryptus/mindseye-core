package com.simiacryptus.mindseye.opt;

public class TrainingResult {

  public enum TerminationCause {
    Failed,
    Timeout,
    Completed
  }

  public final double finalValue;
  public final TerminationCause terminationCause;

  public TrainingResult(double finalValue, TerminationCause terminationCause) {
    this.finalValue = finalValue;
    this.terminationCause = terminationCause;
  }
}
