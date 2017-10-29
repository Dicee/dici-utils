# dici-utils
My personal util library for Scala and Java. Not all of it is high quality as I've come a long way since when I started this !

## Functionalities

### Java

#### Collections

- a single-threaded stream framework that aims at providing better functional style than Java 8's `Stream` API. Also richer than the Java API, allowing interactions with files etc.
- implementation for some basic data structures either for toying around (for example, a doubly linked list) or being used in other of my repositories (for example, a non-blocking bounded queue)
- various other small collection utils I used in other projects

#### Math

- mostly math functions I've used to solve algorithmic problems
- some functions around prime numbers
- a small framework for vector/matrix geometric calculations I used for raytracing
- a few bitwise operations that often come useful in mathsy algorithmic problems
- a permutation generator that generates all permutations of a given number of elements in a lazy fashion, in order or reverse order and starting  from any particular such permutation. Was also useful for solving a few challenges.

#### GUI

- a bunch of JavaFX components I have frequently used in other projects
- a lightweight framework for modelling cancelable/non-cancelable actions that can be used in an editor-like GUI
- a mechanism for tying JavaFX component with a map of localized strings to easily implement a multi-language GUI
- basic image manipulation utils

#### Various

- some IO utils for auto-deleting temp files, creating directories if necessary, work with extensions etc. A bit reinventing the wheel but avoided me adding dependencies to my projects for just a few methods.
- some exception utils to work around Java streams incapability to deal with checked exceptions (at least not nicely)
- some string manipulation utils, mostly to replace things that are easily done in Scala but not in Java
- etc

### Scala

#### Collections

- a few implementation of tries with different properties. Used as part of an auto-completer with basic learning capabilities (simply counts the frequency of words to propose the most frequent first).
- an incomplete implementation of a kd-tree. My intent was to implement a clustering algorithm (OPTICS) but I lacked the time to work on it and eventuallty abandoned.
- other simple utils (quick-select for generic arrays, multiset etc)
