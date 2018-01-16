# How to start guide

1. Clone repository from git

```git clone https://github.com/julia-pavlenko/logger.git```

2. Open project folder

```cd logger```

3. Update log folder in build.gradle file

    args '<log folder>/'

Note:
    - For windows use '\\\\' instead '\\' in path of log folder. Logs file should have file name in specific format: transactionsLog_*.txt


4. Run gradle task:

```./gradlew```

For windows use:

```gradlew.bat```

5. Check report file 'report.txt' it should appear in '<log folder>/report.txt'.
