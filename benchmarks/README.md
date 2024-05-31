# UCFSBenchmarks

## Prerequisites

```text
(1) Gradle (version >= 7.2)
(2) Antlr V4
(3) Jflex
```
## Generate files
Run `/scripts/generate_all.sh`
## Run benchmarks
From root project folder run `./gradlew :benchmarks:jmh -PtoolName=${toolRegexp} -Pdataset=${absolutePathToDatasetFolder}`

## Logging

Logs are stored in `logs`

Results are stored in  `build/reports/benchmarks`
