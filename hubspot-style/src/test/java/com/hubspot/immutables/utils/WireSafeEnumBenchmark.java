package com.hubspot.immutables.utils;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.util.Random;
import java.util.concurrent.TimeUnit;

@State(Scope.Thread)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Fork(value = 2, warmups = 1)
public class WireSafeEnumBenchmark {

  @State(Scope.Benchmark)
  public static class ExecutionPlan {
    private String[] directionsToParse;

    @Setup(Level.Trial)
    public void setUp() {
      this.directionsToParse = new String[10240];
      Random r = new Random(7);
      for (int i = 0; i < directionsToParse.length; ++i) {
        int randInt = r.nextInt(Direction.values().length * 10 + 1);
        if (randInt >= (Direction.values().length * 10)) {
          directionsToParse[i] = FAKE_VALUE;
        } else {
          directionsToParse[i] = Direction.values()[randInt / 10].name();
        }
      }
    }
  }

//    @Benchmark
  @Warmup(iterations = 1, time = 2, timeUnit = TimeUnit.SECONDS)
  @Measurement(iterations = 4, time = 7, timeUnit = TimeUnit.SECONDS)
  public void parse(ExecutionPlan plan, Blackhole blackhole) throws Throwable {
    for (String value : plan.directionsToParse) {
      blackhole.consume(WireSafeEnum.fromJson(Direction.class, value));
    }
  }

  //@Benchmark
  @Warmup(iterations = 1, time = 2, timeUnit = TimeUnit.SECONDS)
  @Measurement(iterations = 4, time = 7, timeUnit = TimeUnit.SECONDS)
  public void parseV2(ExecutionPlan plan, Blackhole blackhole) throws Throwable {
    for (String value : plan.directionsToParse) {
      blackhole.consume(WireSafeEnumV2.fromJson(Direction.class, value));
    }
  }

  //@Benchmark
  @Warmup(iterations = 1, time = 2, timeUnit = TimeUnit.SECONDS)
  @Measurement(iterations = 4, time = 7, timeUnit = TimeUnit.SECONDS)
  public void parseV2ImmutableMap(ExecutionPlan plan, Blackhole blackhole)
    throws Throwable {
    for (String value : plan.directionsToParse) {
      blackhole.consume(WireSafeEnumV2ImmutableMap.fromJson(Direction.class, value));
    }
  }

  @Benchmark

  @Warmup(iterations = 1, time = 2, timeUnit = TimeUnit.SECONDS)
  @Measurement(iterations = 4, time = 17, timeUnit = TimeUnit.SECONDS)
  public void parseV2PerfectMap(ExecutionPlan plan, Blackhole blackhole)
    throws Throwable {
    for (String value : plan.directionsToParse) {
      blackhole.consume(WireSafeEnumV2PerfectMap.fromJson(Direction.class, value));
    }
  }

  public static void main(String[] args) throws Exception {
    Options opt = new OptionsBuilder()
      .include(WireSafeEnumBenchmark.class.getSimpleName())
      .forks(1)
      .shouldDoGC(true)
      .build();

    new Runner(opt).run();
  }

  private static final String FAKE_VALUE = "SMORTH";

  private enum Direction {
    SOUTH,
    WEST,
    NORTH,
    EAST
  }
}
