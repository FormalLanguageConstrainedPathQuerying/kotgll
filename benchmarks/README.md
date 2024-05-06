# UCFSBenchmarks

## Prerequisites

```text
(1) Gradle (version >= 7.2)
(2) Antlr V4
(3) Jflex
```

## Build project 

#### Step 1. Clone repository

`git clone https://github.com/cyb3r-b4stard/UCFSBenchmarks.git`

or

`git@github.com:cyb3r-b4stard/UCFSBenchmarks.git`

or

`gh repo clone cyb3r-b4stard/UCFSBenchmarks`

#### Step 2. Go to the project folder

`cd UCFSBenchmarks`

#### Step 3. Install dependencies

`./ucfs_install.sh`

#### Step 4. Build project

`./ucfs_build.sh`

## Execute Benchmarks

#### Step 1. Run benchmarks

`./ucfs_bench.sh`

## Logging

Logs are stored in build/libs/Benchmarks.log

## Results

After executing all benchmarks, results file will be located in the `/build` folder
under name `results.csv`. That file shall be transfered back to Ivan Lomikovskiy