# Relax/Constrain

This project aims to generate samples from a conditional probability distribution.
The main entry point is the constrain_uniform function, which takes as input a predicate over some real valued uniformly distributed variables, and returns a sampler which generates samples only which adhers to these constraints

## Setup

Requires lpsolve.  To install see http://lpsolve.sourceforge.net/5.5/Java/README.html, or following instructions for Ubuntu 64 bit

Download and decompress lp_solve_5.5.2.0_java.zip, lp_solve_5.5.2.0_dev_ux64.tar.gz, lp_solve_5.5.2.0_exe_ux64.tar.gz.

Copy the library files into /usr/local/lib and /usr/local/include and do ldconfig.

Create a local maven repository
http://www.pgrs.net/2011/10/30/using-local-jars-with-leiningen/

cd project
mkdir maven_repository
mvn install:install-file -Dfile=/path/to/lpsolve55j.jar -DartifactId=lpsolve -Dversion=5.5.2.0 -DgroupId=lpsolve -Dpackaging=jar -Dlocalrepository=maven_repository -Dcreatechecksum=true

## Notes

Much faster with latest java.

# TODO:
RRT - what do i need to do to be able to really compare RRT with RRT/Construct / construct

- Plot obstacles along with graph
- I need an example which really challenges RRT
- Check if unions of convex polytopes are acceptable
- Fix RRT bug

- 2D self intersection test
- Start more general construct