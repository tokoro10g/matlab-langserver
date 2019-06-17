# matlab-langserver
Language Server using MATLAB Engine

## Build

```
$ ./gradlew jar
```

## Usage

```
$ export MATLABROOT=/usr/local/MATLAB/R2019a
$ java -Djava.library.path=$MATLABROOT/bin/glnxa64 -cp $MATLABROOT/extern/engines/java/jar/engine.jar:$MATLABROOT/java/jar/jmi.jar:/path/to/lsp-matlab-x.y.jar org.tokor.lspmatlab.Application
```
