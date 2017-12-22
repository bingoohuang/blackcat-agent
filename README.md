# blackcat-agent
agent that monitor a machine's memory, cpu, processes and etc.


## Log Exceptions Ignore config
in diamond blackcat/log.exception, config like the following:

```
ignore.contains=java.io.IOException: Broken pipe, java.lang.IllegalStateException: getOutputStream
```
