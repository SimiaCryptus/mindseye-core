package com.simiacryptus.mindseye.opt;

/**
 * This class represents the result of training, including the final value and the termination cause.
 *
 * @docgenVersion 9
 */
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
