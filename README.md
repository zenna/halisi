# Sigma

Sigma is a probabilistic programming language.
In it we can specify probabilstic models and perform inference.

[Main Page](http://zenna.github.io/sigma/)

## Setup
Not currently in a distribution ready form.  But for the risk-takers, it requires [clozen](https://github.com/zenna/clozen) a general utility library, and [veneer](https://github.com/zenna/veneer).

Additionally it requires lpsolve for linear programming.  To install see http://lpsolve.sourceforge.net/5.5/Java/README.html, or following instructions for Ubuntu 64 bit

Download and decompress lp_solve_5.5.2.0_java.zip, lp_solve_5.5.2.0_dev_ux64.tar.gz, lp_solve_5.5.2.0_exe_ux64.tar.gz.

Copy the library files into /usr/local/lib and /usr/local/include and do ldconfig.

Create a local maven repository
http://www.pgrs.net/2011/10/30/using-local-jars-with-leiningen/

cd project
mkdir maven_repository
mvn install:install-file -Dfile=/path/to/lpsolve55j.jar -DartifactId=lpsolve -Dversion=5.5.2.0 -DgroupId=lpsolve -Dpackaging=jar -Dlocalrepository=maven_repository -Dcreatechecksum=true

## Run
Then to open a repl:

```Clojure
lein run sigma
 ```