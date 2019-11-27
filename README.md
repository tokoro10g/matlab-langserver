# matlab-langserver
Language Server using MATLAB Engine

## Build

1. Edit `build.gradle` to specify your MATLAB installation path.
For example, if you have R2019a running on Linux with the default path, the following lines should be:
```
compileOnly files(
        '/usr/local/MATLAB/R2019a/extern/engines/java/jar/engine.jar',
        '/usr/local/MATLAB/R2019a/java/jar/mde.jar',
        '/usr/local/MATLAB/R2019a/java/jar/mlwidgets.jar',
)
```
1. Now you can build a jar file with Gradle:
```
$ ./gradlew jar
```

## Usage

```
$ export MATLABROOT=/usr/local/MATLAB/R2019a
$ java -Djava.library.path=$MATLABROOT/bin/glnxa64 -cp $MATLABROOT/extern/engines/java/jar/engine.jar:$MATLABROOT/java/jar/jmi.jar:/path/to/lsp-matlab-x.y.jar org.tokor.lspmatlab.Application
```
Do not forget to replace `/path/to/lsp-matlab-x.y.jar` with your path of the jar file.

## Note

This project is experimental and not tested for every platform.
Future updates on MATLAB might break the functionality because the code heavily depends on undocumented functions and APIs.

If you have any problems or suggestions, please open issues and hopefully contribute by coding.
